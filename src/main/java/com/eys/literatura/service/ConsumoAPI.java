package com.eys.literatura.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class ConsumoAPI {
    private static final int TIMEOUT_SECONDS = 10;
    private static final String USER_AGENT = "Mozilla/5.0";

    public String obtenerDatos(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL) // Permite seguir redirecciones
                    .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Manejar códigos de estado
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else if (response.statusCode() == 301 || response.statusCode() == 302) {
                // Si recibimos una redirección, intentar con la nueva URL
                String nuevaUrl = response.headers().firstValue("Location")
                        .orElseThrow(() -> new RuntimeException("URL de redirección no encontrada"));
                return obtenerDatos(nuevaUrl);
            } else {
                throw new RuntimeException("Error al consumir la API. Código de estado: " + response.statusCode());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error de conexión: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("La operación fue interrumpida", e);
        }
    }
}