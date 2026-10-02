package com.github.esrrhs.majiang_algorithm.web;

import com.github.esrrhs.majiang_algorithm.AIUtil;
import com.github.esrrhs.majiang_algorithm.HuUtil;
import com.github.esrrhs.majiang_algorithm.MaJiangDef;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * 轻量内置 HTTP 服务，提供静态前端展示与对局交互 REST API
 */
public class MahjongHttpServer {
    private final int port;
    private HttpServer server;
    private final Gson gson = new Gson();
    private MahjongGame currentGame;

    public MahjongHttpServer(int port) {
        this.port = port;
        this.currentGame = new MahjongGame("random_flip", 0, false);
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // API 路由
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/game/new", new NewGameHandler());
        server.createContext("/api/game/state", new StateHandler());
        server.createContext("/api/game/spectator", new SpectatorHandler());
        server.createContext("/api/game/discard", new DiscardHandler());
        server.createContext("/api/game/action", new ActionHandler());
        server.createContext("/api/game/self_action", new SelfActionHandler());
        server.createContext("/api/game/step", new StepHandler());
        server.createContext("/api/game/auto_run", new AutoRunHandler());
        server.createContext("/api/algo/test", new AlgoTestHandler());

        // 静态资源路由
        server.createContext("/", new StaticResourceHandler());

        server.start();
        System.out.println("=================================================");
        System.out.println(" 麻将网页端对战与算法演示平台已启动！");
        System.out.println(" 请在浏览器中打开: http://localhost:" + port);
        System.out.println("=================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ok");
            resp.put("ready", true);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class NewGameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readBody(exchange);
            JsonObject json = body.isEmpty() ? new JsonObject() : gson.fromJson(body, JsonObject.class);

            String guiMode = json.has("guiMode") ? json.get("guiMode").getAsString() : "random_flip";
            int customGuiCard = json.has("customGuiCard") ? json.get("customGuiCard").getAsInt() : 0;
            boolean spectator = json.has("spectator") && json.get("spectator").getAsBoolean();

            synchronized (MahjongHttpServer.this) {
                currentGame = new MahjongGame(guiMode, customGuiCard, spectator);
            }

            GameStateView view = currentGame.createView(0);
            sendJsonResponse(exchange, 200, view);
        }
    }

    private class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            GameStateView view;
            synchronized (MahjongHttpServer.this) {
                view = currentGame.createView(0);
            }
            sendJsonResponse(exchange, 200, view);
        }
    }

    private class SpectatorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                boolean spectator = json.get("spectator").getAsBoolean();
                synchronized (MahjongHttpServer.this) {
                    currentGame.setSpectatorMode(spectator);
                }
                GameStateView view;
                synchronized (MahjongHttpServer.this) {
                    view = currentGame.createView(0);
                }
                sendJsonResponse(exchange, 200, view);
            } catch (Throwable t) {
                t.printStackTrace();
                Map<String, Object> err = new HashMap<>();
                err.put("error", t.getMessage() != null ? t.getMessage() : "Internal Error");
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    private class DiscardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                int card = json.get("card").getAsInt();

                boolean success;
                synchronized (MahjongHttpServer.this) {
                    success = currentGame.discard(0, card);
                }
                GameStateView view = currentGame.createView(0);
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", success);
                resp.put("view", view);
                sendJsonResponse(exchange, 200, resp);
            } catch (Throwable t) {
                t.printStackTrace();
                Map<String, Object> err = new HashMap<>();
                err.put("error", t.getMessage() != null ? t.getMessage() : "Internal Error");
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    private class ActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                String action = json.get("action").getAsString();
                int card = json.has("card") ? json.get("card").getAsInt() : 0;
                int chi1 = json.has("chi1") ? json.get("chi1").getAsInt() : 0;
                int chi2 = json.has("chi2") ? json.get("chi2").getAsInt() : 0;

                boolean success;
                synchronized (MahjongHttpServer.this) {
                    success = currentGame.userAction(action, card, chi1, chi2);
                }
                GameStateView view = currentGame.createView(0);
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", success);
                resp.put("view", view);
                sendJsonResponse(exchange, 200, resp);
            } catch (Throwable t) {
                t.printStackTrace();
                Map<String, Object> err = new HashMap<>();
                err.put("error", t.getMessage() != null ? t.getMessage() : "Internal Error");
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    private class SelfActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            try {
                String body = readBody(exchange);
                JsonObject json = gson.fromJson(body, JsonObject.class);
                String action = json.get("action").getAsString();
                int card = json.has("card") ? json.get("card").getAsInt() : 0;

                boolean success;
                synchronized (MahjongHttpServer.this) {
                    success = currentGame.userSelfAction(action, card);
                }
                GameStateView view = currentGame.createView(0);
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", success);
                resp.put("view", view);
                sendJsonResponse(exchange, 200, resp);
            } catch (Throwable t) {
                t.printStackTrace();
                Map<String, Object> err = new HashMap<>();
                err.put("error", t.getMessage() != null ? t.getMessage() : "Internal Error");
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    private class StepHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean stepped;
            synchronized (MahjongHttpServer.this) {
                stepped = currentGame.step();
            }
            GameStateView view = currentGame.createView(0);
            Map<String, Object> resp = new HashMap<>();
            resp.put("stepped", stepped);
            resp.put("view", view);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class AutoRunHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = readBody(exchange);
            JsonObject json = body.isEmpty() ? new JsonObject() : gson.fromJson(body, JsonObject.class);
            int maxSteps = json.has("maxSteps") ? json.get("maxSteps").getAsInt() : 10;

            int executedSteps = 0;
            synchronized (MahjongHttpServer.this) {
                for (int i = 0; i < maxSteps; i++) {
                    if (!currentGame.step()) {
                        break;
                    }
                    executedSteps++;
                }
            }
            GameStateView view = currentGame.createView(0);
            Map<String, Object> resp = new HashMap<>();
            resp.put("executedSteps", executedSteps);
            resp.put("view", view);
            sendJsonResponse(exchange, 200, resp);
        }
    }

    private class AlgoTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            String body = readBody(exchange);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            List<Integer> cards = new ArrayList<>();
            if (json.has("cards")) {
                if (json.get("cards").isJsonArray()) {
                    json.getAsJsonArray("cards").forEach(el -> cards.add(el.getAsInt()));
                } else {
                    String str = json.get("cards").getAsString();
                    cards.addAll(MaJiangDef.stringToCards(str));
                }
            }

            List<Integer> guiCards = new ArrayList<>();
            if (json.has("guiCards")) {
                if (json.get("guiCards").isJsonArray()) {
                    json.getAsJsonArray("guiCards").forEach(el -> guiCards.add(el.getAsInt()));
                } else {
                    String str = json.get("guiCards").getAsString();
                    guiCards.addAll(MaJiangDef.stringToCards(str));
                }
            } else if (json.has("gui")) {
                int gui = json.get("gui").getAsInt();
                if (gui > 0) guiCards.add(gui);
            }

            try {
                Collections.sort(cards);

                long start = System.nanoTime();
                boolean isHu = false;
                if (!cards.isEmpty() && cards.size() % 3 == 2) {
                    isHu = HuUtil.isHuExtra(cards, guiCards, 0);
                }
                long huNanos = System.nanoTime() - start;

                start = System.nanoTime();
                List<Integer> tingCards = Collections.emptyList();
                if (!cards.isEmpty()) {
                    tingCards = HuUtil.isTingExtra(cards, guiCards);
                    if (tingCards == null) tingCards = Collections.emptyList();
                }
                long tingNanos = System.nanoTime() - start;

                int recommendedOut = 0;
                double aiScore = 0;
                long aiNanos = 0;
                if (!cards.isEmpty() && cards.size() % 3 == 2) {
                    start = System.nanoTime();
                    recommendedOut = AIUtil.outAI(cards, guiCards);
                    if (recommendedOut > 0) {
                        List<Integer> rem = new ArrayList<>(cards);
                        rem.remove((Integer) recommendedOut);
                        aiScore = AIUtil.calc(rem, guiCards);
                    }
                    aiNanos = System.nanoTime() - start;
                }

                Map<String, Object> resp = new HashMap<>();
                resp.put("cards", cards);
                resp.put("cardsStr", MaJiangDef.cardsToString(cards));
                resp.put("guiCards", guiCards);
                resp.put("isHu", isHu);
                resp.put("huTimeMicros", huNanos / 1000.0);
                resp.put("tingCards", tingCards);
                resp.put("tingCardsStr", MaJiangDef.cardsToString(tingCards));
                resp.put("tingTimeMicros", tingNanos / 1000.0);
                resp.put("recommendedOut", recommendedOut);
                resp.put("recommendedOutStr", recommendedOut > 0 ? MaJiangDef.cardToString(recommendedOut) : "");
                resp.put("aiScore", aiScore);
                resp.put("aiTimeMicros", aiNanos / 1000.0);

                sendJsonResponse(exchange, 200, resp);
            } catch (Exception e) {
                e.printStackTrace();
                Map<String, Object> err = new HashMap<>();
                err.put("error", e.getMessage());
                sendJsonResponse(exchange, 500, err);
            }
        }
    }

    private class StaticResourceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // 防路径遍历
            if (path.contains("..")) {
                sendNotFound(exchange);
                return;
            }

            // 从资源或本地文件查找
            InputStream in = getClass().getResourceAsStream("/static" + path);
            if (in == null) {
                File localFile = new File("src/main/resources/static" + path);
                if (localFile.exists() && localFile.isFile()) {
                    in = new FileInputStream(localFile);
                }
            }

            if (in == null) {
                sendNotFound(exchange);
                return;
            }

            String contentType = getMimeType(path);
            exchange.getResponseHeaders().set("Content-Type", contentType);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            in.close();

            byte[] data = baos.toByteArray();
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        private void sendNotFound(HttpExchange exchange) throws IOException {
            String resp = "404 Not Found";
            exchange.sendResponseHeaders(404, resp.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp.getBytes(StandardCharsets.UTF_8));
            }
        }

        private String getMimeType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
            return "application/octet-stream";
        }
    }
}
