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
            System.out.println("1. View All Trains");
            System.out.println("2. Search Trains by Source and Destination");
            System.out.println("3. Find Train by Number");
            System.out.println("4. Check Seat Availability");
            System.out.println("5. View Train Details");
            System.out.println("6. Book Ticket");
            System.out.println("7. Cancel Ticket");
            System.out.println("8. Check PNR Status");
            System.out.println("9. View All Bookings");
            System.out.println("10. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); // clear the newline character left behind by nextInt()

                switch (choice) {
                    case 1: service.displayAllTrains(); break;
                    case 2: service.searchTrains(scanner); break;
                    case 3: service.findTrainByNumber(scanner); break;
                    case 4: service.checkSeatAvailability(scanner); break;
                    case 5: service.viewTrainDetails(scanner); break;
                    case 6: service.bookTicket(scanner); break;
                    case 7: service.cancelTicket(scanner); break;
                    case 8: service.checkPnrStatus(scanner); break;
                    case 9: service.viewAllBookings(); break;
                    case 10:
                        System.out.println("Thank you for using the system. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter 1-10.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Please enter a number.");
                scanner.nextLine(); // clear the invalid input
            }
        }
        scanner.close();
    }
}
