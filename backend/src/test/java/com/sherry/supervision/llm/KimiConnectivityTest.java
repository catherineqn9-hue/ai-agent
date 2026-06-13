package com.sherry.supervision.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class KimiConnectivityTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "KIMI_API_KEY", matches = ".+")
    void shouldAuthenticateWithKimiModelsEndpoint() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.moonshot.cn/v1/models"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + System.getenv("KIMI_API_KEY"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
