// BookingManager.java
// Purpose: Creates and stores bookings (in an ArrayList).
// It checks seats, calculates the fare, generates the PNR and saves the booking.

import java.util.ArrayList;
import java.util.Random;

public class BookingManager {

    private ArrayList<Booking> bookings = new ArrayList<>();
    private FareCalculator fareCalculator = new FareCalculator();

    // Makes a random 10-digit PNR that no other booking is using
    public long generatePNR() {
        Random random = new Random();
        long pnr;
        do {
            pnr = 1000000000L + (long) (random.nextDouble() * 9000000000L);
        } while (findBooking(pnr) != null);
        return pnr;
    }

    // Fare per seat x number of passengers
    public double calculateTotalFare(Train train, int passengerCount) {
        return fareCalculator.calculateFare(train.getFarePerSeat(), passengerCount);
    }

    // Creates a booking and stores it.
    // Throws IllegalArgumentException if the booking is not possible.
    public Booking createBooking(Train train, ArrayList<Passenger> passengers) {
        if (train == null) {
            throw new IllegalArgumentException("Train not selected.");
        }
        if (passengers == null || passengers.isEmpty()) {
            throw new IllegalArgumentException("At least one passenger is required.");
        }

        int count = passengers.size();

        // Check seat availability before booking
        if (count > train.getAvailableSeats()) {
            throw new IllegalArgumentException("Not enough seats! Only "
                    + train.getAvailableSeats() + " seat(s) available.");
        }

        double totalFare = calculateTotalFare(train, count);

        // Reduce available seats
        train.bookSeats(count);

        long pnr = generatePNR();
        Booking booking = new Booking(pnr, train, passengers, totalFare, "CONFIRMED");
        bookings.add(booking);
        return booking;
    }

    // Finds a booking by PNR. Returns null if not found.
    public Booking findBooking(long pnr) {
        for (Booking booking : bookings) {
            if (booking.getPnrNumber() == pnr) {
                return booking;
            }
        }
        return null;
    }

    // Prints one booking
    public void displayBookingDetails(Booking booking) {
        System.out.println("PNR: " + booking.getPnrNumber());
        System.out.println("Train: " + booking.getTrain().getTrainName());
        for (Passenger p : booking.getPassengers()) {
            System.out.println("Passenger: " + p.getName());
        }
        System.out.println("Total Fare: Rs. " + booking.getTotalFare());
        System.out.println("Status: " + booking.getBookingStatus());
    }

    // Prints all bookings
    public void displayAllBookings() {
        if (bookings.isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        for (Booking booking : bookings) {
            displayBookingDetails(booking);
            System.out.println("--------------------------------");
        }
    }

    public ArrayList<Booking> getBookings() {
        return bookings;
    }
}
