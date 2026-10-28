package com.facultyams.ui.components;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.imageio.ImageIO;

/** Image loading and high-quality avatar scaling utilities. */
public final class ImageUtils {
    private static final String DEFAULT_IMAGE = "/images/profile.png";
    private static final ConcurrentMap<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    private ImageUtils() { }

    /** Loads a resource or file image and returns its cached, face-positioned circular-avatar source. */
    public static BufferedImage loadAvatarSource(String imagePath) throws IOException {
        String key = imagePath == null || imagePath.isBlank() ? DEFAULT_IMAGE : imagePath;
        BufferedImage cached = CACHE.get(key + ":source");
        if (cached != null) return cached;

        BufferedImage source;
        if (key.startsWith("/")) {
            var stream = ImageUtils.class.getResourceAsStream(key);
            if (stream != null) {
                try (stream) {
                    source = ImageIO.read(stream);
                }
            } else {
                Path developmentResource = Path.of("src", "main", "resources", key.substring(1));
                if (!Files.isRegularFile(developmentResource)) {
                    throw new IOException("Profile image resource not found: " + key);
                }
                source = ImageIO.read(developmentResource.toFile());
            }
        } else {
            source = ImageIO.read(Path.of(key).toFile());
        }
        if (source == null) throw new IOException("Unsupported or unreadable profile image: " + key);
        int side = Math.max(1, Math.min(source.getWidth(), source.getHeight()) * 4 / 5);
        int x = Math.max(0, (source.getWidth() - side) / 2);
        int y = Math.max(0, Math.min(source.getHeight() - side, source.getHeight() / 32));
        BufferedImage crop = source.getSubimage(x, y, side, side);
        CACHE.putIfAbsent(key + ":source", copy(crop));
        return CACHE.get(key + ":source");
    }

    /** Returns a cached, progressively downscaled square avatar image. */
    public static BufferedImage getAvatarImage(String imagePath, int size) throws IOException {
        String key = (imagePath == null || imagePath.isBlank() ? DEFAULT_IMAGE : imagePath) + ":" + size;
        BufferedImage cached = CACHE.get(key);
        if (cached != null) return cached;
        BufferedImage image = loadAvatarSource(imagePath);
        int target = Math.max(1, size * 2);
        while (image.getWidth() / 2 >= target && image.getHeight() / 2 >= target) {
            image = scale(image, Math.max(target, image.getWidth() / 2));
        }
        BufferedImage result = scale(image, target);
        CACHE.putIfAbsent(key, result);
        return CACHE.get(key);
    }

    /** Produces a square crop with bounded pan offsets and high-quality scaling. */
    public static BufferedImage cropAvatar(BufferedImage source, double zoom, double offsetX, double offsetY, int size) {
        double factor = Math.max(1.0, Math.min(3.0, zoom));
        double cropSide = Math.min(source.getWidth(), source.getHeight()) / factor;
        double x = (source.getWidth() - cropSide) / 2.0 + offsetX;
        double y = Math.max(0, source.getHeight() * 0.04) + offsetY;
        x = Math.max(0, Math.min(source.getWidth() - cropSide, x));
        y = Math.max(0, Math.min(source.getHeight() - cropSide, y));
        BufferedImage cropped = source.getSubimage((int) x, (int) y, Math.max(1, (int) cropSide), Math.max(1, (int) cropSide));
        return scale(cropped, Math.max(1, size));
    }

    /** Clears cached versions after a profile photo is replaced or removed. */
    public static void clearCache() {
        CACHE.clear();
    }

    private static BufferedImage scale(BufferedImage source, int width) {
        int height = Math.max(1, (int) Math.round((double) source.getHeight() * width / source.getWidth()));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return target;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return result;
    }
}
