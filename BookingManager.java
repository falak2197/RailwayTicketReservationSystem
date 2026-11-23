// BookingManager.java
// Purpose: Stores and manages the list of bookings (in an ArrayList).

import java.util.ArrayList;

public class BookingManager {

    private ArrayList<Booking> bookings = new ArrayList<>();

    // Creates a booking and stores it
    public void createBooking(Booking booking) {
        // TODO: generate a unique PNR number
        // TODO: reduce the train's available seats
        bookings.add(booking);
    }

    // Prints all bookings
    public void displayAllBookings() {
        // TODO: print PNR, train, passengers, fare and status for each booking
        System.out.println("Total bookings: " + bookings.size());
    }

    public ArrayList<Booking> getBookings() {
        return bookings;
    }

    // TODO: findBookingByPnr(String pnrNumber)
    // TODO: cancelBooking(String pnrNumber) and give the seats back to the train
}
