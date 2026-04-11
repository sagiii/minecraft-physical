package com.example.gpiobridge.camera;

import com.example.gpiobridge.entity.CameraEntity;
import com.example.gpiobridge.network.MqttBridgeClient;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side camera frame capture and MQTT publish.
 *
 * Each frame is rendered from the CameraEntity's world position and rotation
 * by temporarily switching mc.setCameraEntity() for one render frame, then restoring.
 *
 * The player's view flips to the camera for exactly one rendered frame (≈16ms at 60fps),
 * which is imperceptible at typical capture rates (1–5 fps).
 */
public class OffscreenCameraRenderer {

    private static final boolean VERBOSE = true;

    /** camId → timestamp (ms) of the last published frame */
    private final Map<Integer, Long> lastFrameMs = new ConcurrentHashMap<>();

    /** Non-null when the current frame was rendered from a camera entity's viewpoint. */
    private CameraEntity pendingCapture  = null;
    private Entity        prevCamEntity  = null;
    private int           pendingCamId   = -1;
    private int           pendingW       = 240;
    private int           pendingH       = 135;

    public void initialize() {
        // END_MAIN fires after the main level render:
        // 1. If a camera-view frame was just rendered, capture & restore camera.
        // 2. If a camera needs its next frame, switch to it for the next render.
        LevelRenderEvents.END_MAIN.register(context -> onEndMain());
        if (VERBOSE) System.out.println("[MP Bridge] OffscreenCameraRenderer initialized");
    }

    private void onEndMain() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // ---- 1. capture the camera-view frame that was just rendered ----
        if (pendingCapture != null) {
            final int camId = pendingCamId;
            final int w = pendingW, h = pendingH;
            pendingCapture = null;
            pendingCamId   = -1;

            // Restore original camera before taking screenshot callback fires
            if (prevCamEntity != null) {
                mc.setCameraEntity(prevCamEntity);
                prevCamEntity = null;
            }

            Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
                byte[] jpeg = encodeJpeg(image, w, h);
                image.close();
                if (jpeg != null) {
                    MqttBridgeClient.INSTANCE.publishRaw("mp/bridge/m/cam/" + camId + "/frame", jpeg);
                    if (VERBOSE) {
                        System.out.printf("[MP Bridge] Published cam/%d frame: %d bytes%n", camId, jpeg.length);
                    }
                }
            });
        }

        // ---- 2. schedule the next camera-view capture ----
        if (!MqttBridgeClient.INSTANCE.isConnected()) return;

        long nowMs = System.currentTimeMillis();

        if (VERBOSE && nowMs % 5000 < 50) {
            int camCount = 0;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof CameraEntity) camCount++;
            }
            System.out.printf("[MP Bridge] cameras: %d | MQTT: %b%n",
                    camCount, MqttBridgeClient.INSTANCE.isConnected());
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof CameraEntity cam)) continue;
            int id = cam.getCamId();
            if (id <= 0) continue;

            long intervalMs = (long) (1000.0f / Math.max(0.1f, cam.getFps()));
            Long last = lastFrameMs.get(id);
            if (last != null && nowMs - last < intervalMs) continue;

            lastFrameMs.put(id, nowMs);

            // Switch the camera to this entity — next frame renders from its position
            prevCamEntity  = mc.getCameraEntity();
            pendingCapture = cam;
            pendingCamId   = id;
            pendingW       = cam.getWidth();
            pendingH       = cam.getHeight();
            mc.setCameraEntity(cam);

            if (VERBOSE) {
                System.out.printf("[MP Bridge] Switching to cam/%d view for next frame%n", id);
            }
            break; // one camera per frame cycle; others will be captured on subsequent frames
        }
    }

    // ----- JPEG encoding -----

    private static byte[] encodeJpeg(NativeImage image, int targetW, int targetH) {
        try {
            int srcW = image.getWidth();
            int srcH = image.getHeight();

            // NativeImage from GL readback stores pixels as BGRA: bits 0-7=B, 8-15=G, 16-23=R
            BufferedImage bi = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            float scaleX = (float) srcW / targetW;
            float scaleY = (float) srcH / targetH;

            for (int y = 0; y < targetH; y++) {
                for (int x = 0; x < targetW; x++) {
                    int sx = Math.min((int) (x * scaleX), srcW - 1);
                    int sy = Math.min((int) (y * scaleY), srcH - 1);
                    int pixel = image.getPixel(sx, sy);
                    int b = (pixel)       & 0xFF;
                    int g = (pixel >> 8)  & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    bi.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream(targetW * targetH / 2);
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                System.err.println("[MP Bridge] No JPEG ImageWriter found");
                return null;
            }
            ImageWriter writer = writers.next();
            JPEGImageWriteParam params = new JPEGImageWriteParam(null);
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(0.4f);
            writer.setOutput(ImageIO.createImageOutputStream(baos));
            writer.write(null, new IIOImage(bi, null, null), params);
            writer.dispose();
            return baos.toByteArray();

        } catch (Exception e) {
            System.err.println("[MP Bridge] JPEG encode failed: " + e.getMessage());
            return null;
        }
    }
}
