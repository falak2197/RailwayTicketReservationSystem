// PassengerManager.java
// Purpose: Stores and manages the list of passengers (in an ArrayList).

import java.util.ArrayList;

public class PassengerManager {

    private ArrayList<Passenger> passengers = new ArrayList<>();

    // Adds a passenger to the list
    public void addPassenger(Passenger passenger) {
        // TODO: validate name, age, gender and phone number before adding
        passengers.add(passenger);
    }

    public ArrayList<Passenger> getPassengers() {
        return passengers;
    }

    // TODO: findPassengerById(int passengerId)
    // TODO: displayAllPassengers()
}
