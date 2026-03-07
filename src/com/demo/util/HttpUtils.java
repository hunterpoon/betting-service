package com.demo.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HTTP tools
 */
public class HttpUtils {

    private HttpUtils() {
    }

    /**
     * response http code and body
     */
    public static void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    /**
     * parse url path ID
     */
    public static int parsePathId(String path) {
        if (path == null || path.isEmpty()) {
            return -1;
        }
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slashIdx = trimmed.indexOf('/');
        if (slashIdx < 0) {
            return -1;
        }
        try {
            return Integer.parseInt(trimmed.substring(0, slashIdx));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
