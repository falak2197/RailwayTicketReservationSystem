// Booking.java
// Purpose: Represents one ticket booking (PNR, train, passengers, fare, status).

import java.util.ArrayList;

public class Booking {

    private long pnrNumber;
    private Train train;
    private ArrayList<Passenger> passengers;
    private double totalFare;
    private String bookingStatus; // "CONFIRMED" or "CANCELLED"

    public Booking(long pnrNumber, Train train, ArrayList<Passenger> passengers,
                   double totalFare, String bookingStatus) {
        this.pnrNumber = pnrNumber;
        this.train = train;
        this.passengers = passengers;
        this.totalFare = totalFare;
        this.bookingStatus = bookingStatus;
    }

    public long getPnrNumber() { return pnrNumber; }
    public Train getTrain() { return train; }
    public ArrayList<Passenger> getPassengers() { return passengers; }
    public double getTotalFare() { return totalFare; }
    public String getBookingStatus() { return bookingStatus; }

    // Used when a ticket is cancelled
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
}
