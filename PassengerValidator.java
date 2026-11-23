/**
 * PassengerValidator.java
 * Contains only validation rules. Every method returns true (valid) or false (invalid).
 * Methods are static so they can be called without creating an object:
 *     PassengerValidator.isValidName("Falak");
 */
public class PassengerValidator {

    // Name: cannot be null, empty, or only spaces
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    // Age: must be a positive integer (1 or more)
    public static boolean isValidAge(int age) {
        return age > 0;
    }

    // Gender: Male, Female or Other (case-insensitive)
    public static boolean isValidGender(String gender) {
        if (gender == null) {
            return false;
        }
        String g = gender.trim();
        return g.equalsIgnoreCase("Male")
                || g.equalsIgnoreCase("Female")
                || g.equalsIgnoreCase("Other");
    }

    // Phone number: exactly 10 digits (0-9 only)
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() != 10) {
            return false;
        }
        for (int i = 0; i < phoneNumber.length(); i++) {
            char c = phoneNumber.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    // Passenger count: must be at least 1
    public static boolean isValidPassengerCount(int count) {
        return count >= 1;
    }
}