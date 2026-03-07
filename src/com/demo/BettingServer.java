package com.demo;

import com.demo.handler.HighStakesHandler;
import com.demo.handler.SessionHandler;
import com.demo.handler.StakeHandler;
import com.demo.service.SessionService;
import com.demo.service.StakeService;
import com.demo.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * HTTP server.
 */
public class BettingServer {

    private static final int PORT = 8001;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    public static void main(String[] args) throws IOException {
        SessionService sessionService = new SessionService();
        StakeService stakeService = new StakeService();

        //biz hundlers
        SessionHandler sessionHandler = new SessionHandler(sessionService);
        StakeHandler stakeHandler = new StakeHandler(sessionService, stakeService);
        HighStakesHandler highStakesHandler = new HighStakesHandler(stakeService);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        //endpoints
        server.createContext("/", exchange -> {
            long startTime = System.currentTimeMillis();
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            System.out.println("path:" + path);
            try {
                int id = HttpUtils.parsePathId(path);
                //split url  parts[1]: url params, parts[2]: action
                String[] parts = path.split("/");
                if (id < 0 || parts.length != 3) {
                    HttpUtils.sendResponse(exchange, 404, "Not Found");
                    return;
                }
                switch (parts[2].toLowerCase()) {
                    case "session":
                        handleSession(exchange, sessionHandler, id);
                        return;
                    case "stake":
                        handleStake(exchange, stakeHandler, id);
                        return;
                    case "highstakes":
                        handleHighStakes(exchange, highStakesHandler, id);
                        return;
                    default:
                        break;
                }
                HttpUtils.sendResponse(exchange, 404, "Not Found");
            } catch (Exception e) {
                System.err.println("unmatched url method: " + method + " path: " + path);
                System.err.println("error msg: " + e.getMessage());
                try {
                    HttpUtils.sendResponse(exchange, 500, "Internal Server Error");
                } catch (IOException ex) {
                    System.err.println("unmatched url method: " + method + " path: " + path);
                }
            } finally {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("method: " + method + " path: " + path + " completed in: " + elapsed + "ms");
            }
        });

        server.setExecutor(executor);
        server.start();

        System.out.println("Betting Server started on port " + PORT);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down");
            server.stop(2);
            sessionService.shutdown();
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("Server stopped");
        }));
    }


    private static void handleSession(HttpExchange exchange, SessionHandler handler, int customerId)
            throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            String sessionKey = handler.handle(customerId);
            HttpUtils.sendResponse(exchange, 200, sessionKey);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendResponse(exchange, 400, e.getMessage());
        }
    }

    private static void handleStake(HttpExchange exchange, StakeHandler handler, int betOfferId)
            throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }

        String sessionKey = parseQueryParam(exchange.getRequestURI().getQuery(), "sessionkey");
        if (sessionKey == null || sessionKey.isEmpty()) {
            HttpUtils.sendResponse(exchange, 400, "Missing session key");
            return;
        }

        String body = readRequestBody(exchange);
        int stake;
        try {
            stake = Integer.parseInt(body.trim());
        } catch (NumberFormatException e) {
            HttpUtils.sendResponse(exchange, 400, "Invalid stake value");
            return;
        }

        try {
            handler.handle(betOfferId, sessionKey, stake);
            HttpUtils.sendResponse(exchange, 200, "");
        } catch (IllegalArgumentException e) {
            HttpUtils.sendResponse(exchange, 400, e.getMessage());
        }
    }

    private static void handleHighStakes(HttpExchange exchange, HighStakesHandler handler, int betOfferId)
            throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            String result = handler.handle(betOfferId);
            HttpUtils.sendResponse(exchange, 200, result);
        } catch (IllegalArgumentException e) {
            HttpUtils.sendResponse(exchange, 400, e.getMessage());
        }
    }

    private static String parseQueryParam(String query, String paramName) {
        if (query == null)
            return null;
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2 && paramName.equalsIgnoreCase(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }
}
