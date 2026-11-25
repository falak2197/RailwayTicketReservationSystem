import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ApiHandler.java
 * REST API controller bridging HTTP requests from the web frontend
 * directly to RailwayService and the existing Java business classes.
 */
public class ApiHandler implements HttpHandler {

    private final RailwayService railwayService;
    private final Object lock = new Object();

    public ApiHandler(RailwayService railwayService) {
        this.railwayService = railwayService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange.getResponseHeaders());

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        String path = normalizePath(rawPath);
        Map<String, String> query = parseQueryParams(exchange.getRequestURI().getRawQuery());

        try {
            if ("GET".equals(method) && "/api/trains".equals(path)) {
                handleGetAllTrains(exchange);
                return;
            }
            if ("GET".equals(method) && "/api/trains/search".equals(path)) {
                handleSearchTrains(exchange, query);
                return;
            }
            if ("GET".equals(method) && "/api/fare".equals(path)) {
                handleCalculateFare(exchange, query);
                return;
            }
            if ("GET".equals(method) && "/api/bookings".equals(path)) {
                handleGetAllBookings(exchange);
                return;
            }
            if ("POST".equals(method) && "/api/bookings".equals(path)) {
                handleCreateBooking(exchange);
                return;
            }
            if ("POST".equals(method) && "/api/contact".equals(path)) {
                handleContactSubmission(exchange);
                return;
            }

            // Parameterized routes under /api/
            if (path.startsWith("/api/")) {
                String subPath = path.substring("/api/".length());
                String[] parts = subPath.split("/");

                if ("GET".equals(method) && parts.length == 2 && "trains".equals(parts[0])) {
                    handleGetTrainByNumber(exchange, parts[1]);
                    return;
                }
                if ("GET".equals(method) && parts.length == 3 && "trains".equals(parts[0]) && "availability".equals(parts[2])) {
                    handleCheckSeatAvailability(exchange, parts[1]);
                    return;
                }
                if ("GET".equals(method) && parts.length == 2 && "bookings".equals(parts[0])) {
                    handleGetBookingByPnr(exchange, parts[1]);
                    return;
                }
                if ("POST".equals(method) && parts.length == 3 && "bookings".equals(parts[0]) && "cancel".equals(parts[2])) {
                    handleCancelBooking(exchange, parts[1]);
                    return;
                }
            }

            sendJson(exchange, 404, JsonUtil.errorToJson("API endpoint not found."));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, JsonUtil.errorToJson(e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, JsonUtil.errorToJson(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, JsonUtil.errorToJson("Internal server error."));
        }
    }

    private void handleGetAllTrains(HttpExchange exchange) throws IOException {
        String json;
        synchronized (lock) {
            json = JsonUtil.trainsToJson(railwayService.getAllTrains());
        }
        sendJson(exchange, 200, json);
    }

    private void handleSearchTrains(HttpExchange exchange, Map<String, String> query) throws IOException {
        String source = query.get("source");
        String destination = query.get("destination");

        if (source == null || source.trim().isEmpty() || destination == null || destination.trim().isEmpty()) {
            sendJson(exchange, 400, JsonUtil.errorToJson("Both source and destination are required."));
            return;
        }

        String json;
        synchronized (lock) {
            ArrayList<Train> matches = railwayService.searchTrains(source, destination);
            json = JsonUtil.trainsToJson(matches);
        }
        sendJson(exchange, 200, json);
    }

    private void handleGetTrainByNumber(HttpExchange exchange, String trainNumberStr) throws IOException {
        int trainNumber = parseTrainNumber(trainNumberStr);
        Train train;
        synchronized (lock) {
            train = railwayService.findTrainByNumber(trainNumber);
        }
        if (train == null) {
            sendJson(exchange, 404, JsonUtil.errorToJson("Train not found."));
            return;
        }
        sendJson(exchange, 200, JsonUtil.trainToJson(train));
    }

