package au.edu.rmit.sept.webapp.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * GCSStorage Implementation
 * Stores photos in Google Cloud Storage for production profile.
 *
 * STORAGE STRUCTURE:
 * - Bucket: configured via application-prod.properties
 * - Path: events/{eventId}/{uuid}.jpg
 * - Thumbnail: events/{eventId}/{uuid}_thumb.jpg
 *
 * FEATURES:
 * - Uploads to GCS bucket
 * - Generates 300x300px thumbnails
 * - Returns public GCS URLs
 * - Validates file type (JPEG/PNG only)
 * - Validates file size (<5MB)
 *
 * AUTHENTICATION:
 * - Uses Application Default Credentials (ADC)
 * - On VM: Automatically uses service account
 * - Locally: Set GOOGLE_APPLICATION_CREDENTIALS environment variable
 */
@Service
@Profile("prod")
public class GCSStorage implements PhotoStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int THUMBNAIL_SIZE = 300;
    private static final String THUMBNAIL_SUFFIX = "_thumb";

    @Value("${gcs.bucket.name}")
    private String bucketName;

    private final Storage storage;

    public GCSStorage() {
        // Initialize GCS client with Application Default Credentials
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

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

        // Upload original file
        String originalPath = String.format("events/%d/%s", eventId, filename);
        uploadToGCS(originalPath, file.getBytes(), contentType);

        // Generate and upload thumbnail
        byte[] thumbnailBytes = generateThumbnail(file.getBytes(), extension);
        String thumbnailPath = String.format("events/%d/%s", eventId, getThumbnailFilename(filename));
        uploadToGCS(thumbnailPath, thumbnailBytes, contentType);

        return filename;
    }

    @Override
    public void deletePhoto(Long eventId, String filename) throws IOException {
        // Delete original
        String originalPath = String.format("events/%d/%s", eventId, filename);
        deleteFromGCS(originalPath);

        // Delete thumbnail
        String thumbnailPath = String.format("events/%d/%s", eventId, getThumbnailFilename(filename));
        deleteFromGCS(thumbnailPath);
    }

    @Override
    public String getPhotoUrl(Long eventId, String filename) {
        // Return public GCS URL
        // Format: https://storage.googleapis.com/{bucket}/events/{eventId}/{filename}
        return String.format("https://storage.googleapis.com/%s/events/%d/%s",
            bucketName, eventId, filename);
    }

    @Override
    public String getThumbnailUrl(Long eventId, String filename) {
        String thumbnailFilename = getThumbnailFilename(filename);
        return String.format("https://storage.googleapis.com/%s/events/%d/%s",
            bucketName, eventId, thumbnailFilename);
    }

    @Override
    public byte[] downloadPhoto(Long eventId, String filename) throws IOException {
        try {
            String path = String.format("events/%d/%s", eventId, filename);
            BlobId blobId = BlobId.of(bucketName, path);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                throw new IOException("Photo not found: " + path);
            }

            return blob.getContent();
        } catch (StorageException e) {
            throw new IOException("Failed to download file from GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a file to GCS bucket
     */
    private void uploadToGCS(String path, byte[] data, String contentType) throws IOException {
        try {
            BlobId blobId = BlobId.of(bucketName, path);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

            storage.create(blobInfo, data);
        } catch (StorageException e) {
            throw new IOException("Failed to upload file to GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a file from GCS bucket
     */
    private void deleteFromGCS(String path) throws IOException {
        try {
            BlobId blobId = BlobId.of(bucketName, path);
            boolean deleted = storage.delete(blobId);
            if (!deleted) {
                // File might not exist, log but don't throw
                System.err.println("File not found in GCS: " + path);
            }
        } catch (StorageException e) {
            throw new IOException("Failed to delete file from GCS: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a 300x300px thumbnail with aspect ratio maintained.
     * Adds white padding if image is not square.
     */
    private byte[] generateThumbnail(byte[] originalBytes, String extension) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
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

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String format = extension.toLowerCase().endsWith(".png") ? "png" : "jpg";
        ImageIO.write(thumbnail, format, baos);
        return baos.toByteArray();
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
