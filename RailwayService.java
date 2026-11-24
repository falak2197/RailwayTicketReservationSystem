// RailwayService.java
// Purpose: Connects all the managers together. Main.java talks only to this class.

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class RailwayService {

    private TrainManager trainManager;
    private BookingManager bookingManager;
    private FareCalculator fareCalculator;

    public RailwayService() {
        trainManager = new TrainManager();
        bookingManager = new BookingManager();
        fareCalculator = new FareCalculator();
    }

    // 1. View All Trains
    public void displayAllTrains() {
        trainManager.displayAllTrains();
    }

    // 2. Search Trains by Source and Destination
    public void searchTrains(Scanner scanner) {
        System.out.print("Enter Source: ");
        String source = scanner.nextLine();
        System.out.print("Enter Destination: ");
        String destination = scanner.nextLine();

        ArrayList<Train> matchingTrains = trainManager.searchTrains(source, destination);

        if (matchingTrains.isEmpty()) {
            System.out.println("No trains found for route: " + source + " -> " + destination);
        } else {
            System.out.println("\nMatching Trains:");
            for (Train train : matchingTrains) {
                System.out.println(train.getTrainNumber() + " | " + train.getTrainName()
                        + " | " + train.getSource() + " -> " + train.getDestination()
                        + " | Seats: " + train.getAvailableSeats() + "/" + train.getTotalSeats()
                        + " | Fare: " + train.getFarePerSeat());
            }
        }
    }

    public void searchTrains() {
        searchTrains(new Scanner(System.in));
    }

    // Reusable helper method to safely read and validate a train number
    private Integer readTrainNumber(Scanner scanner) {
        System.out.print("Enter Train Number: ");
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid train number.");
            return null;
        }
    }

    // Reusable helper method to display complete details of a train
    public void displayTrainDetails(Train train) {
        System.out.println("Train Number   : " + train.getTrainNumber());
        System.out.println("Train Name     : " + train.getTrainName());
        System.out.println("Source         : " + train.getSource());
        System.out.println("Destination    : " + train.getDestination());
        System.out.println("Total Seats    : " + train.getTotalSeats());
        System.out.println("Available Seats: " + train.getAvailableSeats());
        System.out.println("Fare Per Seat  : " + train.getFarePerSeat());
    }

    // Find Train by Train Number (not in the 7-option menu, kept for later use)
    public void findTrainByNumber(Scanner scanner) {
        Integer trainNumber = readTrainNumber(scanner);
        if (trainNumber == null) {
            return;
        }

        Train train = trainManager.findTrainByNumber(trainNumber);
        if (train != null) {
            System.out.println("\nTrain Found:");
            displayTrainDetails(train);
        } else {
            System.out.println("Train not found.");
        }
    }

    public void findTrainByNumber() {
        findTrainByNumber(new Scanner(System.in));
    }

    // Check Seat Availability (not in the 7-option menu, kept for later use)
    public void checkSeatAvailability(Scanner scanner) {
        Integer trainNumber = readTrainNumber(scanner);
        if (trainNumber == null) {
            return;
        }

        Train train = trainManager.findTrainByNumber(trainNumber);
        if (train != null) {
            System.out.println("\nSeat Availability for " + train.getTrainName() + " (" + train.getTrainNumber() + "):");
            System.out.println("Total Seats    : " + train.getTotalSeats());
            System.out.println("Available Seats: " + train.getAvailableSeats());
        } else {
            System.out.println("Train not found.");
        }
    }

    public void checkSeatAvailability() {
        checkSeatAvailability(new Scanner(System.in));
    }

    // View Train Details (not in the 7-option menu, kept for later use)
    public void viewTrainDetails(Scanner scanner) {
        Integer trainNumber = readTrainNumber(scanner);
        if (trainNumber == null) {
            return;
        }

        Train train = trainManager.findTrainByNumber(trainNumber);
        if (train != null) {
            System.out.println("\n--- Train Details ---");
            displayTrainDetails(train);
        } else {
            System.out.println("Train not found.");
        }
    }

    public void viewTrainDetails() {
        viewTrainDetails(new Scanner(System.in));
    }

    // ------------------------------------------------------------
    // 3. Book Ticket
    // ------------------------------------------------------------
    public void bookTicket(Scanner scanner) {
        trainManager.displayAllTrains();

        Integer trainNumber = readTrainNumber(scanner);
        if (trainNumber == null) {
            return;
        }

        Train train = trainManager.findTrainByNumber(trainNumber);
        if (train == null) {
            System.out.println("Train not found.");
            return;
        }

        if (train.getAvailableSeats() == 0) {
            System.out.println("Sorry, this train is full.");
            return;
        }

        // Passenger module asks for the count and all details itself
        PassengerManager passengerManager = new PassengerManager(scanner);
        passengerManager.collectPassengers();
        ArrayList<Passenger> passengers = passengerManager.getPassengers();
        int count = passengers.size();

        // bookSeats() returns false when there are not enough seats
        if (!train.bookSeats(count)) {
            System.out.println("Not enough seats! Only " + train.getAvailableSeats() + " seat(s) available.");
            return;
        }

        double totalFare = fareCalculator.calculateFare(train.getFarePerSeat(), count);
        String pnr = generatePnr();

        Booking booking = new Booking(pnr, train, passengers, totalFare, "CONFIRMED");
        bookingManager.createBooking(booking);

        System.out.println("\nBOOKING CONFIRMED!\n");
        printBooking(booking);
    }

    // ------------------------------------------------------------
    // 4. Cancel Ticket
    // ------------------------------------------------------------
    public void cancelTicket(Scanner scanner) {
        System.out.print("Enter PNR Number: ");
        String pnr = scanner.nextLine().trim();

        Booking booking = findBooking(pnr);

        if (booking == null) {
            System.out.println("Booking not found!");
            return;
        }

        if (booking.getBookingStatus().equals("CANCELLED")) {
            System.out.println("This ticket is already cancelled.");
            return;
        }

        System.out.println("\nBooking Found!\n");
        for (Passenger p : booking.getPassengers()) {
            System.out.println("Passenger: " + p.getName());
        }
        System.out.println("Train: " + booking.getTrain().getTrainName());

        System.out.print("\nConfirm Cancellation? Y/N: ");
        String answer = scanner.nextLine().trim();

        if (answer.equalsIgnoreCase("Y")) {
            booking.setBookingStatus("CANCELLED");
            booking.getTrain().cancelSeats(booking.getPassengers().size());

            System.out.println("\nTicket Cancelled Successfully!");
            System.out.println("Available Seats Updated!");
        } else if (answer.equalsIgnoreCase("N")) {
            System.out.println("Cancellation aborted.");
        } else {
            System.out.println("Invalid answer. Cancellation aborted.");
        }
    }

    // ------------------------------------------------------------
    // 5. Check PNR Status
    // ------------------------------------------------------------
    public void checkPnrStatus(Scanner scanner) {
        System.out.print("Enter PNR: ");
        String pnr = scanner.nextLine().trim();

        Booking booking = findBooking(pnr);

        if (booking == null) {
            System.out.println("Booking not found!");
        } else {
            System.out.println("\nPNR Status: " + booking.getBookingStatus());
        }
    }

    // ------------------------------------------------------------
    // 6. View All Bookings
    // ------------------------------------------------------------
    public void viewAllBookings() {
        ArrayList<Booking> bookings = bookingManager.getBookings();

        if (bookings.isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }

        for (Booking booking : bookings) {
            printBooking(booking);
            System.out.println("--------------------------------");
        }
    }

    // ------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------

    // Searches the booking list for a PNR. Returns null if not found.
    private Booking findBooking(String pnr) {
        for (Booking booking : bookingManager.getBookings()) {
            if (booking.getPnrNumber().equals(pnr)) {
                return booking;
            }
        }
        return null;
    }

    // Makes a random 10-digit PNR that is not already used
    private String generatePnr() {
        Random random = new Random();
        String pnr;
        do {
            long number = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            pnr = String.valueOf(number);
        } while (findBooking(pnr) != null);
        return pnr;
    }

    // Prints one booking
    private void printBooking(Booking booking) {
        System.out.println("PNR: " + booking.getPnrNumber());
        System.out.println("Train: " + booking.getTrain().getTrainName());
        for (Passenger p : booking.getPassengers()) {
            System.out.println("Passenger: " + p.getName());
        }
        System.out.println("Total Fare: Rs. " + booking.getTotalFare());
        System.out.println("Status: " + booking.getBookingStatus());
    }
}
