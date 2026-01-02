package com.example.mkconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

public class SqsConsumer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SqsConsumer.class);
    private final SqsClient client;
    private volatile boolean running = false;

    public SqsConsumer(Region region) {
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

    public void startPolling(String queueUrl, int maxMessages, int waitTimeSeconds, int pollIntervalSeconds) {
        running = true;
        logger.info("Starting SQS consumer for queue: {}", queueUrl);
        logger.info("Configuration: maxMessages={}, waitTimeSeconds={}, pollIntervalSeconds={}", 
                    maxMessages, waitTimeSeconds, pollIntervalSeconds);

        while (running) {
            try {
                ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(waitTimeSeconds)
                        .build();

                ReceiveMessageResponse receiveResponse = client.receiveMessage(receiveRequest);
                List<Message> messages = receiveResponse.messages();

                if (messages.isEmpty()) {
                    logger.debug("No messages received from queue");
                } else {
                    logger.info("Received {} message(s) from queue", messages.size());
                    
                    for (Message message : messages) {
                        processMessage(message);
                        deleteMessage(queueUrl, message);
                    }
                }

                // Sleep between polling cycles
                if (pollIntervalSeconds > 0) {
                    Thread.sleep(pollIntervalSeconds * 1000L);
                }

            } catch (InterruptedException e) {
                logger.warn("Consumer polling interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error while polling SQS queue", e);
                try {
                    Thread.sleep(5000); // Wait before retrying on error
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("SQS consumer stopped");
    }

    private void processMessage(Message message) {
        logger.info("Processing message - MessageId: {}", message.messageId());
        logger.info("Message Body: {}", message.body());
        
        if (message.attributes() != null && !message.attributes().isEmpty()) {
            logger.debug("Message Attributes: {}", message.attributes());
        }
    }

    private void deleteMessage(String queueUrl, Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            
            client.deleteMessage(deleteRequest);
            logger.debug("Deleted message: {}", message.messageId());
        } catch (Exception e) {
            logger.error("Failed to delete message: {}", message.messageId(), e);
        }
    }

    public void stop() {
        logger.info("Stopping consumer...");
        running = false;
    }

    @Override
    public void close() {
        stop();
        client.close();
    }
}
