// RailwayService.java
// Purpose: Connects all the managers together. Main.java talks only to this class.

import java.util.ArrayList;
import java.util.Scanner;

public class RailwayService {

    private TrainManager trainManager;
    private PassengerManager passengerManager;
    private BookingManager bookingManager;
    private FareCalculator fareCalculator;

    public RailwayService() {
        trainManager = new TrainManager();
        passengerManager = new PassengerManager();
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

    // 3. Find Train by Train Number
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

    // 4. Check Seat Availability
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

    // 5. View Train Details
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

    // Existing methods for features 6-9 (preserved)
    public void bookTicket() {
        // TODO: pick a train, collect passenger details, calculate fare, create booking
        System.out.println("Feature under development");
    }

    public void cancelTicket() {
        // TODO: ask for PNR, cancel the booking, restore available seats
        System.out.println("Feature under development");
    }

    public void checkPnrStatus() {
        // TODO: ask for PNR and show booking details
        System.out.println("Feature under development");
    }

    public void viewAllBookings() {
        bookingManager.displayAllBookings();
    }
}
