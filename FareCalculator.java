// FareCalculator.java
// Purpose: Calculates the total fare for a booking.

public class FareCalculator {

    // Basic fare: fare per seat x number of passengers
    public double calculateFare(double farePerSeat, int numberOfPassengers) {
        // TODO: add discounts (senior citizens, children) if required
        return farePerSeat * numberOfPassengers;
    }
}
