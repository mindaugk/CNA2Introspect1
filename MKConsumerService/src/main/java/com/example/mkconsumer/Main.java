package com.example.mkconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting MKConsumer Service with dual mode: SQS polling + Dapr subscription");
        
        // Start Dapr subscription server for mkpublisher-dapr-queue
        startDaprServer();
        
        // Start SQS polling for mkpublisher-queue (original functionality)
        startSqsPolling();
    }
    
    private static void startDaprServer() {
        new Thread(() -> {
            try {
                int port = 8080;
                DaprSubscriptionServer server = new DaprSubscriptionServer(port);
                server.start();
                logger.info("Dapr HTTP server started on port {} for mkpublisher-dapr-queue", port);
            } catch (Exception e) {
                logger.error("Failed to start Dapr server", e);
            }
        }).start();
    }
    
    private static void startSqsPolling() {
        String queueUrl = Config.getDefaultQueueUrl();
        String regionStr = Config.getDefaultRegion();
        
        if (queueUrl == null || queueUrl.isBlank()) {
            logger.error("No QUEUE_URL configured for SQS polling");
            return;
        }
        
        logger.info("Starting SQS polling for mkpublisher-queue");
        logger.info("Queue URL: {}", queueUrl);
        logger.info("Region: {}", regionStr);

        Region region = Region.of(regionStr);
        int maxMessages = Config.getMaxMessages();
        int waitTimeSeconds = Config.getWaitTimeSeconds();
        int pollIntervalSeconds = Config.getPollIntervalSeconds();

        SqsConsumer consumer = new SqsConsumer(region);

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            consumer.stop();
        }));

        try {
            consumer.startPolling(queueUrl, maxMessages, waitTimeSeconds, pollIntervalSeconds);
        } catch (Exception e) {
            logger.error("Fatal error in SQS consumer", e);
            System.exit(2);
        }
    }
}