    private void handleCheckSeatAvailability(HttpExchange exchange, String trainNumberStr) throws IOException {
        int trainNumber = parseTrainNumber(trainNumberStr);
        Train train;
        synchronized (lock) {
            train = railwayService.findTrainByNumber(trainNumber);
        }
        if (train == null) {
            sendJson(exchange, 404, JsonUtil.errorToJson("Train not found."));
            return;
        }
        int bookedSeats = train.getTotalSeats() - train.getAvailableSeats();
        String json = "{"
                + "\"trainNumber\":" + train.getTrainNumber() + ","
                + "\"trainName\":\"" + JsonUtil.escape(train.getTrainName()) + "\","
                + "\"totalSeats\":" + train.getTotalSeats() + ","
                + "\"availableSeats\":" + train.getAvailableSeats() + ","
                + "\"bookedSeats\":" + bookedSeats
                + "}";
        sendJson(exchange, 200, json);
    }

    private void handleCalculateFare(HttpExchange exchange, Map<String, String> query) throws IOException {
        String trainStr = query.get("trainNumber");
        String countStr = query.get("passengers");

        if (trainStr == null || trainStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Train number is required.");
        }
        if (countStr == null || countStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Passenger count is required.");
        }

        int trainNumber = parseTrainNumber(trainStr);
        int passengerCount;
        try {
            passengerCount = Integer.parseInt(countStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Passenger count must be a whole number.");
        }

        double totalFare;
        double farePerSeat;
        synchronized (lock) {
            Train train = railwayService.findTrainByNumber(trainNumber);
            if (train == null) {
                sendJson(exchange, 404, JsonUtil.errorToJson("Train not found."));
                return;
            }
            farePerSeat = train.getFarePerSeat();
            totalFare = railwayService.calculateFare(trainNumber, passengerCount);
        }

        String json = "{"
                + "\"trainNumber\":" + trainNumber + ","
                + "\"farePerSeat\":" + farePerSeat + ","
                + "\"passengerCount\":" + passengerCount + ","
                + "\"totalFare\":" + totalFare
                + "}";
        sendJson(exchange, 200, json);
    }

    @SuppressWarnings("unchecked")
    private void handleCreateBooking(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange.getRequestBody());
        Map<String, Object> payload = JsonUtil.parseObject(body);

        int trainNumber = extractInt(payload.get("trainNumber"), "Invalid train number.");
        Object rawPassengers = payload.get("passengers");
        if (!(rawPassengers instanceof List)) {
            throw new IllegalArgumentException("At least one passenger is required.");
        }

        List<Object> passengerItems = (List<Object>) rawPassengers;
        if (!PassengerValidator.isValidPassengerCount(passengerItems.size())) {
            throw new IllegalArgumentException("At least one passenger is required.");
        }

        Booking booking;
        synchronized (lock) {
            Train train = railwayService.findTrainByNumber(trainNumber);
            if (train == null) {
                sendJson(exchange, 404, JsonUtil.errorToJson("Train not found."));
                return;
            }
            if (train.getAvailableSeats() == 0) {
                throw new IllegalArgumentException("Sorry, this train is full.");
            }
            if (passengerItems.size() > train.getAvailableSeats()) {
                throw new IllegalArgumentException("Not enough seats! Only "
                        + train.getAvailableSeats() + " seat(s) available.");
            }

            PassengerManager pm = railwayService.getWebPassengerManager();
            pm.clearPassengers();

            for (int i = 0; i < passengerItems.size(); i++) {
                Object item = passengerItems.get(i);
                if (!(item instanceof Map)) {
                    throw new IllegalArgumentException("Invalid passenger entry at index " + (i + 1) + ".");
                }
                Map<String, Object> pMap = (Map<String, Object>) item;
                String name = pMap.get("name") == null ? null : String.valueOf(pMap.get("name"));
                int age = extractInt(pMap.get("age"), "Invalid age for passenger " + (i + 1) + ". Age must be a positive number.");
                String gender = pMap.get("gender") == null ? null : String.valueOf(pMap.get("gender"));
                String phoneNumber = pMap.get("phoneNumber") == null ? null : String.valueOf(pMap.get("phoneNumber"));

                try {
                    pm.addPassenger(name, age, gender, phoneNumber);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Passenger " + (i + 1) + ": " + e.getMessage());
                }
            }

            ArrayList<Passenger> validatedPassengers = pm.getPassengers();
            booking = railwayService.createBooking(trainNumber, validatedPassengers);
            DataStore.saveState(railwayService);
        }

        sendJson(exchange, 201, JsonUtil.bookingToJson(booking));
    }

