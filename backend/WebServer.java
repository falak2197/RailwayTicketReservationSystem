import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * WebServer.java
 * Embedded Core Java HTTP server (zero external dependencies) that serves
 * the web frontend and exposes REST API endpoints backed by RailwayService.
 */
public class WebServer {

    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        Path frontendDir = resolveFrontendDir();

        RailwayService railwayService = new RailwayService();
        DataStore.loadState(railwayService);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiHandler(railwayService));
        server.createContext("/", new StaticFileHandler(frontendDir));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("====================================================");
        System.out.println("  Railway Ticket Reservation System - Web Server");
        System.out.println("====================================================");
        System.out.println("  Web Frontend : http://localhost:" + port);
        System.out.println("  API Base URL : http://localhost:" + port + "/api");
        System.out.println("  Static Root  : " + frontendDir);
        System.out.println("  Console Mode : java -cp out Main");
        System.out.println("====================================================");
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0) {
            try {
                return Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                // Fall back to environment variable or default port
            }
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                return Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {
                // Fall back to default port
            }
        }
        return DEFAULT_PORT;
    }

    private static Path resolveFrontendDir() {
        Path direct = Paths.get("frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(direct)) {
            return direct;
        }
        Path parent = Paths.get("..", "frontend").toAbsolutePath().normalize();
        if (Files.isDirectory(parent)) {
            return parent;
        }
        return direct;
    }

    private static final class StaticFileHandler implements HttpHandler {
        private final Path frontendRoot;

        StaticFileHandler(Path frontendRoot) {
            this.frontendRoot = frontendRoot;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod().toUpperCase();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                sendText(exchange, 405, "Method Not Allowed");
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath == null || "/".equals(requestPath)) {
                requestPath = "/index.html";
            }

            String relative = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            Path target = frontendRoot.resolve(relative).normalize();

            // Prevent directory traversal outside frontend/
            if (!target.startsWith(frontendRoot)) {
                sendText(exchange, 403, "Forbidden");
                return;
            }

            if (Files.isDirectory(target)) {
                target = target.resolve("index.html");
            }

            if (!Files.exists(target) || !Files.isRegularFile(target)) {
                sendText(exchange, 404, "File not found: " + requestPath);
                return;
            }

            byte[] content = Files.readAllBytes(target);
            exchange.getResponseHeaders().set("Content-Type", getContentType(target.getFileName().toString()));
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");

            if ("HEAD".equals(method)) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            }
        }

        private String getContentType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css"))  return "text/css; charset=UTF-8";
            if (lower.endsWith(".js"))   return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg"))  return "image/svg+xml";
            if (lower.endsWith(".png"))  return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico"))  return "image/x-icon";
            return "application/octet-stream";
        }

        private void sendText(HttpExchange exchange, int status, String text) throws IOException {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
