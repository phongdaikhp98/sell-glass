package com.sellglass.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary under folder "sell-glass/products".
     *
     * @return secure_url of the uploaded image
     */
    public String upload(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", "sell-glass/products")
        );
        return (String) result.get("secure_url");
    }

    /**
     * Delete image from Cloudinary by public_id.
     * public_id is extracted from the Cloudinary URL.
     */
    public void delete(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            log.warn("Cannot extract publicId from URL: {}", imageUrl);
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", publicId, e);
        }
    }

    /**
     * Extract Cloudinary public_id from a secure URL.
     * Example: https://res.cloudinary.com/demo/image/upload/v1234/sell-glass/products/abc.jpg
     * → public_id: sell-glass/products/abc
     */
    private String extractPublicId(String url) {
        if (url == null || !url.contains("/upload/")) return null;
        String[] parts = url.split("/upload/");
        if (parts.length < 2) return null;
        String afterUpload = parts[1];
        // Remove version prefix (e.g. "v1234567890/")
        afterUpload = afterUpload.replaceFirst("v\\d+/", "");
        // Remove file extension
        int lastDot = afterUpload.lastIndexOf('.');
        return lastDot > 0 ? afterUpload.substring(0, lastDot) : afterUpload;
    }
}
