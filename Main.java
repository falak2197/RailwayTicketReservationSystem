// Main.java
// Purpose: Entry point. Shows a console menu and calls RailwayService.

import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RailwayService service = new RailwayService();
        boolean running = true;

        while (running) {
            System.out.println("\n===== Railway Ticket Reservation System =====");
            System.out.println("1. Display All Trains");
            System.out.println("2. Search Trains");
            System.out.println("3. Book Ticket");
            System.out.println("4. Cancel Ticket");
            System.out.println("5. Check PNR Status");
            System.out.println("6. View All Bookings");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1: service.displayAllTrains(); break;
                    case 2: service.searchTrains(); break;
                    case 3: service.bookTicket(); break;
                    case 4: service.cancelTicket(); break;
                    case 5: service.checkPnrStatus(); break;
                    case 6: service.viewAllBookings(); break;
                    case 7:
                        System.out.println("Thank you for using the system. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter 1-7.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter a number.");
                scanner.nextLine(); // clear the invalid input
            }
        }
        scanner.close();
    }
}
