package com.lavander.estore.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ImageStorageService {

    private static final int THUMBNAIL_SIZE = 300;
    private static final int MEDIUM_SIZE = 1000;

    private final S3Client r2Client;
    private final String bucket;
    private final String publicBaseUrl;

    public ImageStorageService(
            S3Client r2Client,
            @Value("${r2.bucket}") String bucket,
            @Value("${r2.public-base-url}") String publicBaseUrl) {
        this.r2Client = r2Client;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl;
    }

    public record UploadedImage(String thumbnailKey, String thumbnailUrl, String mediumKey, String mediumUrl) {
    }

    public UploadedImage upload(Long variantId, MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("File must be a non-empty image");
        }

        String id = UUID.randomUUID().toString();
        String thumbnailKey = "variants/" + variantId + "/" + id + "-thumb.jpg";
        String mediumKey = "variants/" + variantId + "/" + id + "-medium.jpg";

        byte[] thumbnailBytes = resize(file, THUMBNAIL_SIZE);
        byte[] mediumBytes = resize(file, MEDIUM_SIZE);

        putObject(thumbnailKey, thumbnailBytes);
        putObject(mediumKey, mediumBytes);

        return new UploadedImage(
                thumbnailKey, publicUrl(thumbnailKey), mediumKey, publicUrl(mediumKey));
    }

    public void delete(String thumbnailKey, String mediumKey) {
        r2Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(thumbnailKey).build());
        r2Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(mediumKey).build());
    }

    public record BrowsedImage(String thumbnailKey, String thumbnailUrl, String mediumUrl, Long sourceVariantId) {
    }

    // Only the "-thumb" half of each pair is listed (one grid tile per logical
    // image, not two) — its "-medium" counterpart is derived by naming
    // convention rather than fetched, since both are always written together.
    public List<BrowsedImage> listImages() {
        List<S3Object> objects = r2Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucket).prefix("variants/").build())
                .contents();

        return objects.stream()
                .map(S3Object::key)
                .filter(key -> key.endsWith("-thumb.jpg"))
                .sorted(Comparator.reverseOrder())
                .map(this::toBrowsedImage)
                .toList();
    }

    // Attaches an object that's already sitting in the bucket (e.g. from a
    // previous upload on another variant) without re-uploading or re-resizing.
    public UploadedImage attachExisting(String thumbnailKey) {
        String mediumKey = mediumKeyFor(thumbnailKey);
        return new UploadedImage(thumbnailKey, publicUrl(thumbnailKey), mediumKey, publicUrl(mediumKey));
    }

    private BrowsedImage toBrowsedImage(String thumbnailKey) {
        String mediumKey = mediumKeyFor(thumbnailKey);
        return new BrowsedImage(thumbnailKey, publicUrl(thumbnailKey), publicUrl(mediumKey), extractVariantId(thumbnailKey));
    }

    private String mediumKeyFor(String thumbnailKey) {
        return thumbnailKey.substring(0, thumbnailKey.length() - "-thumb.jpg".length()) + "-medium.jpg";
    }

    // Key format: variants/{variantId}/{uuid}-thumb.jpg
    private Long extractVariantId(String key) {
        String[] parts = key.split("/");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private byte[] resize(MultipartFile file, int size) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(size, size)
                    .outputFormat("jpg")
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process image file", e);
        }
    }

    private void putObject(String key, byte[] bytes) {
        r2Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType("image/jpeg").build(),
                RequestBody.fromInputStream(new ByteArrayInputStream(bytes), bytes.length));
    }

    private String publicUrl(String key) {
        return publicBaseUrl + "/" + key;
    }
}
