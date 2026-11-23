// Booking.java
// Purpose: Represents one ticket booking (PNR, train, passengers, fare, status).

import java.util.ArrayList;

public class Booking {

    private String pnrNumber;
    private Train train;
    private ArrayList<Passenger> passengers;
    private double totalFare;
    private String bookingStatus; // e.g. "CONFIRMED" or "CANCELLED"

    public Booking(String pnrNumber, Train train, ArrayList<Passenger> passengers,
                   double totalFare, String bookingStatus) {
        this.pnrNumber = pnrNumber;
        this.train = train;
        this.passengers = passengers;
        this.totalFare = totalFare;
        this.bookingStatus = bookingStatus;
    }

    public String getPnrNumber() { return pnrNumber; }
    public Train getTrain() { return train; }
    public ArrayList<Passenger> getPassengers() { return passengers; }
    public double getTotalFare() { return totalFare; }
    public String getBookingStatus() { return bookingStatus; }

    // TODO: add setBookingStatus() when cancellation is implemented
}
