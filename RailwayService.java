// RailwayService.java
// Purpose: Connects all the managers together. Main.java talks only to this class.

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

    public void displayAllTrains() {
        trainManager.displayAllTrains();
    }

    public void searchTrains() {
        // TODO: ask for source and destination, then search using TrainManager
        System.out.println("Feature under development");
    }

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
