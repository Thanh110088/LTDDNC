package com.btlltdd.fitnesstracker.network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** HttpURLConnection thuần, không dùng thư viện HTTP. */
public final class HttpHelper {
    private static final String[] ENDPOINTS = {
            "https://httpbin.org/post",
            "https://postman-echo.com/post"
    };

    private HttpHelper() {
    }

    public static int postJson(String json) throws IOException {
        IOException last = null;
        for (String endpoint : ENDPOINTS) {
            try {
                return post(endpoint, json);
            } catch (IOException exception) {
                last = exception;
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IOException("Không có máy chủ đồng bộ");
    }

    private static int post(String endpoint, String json) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body);
        }
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }
}
