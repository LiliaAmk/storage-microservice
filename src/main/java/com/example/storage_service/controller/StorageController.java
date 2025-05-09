// src/main/java/com/example/storage_service/controller/StorageController.java
package com.example.storage_service.controller;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.storage_service.service.StorageService;

@RestController
@RequestMapping("/storage")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Upload endpoint now *only* accepts multipart/form-data,
     * and binds the file part via @RequestPart.
     */
    @PostMapping(
      path     = "/upload",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        // will throw 413 or 415 from StorageService if limits or type checks fail
        String key = storageService.upload(file);
        return ResponseEntity
                .status(201)
                .body(Map.of("key", key));
    }

    /**
     * Download endpoint redirects (302) the client to the presigned S3/MinIO URL.
     */
    @GetMapping("/download/{key}")
    public ResponseEntity<Void> downloadFile(@PathVariable String key) {
        // generateDownloadUrl returns a java.net.URL
        String url = storageService
                        .generateDownloadUrl(key, Duration.ofMinutes(60))
                        .toString();
        // URI.create(...) sidesteps URISyntaxException
        URI redirect = URI.create(url);
        return ResponseEntity
                .status(302)
                .location(redirect)
                .build();
    }
}
