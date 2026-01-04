package com.example.mkconsumer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class DaprSubscriptionServer {
    private static final Logger logger = LoggerFactory.getLogger(DaprSubscriptionServer.class);
    private final HttpServer server;

    public DaprSubscriptionServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/messages", new MessageHandler());
        server.createContext("/dapr/subscribe", new SubscribeHandler());
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop(int delay) {
        server.stop(delay);
    }

    // Handler for Dapr subscription discovery
    static class SubscribeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "[{\"pubsubname\":\"aws-pubsub\",\"topic\":\"mkpublisher-dapr-queue\",\"route\":\"/messages\"}]";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    // Handler for receiving messages from Dapr
    static class MessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream is = exchange.getRequestBody();
            String message = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            logger.info("[CONSUMER-DAPR] Received message from Dapr (mkpublisher-dapr-queue)");
            logger.info("[CONSUMER-DAPR] Message: {}", message);
            
            // Process the message
            try {
                // Your message processing logic here
                logger.info("[CONSUMER-DAPR] Message processed successfully");
                
                // Respond with success
                String response = "{\"status\":\"SUCCESS\"}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                logger.error("[CONSUMER-DAPR] Error processing message", e);
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        }
    }
}
