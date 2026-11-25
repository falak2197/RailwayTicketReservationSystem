import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DataStore.java
 * Beginner-friendly file-based persistence using human-readable JSON files in data/.
 * Saves and loads:
 *   - data/railway_data.json     (train seat counts & booking records)
 *   - data/contact_messages.json (submitted contact form inquiries)
 */
public class DataStore {

    private static final String RAILWAY_DATA_FILE = "railway_data.json";
    private static final String CONTACT_DATA_FILE = "contact_messages.json";

    private static Path resolveDataDir() {
        // Keep data/ at the project root even if launched from backend/
        if (!Files.isDirectory(Paths.get("frontend")) && Files.isDirectory(Paths.get("..", "frontend"))) {
            return Paths.get("..", "data").toAbsolutePath().normalize();
        }
        return Paths.get("data").toAbsolutePath().normalize();
    }

    /**
     * Loads saved train seat counts and booking records from data/railway_data.json.
     * Safely ignores missing or corrupted files so the server still starts cleanly.
     */
    @SuppressWarnings("unchecked")
    public static synchronized void loadState(RailwayService service) {
        Path dataFile = resolveDataDir().resolve(RAILWAY_DATA_FILE);
        if (!Files.exists(dataFile) || !Files.isRegularFile(dataFile)) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(dataFile), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                return;
            }

            Map<String, Object> root = JsonUtil.parseObject(content);

            // 1. Restore availableSeats onto existing sample trains without overwriting train definitions
            Object rawTrains = root.get("trains");
            if (rawTrains instanceof List) {
                for (Object item : (List<Object>) rawTrains) {
                    if (!(item instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> tMap = (Map<String, Object>) item;
                    Integer trainNumber = asInt(tMap.get("trainNumber"));
                    Integer availableSeats = asInt(tMap.get("availableSeats"));
                    if (trainNumber == null || availableSeats == null) {
                        continue;
                    }
                    Train train = service.findTrainByNumber(trainNumber);
                    if (train != null && availableSeats >= 0 && availableSeats <= train.getTotalSeats()) {
                        train.setAvailableSeats(availableSeats);
                    }
                }
            }

            // 2. Restore bookings while avoiding duplicate PNRs
            Object rawBookings = root.get("bookings");
            Set<Long> seenPnrs = new HashSet<>();
            for (Booking existing : service.getAllBookings()) {
                seenPnrs.add(existing.getPnrNumber());
            }

            int maxPassengerId = 100;

            if (rawBookings instanceof List) {
                for (Object item : (List<Object>) rawBookings) {
                    if (!(item instanceof Map)) {
                        continue;
                    }
                    Map<String, Object> bMap = (Map<String, Object>) item;
                    Long pnr = asLong(bMap.get("pnrNumber"));
                    Integer trainNumber = asInt(bMap.get("trainNumber"));
                    Double totalFare = asDouble(bMap.get("totalFare"));
                    String status = bMap.get("bookingStatus") == null ? null : String.valueOf(bMap.get("bookingStatus"));
                    Object rawPassengers = bMap.get("passengers");

                    if (pnr == null || trainNumber == null || totalFare == null || status == null
                            || !(rawPassengers instanceof List)) {
                        continue;
                    }
                    if (!"CONFIRMED".equals(status) && !"CANCELLED".equals(status)) {
                        continue;
                    }
                    if (seenPnrs.contains(pnr) || service.findBookingByPnr(pnr) != null) {
                        continue;
                    }

                    Train train = service.findTrainByNumber(trainNumber);
                    if (train == null) {
                        continue;
                    }

                    ArrayList<Passenger> passengers = new ArrayList<>();
                    boolean validPassengers = true;
                    for (Object pObj : (List<Object>) rawPassengers) {
                        if (!(pObj instanceof Map)) {
                            validPassengers = false;
                            break;
                        }
                        Map<String, Object> pMap = (Map<String, Object>) pObj;
                        Integer pid = asInt(pMap.get("passengerId"));
                        String name = pMap.get("name") == null ? null : String.valueOf(pMap.get("name"));
                        Integer age = asInt(pMap.get("age"));
                        String gender = pMap.get("gender") == null ? null : String.valueOf(pMap.get("gender"));
                        String phone = pMap.get("phoneNumber") == null ? null : String.valueOf(pMap.get("phoneNumber"));

                        if (pid == null || age == null
                                || !PassengerValidator.isValidName(name)
                                || !PassengerValidator.isValidAge(age)
                                || !PassengerValidator.isValidGender(gender)
                                || !PassengerValidator.isValidPhoneNumber(phone)) {
                            validPassengers = false;
                            break;
                        }

                        if (pid > maxPassengerId) {
                            maxPassengerId = pid;
                        }
                        passengers.add(new Passenger(pid, name.trim(), age, gender.trim(), phone.trim()));
                    }

                    if (!validPassengers || passengers.isEmpty()) {
                        continue;
                    }

                    Booking restored = new Booking(pnr, train, passengers, totalFare, status);
                    service.getAllBookings().add(restored);
                    seenPnrs.add(pnr);
                }
            }

            service.getWebPassengerManager().ensureNextPassengerId(maxPassengerId + 1);
            System.out.println("  Loaded persistence data: " + service.getAllBookings().size() + " booking(s) restored.");
        } catch (Exception e) {
            System.err.println("  Warning: Could not read " + dataFile + " (" + e.getMessage() + "). Starting with clean default state.");
        }
    }

