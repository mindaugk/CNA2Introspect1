package com.example.mkconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String queueUrl;
        String regionStr;

        // Parse command line arguments or use config defaults
        if (args.length >= 1) {
            queueUrl = args[0];
            regionStr = args.length >= 2 ? args[1] : Config.getDefaultRegion();
        } else {
            queueUrl = Config.getDefaultQueueUrl();
            regionStr = Config.getDefaultRegion();
            
            if (queueUrl == null || queueUrl.isBlank()) {
                logger.error("No QUEUE_URL configured. Provide it as first argument or set config.properties/QUEUE_URL env var");
                System.err.println("Usage: java -jar mkconsumer.jar [queueUrl] [region]");
                System.exit(1);
            }
        }

        logger.info("Starting MKConsumer Service");
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
            logger.error("Fatal error in consumer", e);
            System.exit(2);
        } finally {
            consumer.close();
        }
    }
}
