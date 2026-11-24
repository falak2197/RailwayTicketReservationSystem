// FareCalculator.java
// Purpose: Calculates the total fare for a booking.

public class FareCalculator {

    // Total Fare = Fare per Seat x Number of Passengers
    public double calculateFare(double farePerSeat, int numberOfPassengers) {
        if (farePerSeat < 0) {
            throw new IllegalArgumentException("Fare cannot be negative.");
        }
        if (numberOfPassengers <= 0) {
            throw new IllegalArgumentException("Passenger count must be at least 1.");
        }
        return farePerSeat * numberOfPassengers;
    }
}
