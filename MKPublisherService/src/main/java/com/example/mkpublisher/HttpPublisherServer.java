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
        server.createContext("/publish-dapr", new PublishDaprHandler());
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
            
            System.out.println("[PUBLISHER-SQS] Received POST request on /publish endpoint");
            System.out.println("[PUBLISHER-SQS] Message content: " + message);
            System.out.println("[PUBLISHER-SQS] Target queue: " + queueUrl);

            try (SqsPublisher publisher = new SqsPublisher(Region.of(regionStr))) {
                SendMessageResponse resp = publisher.publish(queueUrl, message);
                System.out.println("[PUBLISHER-SQS] SUCCESS - Message sent to SQS. MessageId: " + resp.messageId());
                
                String body = "{\"messageId\":\"" + resp.messageId() + "\",\"channel\":\"sqs\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                System.err.println("[PUBLISHER-SQS] FAILED - Error sending message to SQS: " + e.getMessage());
                e.printStackTrace();
                
                byte[] b = ("Failed to send: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, b.length);
                exchange.getResponseBody().write(b);
            } finally {
                exchange.close();
            }
        }
    }

    // Handler for Dapr publishing
    static class PublishDaprHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream is = exchange.getRequestBody();
            String message = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            System.out.println("[PUBLISHER-DAPR] Received POST request on /publish-dapr endpoint");
            System.out.println("[PUBLISHER-DAPR] Message content: " + message);
            System.out.println("[PUBLISHER-DAPR] Publishing via Dapr to mkpublisher-dapr-queue");

            try {
                DaprPublisher daprPublisher = new DaprPublisher();
                String result = daprPublisher.publish(message);
                System.out.println("[PUBLISHER-DAPR] SUCCESS - " + result);
                
                String body = "{\"status\":\"success\",\"message\":\"Published via Dapr\",\"channel\":\"dapr\"}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (Exception e) {
                System.err.println("[PUBLISHER-DAPR] FAILED - Error publishing via Dapr: " + e.getMessage());
                e.printStackTrace();
                
                byte[] b = ("Failed to send via Dapr: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, b.length);
                exchange.getResponseBody().write(b);
            } finally {
                exchange.close();
            }
        }
    }
}
