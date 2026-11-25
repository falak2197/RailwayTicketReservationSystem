import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JsonUtil.java
 * Zero-dependency JSON serializer and parser for the Core Java REST API.
 */
public class JsonUtil {

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String trainToJson(Train train) {
        int bookedSeats = train.getTotalSeats() - train.getAvailableSeats();
        return "{"
                + "\"trainNumber\":" + train.getTrainNumber() + ","
                + "\"trainName\":\"" + escape(train.getTrainName()) + "\","
                + "\"source\":\"" + escape(train.getSource()) + "\","
                + "\"destination\":\"" + escape(train.getDestination()) + "\","
                + "\"totalSeats\":" + train.getTotalSeats() + ","
                + "\"availableSeats\":" + train.getAvailableSeats() + ","
                + "\"bookedSeats\":" + bookedSeats + ","
                + "\"farePerSeat\":" + train.getFarePerSeat()
                + "}";
    }

    public static String trainsToJson(List<Train> trains) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trains.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(trainToJson(trains.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String passengerToJson(Passenger passenger) {
        return "{"
                + "\"passengerId\":" + passenger.getPassengerId() + ","
                + "\"name\":\"" + escape(passenger.getName()) + "\","
                + "\"age\":" + passenger.getAge() + ","
                + "\"gender\":\"" + escape(passenger.getGender()) + "\","
                + "\"phoneNumber\":\"" + escape(passenger.getPhoneNumber()) + "\""
                + "}";
    }

    public static String passengersToJson(List<Passenger> passengers) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < passengers.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(passengerToJson(passengers.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String bookingToJson(Booking booking) {
        return "{"
                + "\"pnrNumber\":" + booking.getPnrNumber() + ","
                + "\"train\":" + trainToJson(booking.getTrain()) + ","
                + "\"passengers\":" + passengersToJson(booking.getPassengers()) + ","
                + "\"passengerCount\":" + booking.getPassengers().size() + ","
                + "\"totalFare\":" + booking.getTotalFare() + ","
                + "\"bookingStatus\":\"" + escape(booking.getBookingStatus()) + "\""
                + "}";
    }

    public static String bookingsToJson(List<Booking> bookings) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(bookingToJson(bookings.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String errorToJson(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body cannot be empty.");
        }
        Object parsed = new Parser(json).parse();
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Expected a JSON object.");
        }
        return (Map<String, Object>) parsed;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Object parsed = new Parser(json).parse();
        if (!(parsed instanceof List)) {
            throw new IllegalArgumentException("Expected a JSON array.");
        }
        return (List<Object>) parsed;
    }

    // Minimal recursive-descent JSON parser for standard JSON payloads
    private static final class Parser {
        private final String input;
        private int pos = 0;

        Parser(String input) {
            this.input = input;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (pos < input.length()) {
                throw new IllegalArgumentException("Unexpected trailing characters in JSON.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON.");
            }
            char c = input.charAt(pos);
            if (c == '{') {
                return parseObjectValue();
            }
            if (c == '[') {
                return parseArrayValue();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't' || c == 'f') {
                return parseBoolean();
            }
            if (c == 'n') {
                return parseNull();
            }
            if (c == '-' || (c >= '0' && c <= '9')) {
                return parseNumber();
            }
            throw new IllegalArgumentException("Invalid JSON token at position " + pos + ".");
        }

        private Map<String, Object> parseObjectValue() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw new IllegalArgumentException("Expected string key at position " + pos + ".");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek('}')) {
                    pos++;
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or '}' at position " + pos + ".");
                }
            }
            return map;
        }

        private List<Object> parseArrayValue() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek(']')) {
                    pos++;
                    break;
                } else {
                    throw new IllegalArgumentException("Expected ',' or ']' at position " + pos + ".");
                }
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= input.length()) {
                        throw new IllegalArgumentException("Unterminated escape sequence.");
                    }
                    char esc = input.charAt(pos++);
                    switch (esc) {
                        case '"':  sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > input.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape.");
                            }
                            String hex = input.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape '\\" + esc + "'.");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Boolean parseBoolean() {
            if (input.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at position " + pos + ".");
        }

        private Object parseNull() {
            if (input.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Invalid null at position " + pos + ".");
        }

        private Number parseNumber() {
            int start = pos;
            if (input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            boolean isFloat = false;
            if (pos < input.length() && input.charAt(pos) == '.') {
                isFloat = true;
                pos++;
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
            }
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                isFloat = true;
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    pos++;
                }
            }
            String numStr = input.substring(start, pos);
            try {
                return isFloat ? Double.parseDouble(numStr) : Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number '" + numStr + "'.");
            }
        }

        private void skipWhitespace() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private boolean peek(char expected) {
            return pos < input.length() && input.charAt(pos) == expected;
        }

        private void expect(char expected) {
            if (pos >= input.length() || input.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos + ".");
            }
            pos++;
        }
    }
}
