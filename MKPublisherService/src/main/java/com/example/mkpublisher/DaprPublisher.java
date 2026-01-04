package com.example.mkpublisher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class DaprPublisher {
    private static final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static final String DAPR_URL = "http://localhost:" + DAPR_HTTP_PORT + "/v1.0/publish/aws-pubsub/mkpublisher-dapr-topic";
    
    private final HttpClient httpClient;

    public DaprPublisher() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String publish(String messageBody) throws IOException, InterruptedException {
        System.out.println("[DAPR-PUBLISHER] Publishing message to Dapr pubsub");
        System.out.println("[DAPR-PUBLISHER] Message: " + messageBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DAPR_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(messageBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("[DAPR-PUBLISHER] SUCCESS - Message published via Dapr. Status: " + response.statusCode());
            return "Message published successfully";
        } else {
            System.err.println("[DAPR-PUBLISHER] FAILED - Status: " + response.statusCode() + ", Body: " + response.body());
            throw new IOException("Failed to publish message. Status: " + response.statusCode());
        }
    }
}
