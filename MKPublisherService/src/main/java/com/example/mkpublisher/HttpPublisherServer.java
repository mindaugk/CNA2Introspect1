package com.example.mkpublisher;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpPublisherServer {
    private final HttpServer server;

    public HttpPublisherServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/publish", new PublishHandler());
        server.createContext("/health", new HealthHandler());
        server.setExecutor(null);
    }

    public void start() {
        server.start();
    }

    public void stop(int delay) {
        server.stop(delay);
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{\"status\":\"healthy\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }
    }

    static class PublishHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            Map<String, String> params = QueryUtils.parseQuery(query);
            String queueUrl = params.get("queueUrl");
            String regionStr = params.getOrDefault("region", Config.getDefaultRegion());

            if (queueUrl == null || queueUrl.isEmpty()) {
                queueUrl = Config.getDefaultQueueUrl();
                if (queueUrl == null || queueUrl.isEmpty()) {
                    byte[] resp = "Missing 'queueUrl' query parameter and no default configured".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, resp.length);
                    exchange.getResponseBody().write(resp);
                    exchange.close();
                    return;
                }
            }

            InputStream is = exchange.getRequestBody();
            String message = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            System.out.println("[PUBLISHER] Received POST request on /publish endpoint");
            System.out.println("[PUBLISHER] Message content: " + message);
            System.out.println("[PUBLISHER] Target queue: " + queueUrl);

            try (SqsPublisher publisher = new SqsPublisher(Region.of(regionStr))) {
                SendMessageResponse resp = publisher.publish(queueUrl, message);
                System.out.println("[PUBLISHER] SUCCESS - Message sent to SQS. MessageId: " + resp.messageId());
                
                String body = "{\"messageId\":\"" + resp.messageId() + "\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                System.err.println("[PUBLISHER] FAILED - Error sending message to SQS: " + e.getMessage());
                e.printStackTrace();
                
                byte[] b = ("Failed to send: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, b.length);
                exchange.getResponseBody().write(b);
            } finally {
                exchange.close();
            }
        }
    }
}
