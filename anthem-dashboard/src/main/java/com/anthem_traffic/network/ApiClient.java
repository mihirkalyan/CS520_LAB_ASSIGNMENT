package com.anthem_traffic.network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.anthem_traffic.config.ApiConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Utility class for making API calls to the backend.
 * Handles HTTP communication, JSON serialization/deserialization, and error handling.
 */
public class ApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(ApiConfig.CONNECT_TIMEOUT_SECONDS))
            .build();

    private static final Gson GSON = new Gson();

    /**
     * Make a GET request to the backend
     * @param endpoint The API endpoint (e.g., "/api/traffic/live")
     * @param queryParams Query parameters as a query string (e.g., "from=2024-01-01&to=2024-01-31")
     * @param responseClass The class to deserialize the response into
     * @return The deserialized response object, or null if request fails
     */
    public static <T> T get(String endpoint, String queryParams, Class<T> responseClass) {
        String url = ApiConfig.getFullUrl(endpoint);
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                try {
                    return GSON.fromJson(response.body(), responseClass);
                } catch (JsonSyntaxException e) {
                    System.err.println("Failed to parse JSON response: " + e.getMessage());
                    return null;
                }
            } else {
                System.err.println("GET " + endpoint + " returned status: " + response.statusCode());
                System.err.println("Response body: " + response.body());
                return null;
            }
        } catch (IOException e) {
            System.err.println("IOException during GET " + endpoint + ": " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("InterruptedException during GET " + endpoint + ": " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Make a GET request without query parameters
     */
    public static <T> T get(String endpoint, Class<T> responseClass) {
        return get(endpoint, null, responseClass);
    }

    /**
     * Make a POST request to the backend
     * @param endpoint The API endpoint
     * @param requestBody The object to serialize and send as JSON
     * @param responseClass The class to deserialize the response into
     * @return The deserialized response object, or null if request fails
     */
    public static <T> T post(String endpoint, Object requestBody, Class<T> responseClass) {
        String url = ApiConfig.getFullUrl(endpoint);
        String jsonBody = GSON.toJson(requestBody);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try {
                    if (response.body().isEmpty()) {
                        return null; // No response body
                    }
                    return GSON.fromJson(response.body(), responseClass);
                } catch (JsonSyntaxException e) {
                    System.err.println("Failed to parse JSON response: " + e.getMessage());
                    return null;
                }
            } else {
                System.err.println("POST " + endpoint + " returned status: " + response.statusCode());
                System.err.println("Response body: " + response.body());
                return null;
            }
        } catch (IOException e) {
            System.err.println("IOException during POST " + endpoint + ": " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("InterruptedException during POST " + endpoint + ": " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Make a POST request without a request body
     */
    public static <T> T post(String endpoint, Class<T> responseClass) {
        return post(endpoint, "", responseClass);
    }

    /**
     * Check if backend is reachable
     * @return true if backend responds to health check, false otherwise
     */
    public static boolean isBackendHealthy() {
        try {
            String url = ApiConfig.getFullUrl(ApiConfig.HEALTH_CHECK);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Download a file from the specified URL and return as byte array
     */
    public static byte[] downloadFile(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(ApiConfig.REQUEST_TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Download failed with status: " + response.statusCode());
                return null;
            }
        } catch (IOException e) {
            System.err.println("IOException during file download: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            System.err.println("InterruptedException during file download: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
