package com.aggregator.core.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JavaHttpClientWrapper implements HttpClientWrapper {
    private static final Logger log = LoggerFactory.getLogger(JavaHttpClientWrapper.class);
    private final HttpClient client;
    private final Duration timeout;

    public JavaHttpClientWrapper(Duration timeout) {
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String get(String url) {
        log.info("HTTP GET {}", url);

        long startTime = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("HTTP GET {} -> {} in {} ms", url, response.statusCode(), elapsed);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new HttpException("HTTP " + response.statusCode() + " for " + url);
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("HTTP GET {} failed in {} ms: {}", url, elapsed, e.getMessage());
            throw new HttpException("Request failed: " + url, e);
        }
    }
}
