package com.example.storage_service.service;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class StorageService {

    @Value("${minio.bucket}")
    private String bucket;

    private final S3Client s3Client;
    private final S3Presigner presigner;

    // 10 MB max
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Allowed MIME types: PNG, JPEG, PDF, and plain‐text
    private static final List<String> ALLOWED_TYPES = List.of(
        MediaType.IMAGE_PNG_VALUE,
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.APPLICATION_PDF_VALUE,
        MediaType.TEXT_PLAIN_VALUE        // <- added
    );

    public StorageService(S3Client s3Client, S3Presigner presigner) {
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    public String upload(MultipartFile file) throws IOException {
        // size check
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File must be at most " + (MAX_FILE_SIZE / (1024 * 1024)) + " MB"
            );
        }

        // content-type check
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported file type: " + contentType
            );
        }

        // generate key and upload
        String key = UUID.randomUUID() + "_" + file.getOriginalFilename();
        PutObjectRequest por = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(por,
            RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return key;
    }

    public URL generateDownloadUrl(String key, Duration validFor) {
        GetObjectRequest gor = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        var presignRequest = GetObjectPresignRequest.builder()
            .getObjectRequest(gor)
            .signatureDuration(validFor)
            .build();

        return presigner.presignGetObject(presignRequest).url();
    }
}