    /**
     * Saves current train seat availability and all bookings to data/railway_data.json.
     */
    public static synchronized void saveState(RailwayService service) {
        try {
            Path dataDir = resolveDataDir();
            Files.createDirectories(dataDir);

            StringBuilder sb = new StringBuilder();
            sb.append("{\n");

            // Trains seat snapshot
            sb.append("  \"trains\": [\n");
            List<Train> trains = service.getAllTrains();
            for (int i = 0; i < trains.size(); i++) {
                Train t = trains.get(i);
                sb.append("    {\"trainNumber\": ").append(t.getTrainNumber())
                  .append(", \"availableSeats\": ").append(t.getAvailableSeats()).append("}");
                if (i < trains.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ],\n");

            // Bookings list
            sb.append("  \"bookings\": [\n");
            List<Booking> bookings = service.getAllBookings();
            for (int i = 0; i < bookings.size(); i++) {
                Booking b = bookings.get(i);
                sb.append("    {\n");
                sb.append("      \"pnrNumber\": ").append(b.getPnrNumber()).append(",\n");
                sb.append("      \"trainNumber\": ").append(b.getTrain().getTrainNumber()).append(",\n");
                sb.append("      \"totalFare\": ").append(b.getTotalFare()).append(",\n");
                sb.append("      \"bookingStatus\": \"").append(JsonUtil.escape(b.getBookingStatus())).append("\",\n");
                sb.append("      \"passengers\": ").append(JsonUtil.passengersToJson(b.getPassengers())).append("\n");
                sb.append("    }");
                if (i < bookings.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}\n");

            writeAtomically(dataDir.resolve(RAILWAY_DATA_FILE), sb.toString());
        } catch (IOException e) {
            System.err.println("Error saving railway data: " + e.getMessage());
        }
    }

    /**
     * Persists a validated contact form inquiry to data/contact_messages.json.
     *
     * @return the newly assigned inquiry ID
     */
    @SuppressWarnings("unchecked")
    public static synchronized long saveContactMessage(String name, String email, String subject, String message)
            throws IOException {
        Path dataDir = resolveDataDir();
        Files.createDirectories(dataDir);
        Path contactFile = dataDir.resolve(CONTACT_DATA_FILE);

        List<Map<String, Object>> existingMessages = new ArrayList<>();
        long nextId = 1;

        if (Files.exists(contactFile) && Files.isRegularFile(contactFile)) {
            try {
                String raw = new String(Files.readAllBytes(contactFile), StandardCharsets.UTF_8);
                List<Object> parsed = JsonUtil.parseArray(raw);
                for (Object item : parsed) {
                    if (item instanceof Map) {
                        Map<String, Object> msgMap = (Map<String, Object>) item;
                        existingMessages.add(msgMap);
                        Long id = asLong(msgMap.get("id"));
                        if (id != null && id >= nextId) {
                            nextId = id + 1;
                        }
                    }
                }
            } catch (Exception ignored) {
                // If file was corrupted, preserve safety by starting a clean list
                existingMessages.clear();
                nextId = 1;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < existingMessages.size(); i++) {
            Map<String, Object> m = existingMessages.get(i);
            sb.append("  {")
              .append("\"id\": ").append(asLong(m.get("id")) != null ? asLong(m.get("id")) : (i + 1)).append(", ")
              .append("\"name\": \"").append(JsonUtil.escape(String.valueOf(m.get("name")))).append("\", ")
              .append("\"email\": \"").append(JsonUtil.escape(String.valueOf(m.get("email")))).append("\", ")
              .append("\"subject\": \"").append(JsonUtil.escape(String.valueOf(m.get("subject")))).append("\", ")
              .append("\"message\": \"").append(JsonUtil.escape(String.valueOf(m.get("message")))).append("\", ")
              .append("\"submittedAt\": \"").append(JsonUtil.escape(String.valueOf(m.get("submittedAt")))).append("\"")
              .append("},\n");
        }

        String submittedAt = Instant.now().toString();
        sb.append("  {")
          .append("\"id\": ").append(nextId).append(", ")
          .append("\"name\": \"").append(JsonUtil.escape(name)).append("\", ")
          .append("\"email\": \"").append(JsonUtil.escape(email)).append("\", ")
          .append("\"subject\": \"").append(JsonUtil.escape(subject)).append("\", ")
          .append("\"message\": \"").append(JsonUtil.escape(message)).append("\", ")
          .append("\"submittedAt\": \"").append(JsonUtil.escape(submittedAt)).append("\"")
          .append("}\n");
        sb.append("]\n");

        writeAtomically(contactFile, sb.toString());
        return nextId;
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path tempFile = target.resolveSibling(target.getFileName().toString() + ".tmp");
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Integer asInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return null;
    }

    private static Long asLong(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    private static Double asDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }
}
