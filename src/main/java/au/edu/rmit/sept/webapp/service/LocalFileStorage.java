package au.edu.rmit.sept.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * LocalFileStorage Implementation
 * Stores photos on local filesystem for dev and devprod profiles.
 *
 * STORAGE STRUCTURE:
 * - Base: uploads/events/{eventId}/
 * - Original: {uuid}.jpg
 * - Thumbnail: {uuid}_thumb.jpg
 *
 * FEATURES:
 * - Generates 300x300px thumbnails (maintains aspect ratio, adds padding)
 * - Validates file type (JPEG/PNG only)
 * - Validates file size (<5MB)
 * - UUID-based filenames prevent path traversal
 */
@Service
@Profile({"dev", "devprod", "test", "default"})
public class LocalFileStorage implements PhotoStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int THUMBNAIL_SIZE = 300;
    private static final String THUMBNAIL_SUFFIX = "_thumb";

    @Value("${photo.upload.dir:uploads}")
    private String uploadBaseDir;

    @Override
    public String uploadPhoto(Long eventId, MultipartFile file, String originalFilename) throws IOException {
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null ||
            (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Only JPEG and PNG images are allowed");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Generate UUID-based filename
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + extension;

        // Create directory structure
        Path eventDir = Paths.get(uploadBaseDir, "events", eventId.toString());
        Files.createDirectories(eventDir);

        // Save original file
        Path originalPath = eventDir.resolve(filename);
        file.transferTo(originalPath.toFile());

        // Generate and save thumbnail
        generateThumbnail(originalPath.toFile(), eventDir, filename);

        return filename;
    }

    @Override
    public void deletePhoto(Long eventId, String filename) throws IOException {
        Path eventDir = Paths.get(uploadBaseDir, "events", eventId.toString());

        // Delete original
        Path originalPath = eventDir.resolve(filename);
        Files.deleteIfExists(originalPath);

        // Delete thumbnail
        String thumbnailFilename = getThumbnailFilename(filename);
        Path thumbnailPath = eventDir.resolve(thumbnailFilename);
        Files.deleteIfExists(thumbnailPath);
    }

    @Override
    public String getPhotoUrl(Long eventId, String filename) {
        return String.format("/uploads/events/%d/%s", eventId, filename);
    }

    @Override
    public String getThumbnailUrl(Long eventId, String filename) {
        String thumbnailFilename = getThumbnailFilename(filename);
        return String.format("/uploads/events/%d/%s", eventId, thumbnailFilename);
    }

    @Override
    public byte[] downloadPhoto(Long eventId, String filename) throws IOException {
        Path eventDir = Paths.get(uploadBaseDir, "events", eventId.toString());
        Path photoPath = eventDir.resolve(filename);

        if (!Files.exists(photoPath)) {
            throw new IOException("Photo not found: " + filename);
        }

        return Files.readAllBytes(photoPath);
    }

    /**
     * Generates a 300x300px thumbnail with aspect ratio maintained.
     * Adds white padding if image is not square.
     */
    private void generateThumbnail(File originalFile, Path eventDir, String filename) throws IOException {
        BufferedImage original = ImageIO.read(originalFile);
        if (original == null) {
            throw new IOException("Failed to read image file");
        }

        // Calculate scaling to fit within 300x300 while maintaining aspect ratio
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        double scale = Math.min(
            (double) THUMBNAIL_SIZE / originalWidth,
            (double) THUMBNAIL_SIZE / originalHeight
        );

        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        // Create thumbnail canvas (white background)
        BufferedImage thumbnail = new BufferedImage(
            THUMBNAIL_SIZE, THUMBNAIL_SIZE, BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = thumbnail.createGraphics();

        // Fill with white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

        // Draw scaled image centered
        int x = (THUMBNAIL_SIZE - scaledWidth) / 2;
        int y = (THUMBNAIL_SIZE - scaledHeight) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, x, y, scaledWidth, scaledHeight, null);
        g.dispose();

        // Save thumbnail
        String thumbnailFilename = getThumbnailFilename(filename);
        Path thumbnailPath = eventDir.resolve(thumbnailFilename);
        String format = filename.toLowerCase().endsWith(".png") ? "png" : "jpg";
        ImageIO.write(thumbnail, format, thumbnailPath.toFile());
    }

    /**
     * Gets file extension from filename (e.g., ".jpg", ".png")
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".jpg"; // default
    }

    /**
     * Converts filename to thumbnail filename (e.g., "abc.jpg" -> "abc_thumb.jpg")
     */
    private String getThumbnailFilename(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            String name = filename.substring(0, lastDot);
            String ext = filename.substring(lastDot);
            return name + THUMBNAIL_SUFFIX + ext;
        }
        return filename + THUMBNAIL_SUFFIX;
    }
}
