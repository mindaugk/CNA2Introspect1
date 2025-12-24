package com.example.mkpublisher;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class SqsPublisher implements AutoCloseable {
    private final SqsClient client;

    public SqsPublisher(Region region) {
        // Prefer explicit environment variables when provided, otherwise fall back to default provider chain
        AwsCredentialsProvider credsProvider;
        String envKey = System.getenv("AWS_ACCESS_KEY_ID");
        String envSecret = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (envKey != null && !envKey.isBlank() && envSecret != null && !envSecret.isBlank()) {
            credsProvider = EnvironmentVariableCredentialsProvider.create();
        } else {
            credsProvider = DefaultCredentialsProvider.create();
        }

        this.client = SqsClient.builder()
                .region(region)
                .credentialsProvider(credsProvider)
                .build();
    }

    public SendMessageResponse publish(String queueUrl, String messageBody) {
        SendMessageRequest req = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build();
        return client.sendMessage(req);
    }

    @Override
    public void close() {
        client.close();
    }
}
