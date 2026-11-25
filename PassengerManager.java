import java.util.ArrayList;
import java.util.Scanner;

/**
 * PassengerManager.java
 * Collects, validates, stores, displays and searches passengers for ONE booking.
 * Member 3 (Booking) can call getPassengers() to receive the passenger list.
 */
public class PassengerManager {

    // Passenger IDs start at 101 and increase by 1 for each new passenger
    private static final int FIRST_PASSENGER_ID = 101;

    private ArrayList<Passenger> passengers;   // passengers of the current booking
    private int nextPassengerId;               // next ID to hand out
    private Scanner scanner;                   // used for keyboard input

    // Default constructor: creates its own Scanner
    public PassengerManager() {
        this(new Scanner(System.in));
    }

    // Constructor that accepts a shared Scanner (recommended when other classes
    // also read from the keyboard, so only one Scanner uses System.in)
    public PassengerManager(Scanner scanner) {
        this.passengers = new ArrayList<>();
        this.nextPassengerId = FIRST_PASSENGER_ID;
        this.scanner = scanner;
    }

    // ---------------------------------------------------------------
    // Main flow: ask count, then collect every passenger's details
    // ---------------------------------------------------------------
    public void collectPassengers() {
        // Start a fresh list for this booking (IDs keep increasing, so they stay unique)
        passengers.clear();

        System.out.println("========================================");
        System.out.println("       PASSENGER MANAGEMENT");
        System.out.println("========================================");
        System.out.println();

        int count = readPassengerCount();

        for (int i = 1; i <= count; i++) {
            System.out.println();
            System.out.println("Passenger " + i + " Details:");
            System.out.println();

            String name = readName();
            int age = readAge();
            String gender = readGender();
            String phone = readPhoneNumber();

            // All inputs are valid at this point, so create the object
            Passenger passenger = new Passenger(nextPassengerId, name, age, gender, phone);
            nextPassengerId++;               // generate the next unique ID
            passengers.add(passenger);
            System.out.println();
        }

        System.out.println("Passenger details added successfully!");
    }

    // ---------------------------------------------------------------
    // Input helpers: each one loops until the user enters valid data
    // ---------------------------------------------------------------
    private int readPassengerCount() {
        while (true) {
            System.out.print("Enter number of passengers: ");
            String input = scanner.nextLine().trim();
            try {
                int count = Integer.parseInt(input);
                if (PassengerValidator.isValidPassengerCount(count)) {
                    return count;
                }
                System.out.println("Invalid count! At least 1 passenger is required.");
            } catch (NumberFormatException e) {
                // Thrown when the user types letters or leaves it blank
                System.out.println("Invalid input! Please enter a whole number for passenger count.");
            }
        }
    }

    private String readName() {
        while (true) {
            System.out.print("Enter Name: ");
            String name = scanner.nextLine();
            if (PassengerValidator.isValidName(name)) {
                return name.trim();
            }
            System.out.println("Invalid name! Please enter a valid name.");
        }
    }

    private int readAge() {
        while (true) {
            System.out.print("Enter Age: ");
            String input = scanner.nextLine().trim();
            try {
                int age = Integer.parseInt(input);
                if (PassengerValidator.isValidAge(age)) {
                    return age;
                }
                System.out.println("Invalid age! Age must be a positive number.");
            } catch (NumberFormatException e) {
                // Letters, decimals or blank input end up here
                System.out.println("Invalid age! Age must be a positive number.");
            }
        }
    }

    private String readGender() {
        while (true) {
            System.out.print("Enter Gender: ");
            String gender = scanner.nextLine();
            if (PassengerValidator.isValidGender(gender)) {
                return formatGender(gender);
            }
            System.out.println("Invalid gender! Please enter Male, Female or Other.");
        }
    }

    private String readPhoneNumber() {
        while (true) {
            System.out.print("Enter Phone Number: ");
            String phone = scanner.nextLine().trim();
            if (PassengerValidator.isValidPhoneNumber(phone)) {
                return phone;
            }
            System.out.println("Invalid phone number! Please enter a valid 10-digit number.");
        }
    }