    private void handleGetBookingByPnr(HttpExchange exchange, String pnrStr) throws IOException {
        long pnr = parsePnr(pnrStr);
        Booking booking;
        synchronized (lock) {
            booking = railwayService.findBookingByPnr(pnr);
        }
        if (booking == null) {
            sendJson(exchange, 404, JsonUtil.errorToJson("Booking not found!"));
            return;
        }
        sendJson(exchange, 200, JsonUtil.bookingToJson(booking));
    }

    private void handleCancelBooking(HttpExchange exchange, String pnrStr) throws IOException {
        long pnr = parsePnr(pnrStr);
        Booking cancelledBooking;
        synchronized (lock) {
            Booking existing = railwayService.findBookingByPnr(pnr);
            if (existing == null) {
                sendJson(exchange, 404, JsonUtil.errorToJson("Booking not found!"));
                return;
            }
            cancelledBooking = railwayService.cancelTicket(pnr);
            DataStore.saveState(railwayService);
        }

        String json = "{"
                + "\"message\":\"Ticket Cancelled Successfully! Available Seats Updated!\","
                + "\"booking\":" + JsonUtil.bookingToJson(cancelledBooking)
                + "}";
        sendJson(exchange, 200, json);
    }

    private void handleGetAllBookings(HttpExchange exchange) throws IOException {
        String json;
        synchronized (lock) {
            json = JsonUtil.bookingsToJson(railwayService.getAllBookings());
        }
        sendJson(exchange, 200, json);
    }

    private void handleContactSubmission(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange.getRequestBody());
        Map<String, Object> payload = JsonUtil.parseObject(body);

        String name = payload.get("name") == null ? "" : String.valueOf(payload.get("name")).trim();
        String email = payload.get("email") == null ? "" : String.valueOf(payload.get("email")).trim();
        String subject = payload.get("subject") == null ? "" : String.valueOf(payload.get("subject")).trim();
        String message = payload.get("message") == null ? "" : String.valueOf(payload.get("message")).trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Please enter your full name.");
        }
        if (email.isEmpty() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("Please enter a subject for your inquiry.");
        }
        if (message.length() < 10) {
            throw new IllegalArgumentException("Message must be at least 10 characters long.");
        }

        long ticketId;
        synchronized (lock) {
            ticketId = DataStore.saveContactMessage(name, email, subject, message);
        }

        String json = "{"
                + "\"id\":" + ticketId + ","
                + "\"message\":\"Thank you! Your inquiry (#" + ticketId + ") has been saved by our support desk.\""
                + "}";
        sendJson(exchange, 201, json);
    }

    private int parseTrainNumber(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid input! Please enter a valid train number.");
        }
    }

    private long parsePnr(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PNR! Please enter numbers only.");
        }
    }

    private int extractInt(Object value, String errorMessage) {
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d != Math.floor(d) || d > Integer.MAX_VALUE || d < Integer.MIN_VALUE) {
                throw new IllegalArgumentException(errorMessage);
            }
            return (int) d;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = idx >= 0 ? pair.substring(0, idx) : pair;
            String val = idx >= 0 ? pair.substring(idx + 1) : "";
            params.put(urlDecode(key), urlDecode(val));
        }
        return params;
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String readRequestBody(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
