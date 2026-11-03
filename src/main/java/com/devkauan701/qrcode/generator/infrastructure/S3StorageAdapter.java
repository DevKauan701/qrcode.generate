package com.devkauan701.qrcode.generator.infrastructure;

import com.devkauan701.qrcode.generator.ports.StoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

@Component
public class S3StorageAdapter implements StoragePort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration urlExpiration;

    public S3StorageAdapter(
            @Value("${storage.endpoint}") String endpoint,
            @Value("${storage.public-endpoint}") String publicEndpoint,
            @Value("${storage.region}") String region,
            @Value("${storage.access-key}") String accessKey,
            @Value("${storage.secret-key}") String secretKey,
            @Value("${storage.bucket}") String bucket,
            @Value("${storage.url-expiration-minutes}") long expirationMinutes) {

        this.bucket = bucket;
        this.urlExpiration = Duration.ofMinutes(expirationMinutes);

        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));
        var pathStyle = S3Configuration.builder()
                .pathStyleAccessEnabled(true)// Obrigatorio fora da AWS
                .build();

        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(publicEndpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .serviceConfiguration(pathStyle)
                .build();
    }

    @Override
    public String uploadFile(byte[] fileData, String fileName, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileName)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(fileData));

        // Link temporário assinado: Não precisa deixar o bucket público
        var presigned = presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(urlExpiration)
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(fileName)
                                .build())
                        .build());

        return presigned.url().toString();
    }
}
