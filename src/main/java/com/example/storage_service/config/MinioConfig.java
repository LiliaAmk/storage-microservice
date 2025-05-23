package com.example.storage_service.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private URI endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey))
                )
                .region(Region.of(region))
                .serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // required for MinIO
                        .build()
                )
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey))
                )
                .region(Region.of(region))
                .serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build()
                )
                .build();
    }
}
