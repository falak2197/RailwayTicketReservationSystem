// RailwayService.java
// Purpose: Connects all the managers together. Main.java talks only to this class.

import java.util.ArrayList;
import java.util.Scanner;

public class RailwayService {

    private TrainManager trainManager;
    private BookingManager bookingManager;

    public RailwayService() {
        trainManager = new TrainManager();
        bookingManager = new BookingManager();
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

    // Reads a PNR number safely. Returns null if the input is not a number.
    private Long readPnr(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid PNR! Please enter numbers only.");
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

        try {
            // BookingManager checks seats, calculates fare, makes the PNR and saves
            Booking booking = bookingManager.createBooking(train, passengers);

            System.out.println("\nBOOKING CONFIRMED!\n");
            bookingManager.displayBookingDetails(booking);
        } catch (IllegalArgumentException e) {
            System.out.println("Booking failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 4. Cancel Ticket
    // ------------------------------------------------------------
    public void cancelTicket(Scanner scanner) {
        Long pnr = readPnr(scanner, "Enter PNR Number: ");
        if (pnr == null) {
            return;
        }

        Booking booking = bookingManager.findBooking(pnr);

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
        Long pnr = readPnr(scanner, "Enter PNR: ");
        if (pnr == null) {
            return;
        }

        Booking booking = bookingManager.findBooking(pnr);

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
        bookingManager.displayAllBookings();
    }
}
