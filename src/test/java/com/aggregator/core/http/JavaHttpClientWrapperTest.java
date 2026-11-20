package com.aggregator.core.http;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaHttpClientWrapperTest {
    private HttpServer server;
    private JavaHttpClientWrapper client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ok", exchange -> {
            String response = "{\"status\":\"ok\"}";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.createContext("/fail", exchange -> {
            String response = "{\"error\":\"server error\"}";
            exchange.sendResponseHeaders(500, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;

        client = new JavaHttpClientWrapper(Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void shouldReturnBodyOn200() {
        String body = client.get(baseUrl + "/ok");
        assertThat(body).contains("\"status\":\"ok\"");
    }

    @Test
    void shouldThrowHttpExceptionOnNon200() {
        assertThatThrownBy(() -> client.get(baseUrl + "/fail"))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void shouldThrowHttpExceptionOnConnectionRefused() {
        assertThatThrownBy(() -> client.get("http://localhost:1/nothing"))
                .isInstanceOf(HttpException.class)
                .hasMessageContaining("Request failed");
    }

    @Test
    void shouldThrowHttpExceptionOnInvalidUrl() {
        assertThatThrownBy(() -> client.get("not a url"))
                .isInstanceOf(HttpException.class);
    }
}
