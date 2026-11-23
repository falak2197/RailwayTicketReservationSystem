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
        for (Train train : trains) {
            System.out.println(train.getTrainNumber() + " | " + train.getTrainName()
                    + " | " + train.getSource() + " -> " + train.getDestination()
                    + " | Seats: " + train.getAvailableSeats() + "/" + train.getTotalSeats()
                    + " | Fare: " + train.getFarePerSeat());
        }
    }

    public ArrayList<Train> getTrains() {
        return trains;
    }

    // TODO: searchTrains(String source, String destination)
    // TODO: findTrainByNumber(int trainNumber)
    // TODO: addTrain() / removeTrain() (optional)
}
