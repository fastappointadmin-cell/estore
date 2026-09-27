package com.lavander.estore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class R2Config {

    @Bean
    public S3Client r2Client(@Value("${r2.account-id}") String accountId,
                              @Value("${r2.access-key-id}") String accessKeyId,
                              @Value("${r2.secret-access-key}") String secretAccessKey) {
        // Falls back to placeholder values when unset so the app still boots locally
        // without R2 configured; actual upload/delete calls then fail at call-time
        // instead of at startup, matching how the other optional local secrets behave.
        String resolvedAccessKeyId = accessKeyId.isBlank() ? "unset" : accessKeyId;
        String resolvedSecretAccessKey = secretAccessKey.isBlank() ? "unset" : secretAccessKey;
        String resolvedAccountId = accountId.isBlank() ? "unset" : accountId;

        return S3Client.builder()
                .endpointOverride(URI.create("https://" + resolvedAccountId + ".r2.cloudflarestorage.com"))
                // R2 ignores the region value but the SDK requires one to be set.
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(resolvedAccessKeyId, resolvedSecretAccessKey)))
                .build();
    }
}
