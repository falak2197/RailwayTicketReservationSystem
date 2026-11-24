// TrainManager.java
// Purpose: Stores and manages the list of trains (in an ArrayList).

import java.util.ArrayList;

public class TrainManager {

    private ArrayList<Train> trains = new ArrayList<>();

    // Adds a few sample trains so there is something to display
    public TrainManager() {
        trains.add(new Train(12001, "Shatabdi Express", "Delhi", "Chandigarh", 100, 100, 750.0));
        trains.add(new Train(12002, "Rajdhani Express", "Delhi", "Mumbai", 200, 200, 2200.0));
        trains.add(new Train(12003, "Duronto Express", "Mumbai", "Kolkata", 150, 150, 1800.0));
    }

    // Prints all trains in the list
    public void displayAllTrains() {
        if (trains.isEmpty()) {
            System.out.println("No trains available.");
            return;
        }
        for (Train train : trains) {
            System.out.println(train.getTrainNumber() + " | " + train.getTrainName()
                    + " | " + train.getSource() + " -> " + train.getDestination()
                    + " | Seats: " + train.getAvailableSeats() + "/" + train.getTotalSeats()
                    + " | Fare: " + train.getFarePerSeat());
        }
    }

    // Searches for trains matching the source and destination (case-insensitive and trimmed)
    public ArrayList<Train> searchTrains(String source, String destination) {
        ArrayList<Train> matchingTrains = new ArrayList<>();

        // Handle null and blank inputs safely
        if (source == null || destination == null) {
            return matchingTrains;
        }

        String trimmedSource = source.trim();
        String trimmedDestination = destination.trim();

        if (trimmedSource.isEmpty() || trimmedDestination.isEmpty()) {
            return matchingTrains;
        }

        for (Train train : trains) {
            if (train.getSource() != null && train.getDestination() != null) {
                if (train.getSource().trim().equalsIgnoreCase(trimmedSource)
                        && train.getDestination().trim().equalsIgnoreCase(trimmedDestination)) {
                    matchingTrains.add(train);
                }
            }
        }
        return matchingTrains;
    }

    // Finds and returns a train by its train number; returns null if not found
    public Train findTrainByNumber(int trainNumber) {
        for (Train train : trains) {
            if (train.getTrainNumber() == trainNumber) {
                return train;
            }
        }
        return null;
    }

    // Adds a new train to the list after validating all fields and checking for duplicates
    public boolean addTrain(Train train) {
        // Reject null train object
        if (train == null) {
            return false;
        }

        // Validate train number (must be positive)
        if (train.getTrainNumber() <= 0) {
            return false;
        }

        // Prevent duplicate train numbers
        if (findTrainByNumber(train.getTrainNumber()) != null) {
            return false;
        }

        // Validate train name (cannot be null or blank)
        if (train.getTrainName() == null || train.getTrainName().trim().isEmpty()) {
            return false;
        }

        // Validate source (cannot be null or blank)
        if (train.getSource() == null || train.getSource().trim().isEmpty()) {
            return false;
        }

        // Validate destination (cannot be null or blank)
        if (train.getDestination() == null || train.getDestination().trim().isEmpty()) {
            return false;
        }

        // Validate total seats (must be greater than 0)
        if (train.getTotalSeats() <= 0) {
            return false;
        }

        // Validate available seats (must be non-negative and not exceed total seats)
        if (train.getAvailableSeats() < 0 || train.getAvailableSeats() > train.getTotalSeats()) {
            return false;
        }

        // Validate fare per seat (must be greater than 0)
        if (train.getFarePerSeat() <= 0) {
            return false;
        }

        // All validations passed; add train to the list
        trains.add(train);
        return true;
    }

    // Returns the underlying train list for compatibility with other modules
    public ArrayList<Train> getTrains() {
        return trains;
    }
}
