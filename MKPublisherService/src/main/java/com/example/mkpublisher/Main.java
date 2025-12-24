package com.example.mkpublisher;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length >= 1 && "server".equalsIgnoreCase(args[0])) {
            int port = 8080;
            if (args.length >= 2) {
                try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
            }

            HttpPublisherServer server = new HttpPublisherServer(port);
            server.start();
            System.out.println("HTTP server listening on port " + port + " - POST /publish?queueUrl=<url>&region=<region> with raw body as message");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.stop(0);
            }));

            // block main thread
            synchronized (Main.class) {
                try { Main.class.wait(); } catch (InterruptedException ignored) {}
            }
            return;
        }

        if (args.length < 1) {
            System.err.println("Usage: java -jar mkpublisher.jar <queueUrl> <message> [region]\n       or: java -jar mkpublisher.jar <message> (uses default queue from config)\n       or: java -jar mkpublisher.jar server [port]");
            System.exit(1);
        }

        String queueUrl;
        String message;
        if (args.length == 1) {
            // only message provided, try to get queueUrl from config
            queueUrl = Config.getDefaultQueueUrl();
            message = args[0];
            if (queueUrl == null || queueUrl.isBlank()) {
                System.err.println("No default QUEUE_URL configured; provide queueUrl as first argument or set config.properties/QUEUE_URL env var");
                System.exit(1);
            }
        } else {
            queueUrl = args[0];
            message = args[1];
        }

        String regionStr = args.length >= 3 ? args[2] : Config.getDefaultRegion();

        Region region = Region.of(regionStr);

        try (SqsPublisher publisher = new SqsPublisher(region)) {
            SendMessageResponse resp = publisher.publish(queueUrl, message);
            System.out.println("Message sent. MessageId: " + resp.messageId());
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