    // Stores gender in a standard form: "female" / "FEMALE" -> "Female"
    private String formatGender(String gender) {
        String g = gender.trim().toLowerCase();
        return g.substring(0, 1).toUpperCase() + g.substring(1);
    }

    // ---------------------------------------------------------------
    // Display, search and integration methods
    // ---------------------------------------------------------------
    public void displayAllPassengers() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       PASSENGER RECORDS");
        System.out.println("========================================");

        if (passengers.isEmpty()) {
            System.out.println();
            System.out.println("No passenger records found.");
            return;
        }

        for (int i = 0; i < passengers.size(); i++) {
            System.out.println();
            passengers.get(i).displayPassengerDetails();
            if (i < passengers.size() - 1) {
                System.out.println();
                System.out.println("----------------------------------------");
            }
        }
    }

    // Returns the matching Passenger, or null if the ID is not found
    public Passenger searchPassengerById(int passengerId) {
        for (Passenger p : passengers) {
            if (p.getPassengerId() == passengerId) {
                return p;
            }
        }
        return null;
    }

    // For Member 3: returns a COPY of the list so the internal list cannot be
    // modified from outside (adding/removing in the copy does not affect this class).
    public ArrayList<Passenger> getPassengers() {
        return new ArrayList<>(passengers);
    }

    // Number of passengers in the current booking (handy for fare calculation)
    public int getPassengerCount() {
        return passengers.size();
    }

    // ---------------------------------------------------------------
    // Programmatic integration helpers (used by Web API via RailwayService)
    // ---------------------------------------------------------------

    /**
     * Clears the internal passenger list before collecting a new web booking.
     */
    public void clearPassengers() {
        passengers.clear();
    }

    /**
     * Advances nextPassengerId if persisted bookings already used higher IDs,
     * avoiding duplicate Passenger IDs after a server restart.
     */
    public void ensureNextPassengerId(int minNextId) {
        if (minNextId > this.nextPassengerId) {
            this.nextPassengerId = minNextId;
        }
    }

    /**
     * Validates and adds one passenger without console Scanner prompts,
     * reusing PassengerValidator rules and the shared passengerId sequence.
     */
    public Passenger addPassenger(String name, int age, String gender, String phoneNumber) {
        if (!PassengerValidator.isValidName(name)) {
            throw new IllegalArgumentException("Invalid name! Please enter a valid name.");
        }
        if (!PassengerValidator.isValidAge(age)) {
            throw new IllegalArgumentException("Invalid age! Age must be a positive number.");
        }
        if (!PassengerValidator.isValidGender(gender)) {
            throw new IllegalArgumentException("Invalid gender! Please enter Male, Female or Other.");
        }
        String trimmedPhone = phoneNumber == null ? "" : phoneNumber.trim();
        if (!PassengerValidator.isValidPhoneNumber(trimmedPhone)) {
            throw new IllegalArgumentException("Invalid phone number! Please enter a valid 10-digit number.");
        }

        Passenger passenger = new Passenger(
                nextPassengerId,
                name.trim(),
                age,
                formatGender(gender),
                trimmedPhone
        );
        nextPassengerId++;
        passengers.add(passenger);
        return passenger;
    }

    // ---------------------------------------------------------------
    // Stand-alone demo so Member 2 can test the module on its own
    // ---------------------------------------------------------------
    public static void main(String[] args) {
        PassengerManager manager = new PassengerManager();

        manager.collectPassengers();
        manager.displayAllPassengers();

        // Optional search demo: keep searching until the user enters 0
        Scanner input = manager.scanner;
        while (true) {
            System.out.println();
            System.out.print("Enter Passenger ID to search (0 to exit): ");
            String line = input.nextLine().trim();
            int id;
            try {
                id = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid ID! Please enter a number.");
                continue;
            }
            if (id == 0) {
                break;
            }
            Passenger found = manager.searchPassengerById(id);
            System.out.println();
            if (found == null) {
                System.out.println("No passenger found with ID " + id + ".");
            } else {
                System.out.println("Passenger found:");
                found.displayPassengerDetails();
            }
        }
        System.out.println("Exiting Passenger Management.");
    }
}