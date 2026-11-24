// Main.java
// Purpose: Entry point. Shows a console menu and calls RailwayService.

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        RailwayService service = new RailwayService();
        int choice = 0;

        do {
            System.out.println("\n=================================");
            System.out.println("     RAILWAY RESERVATION SYSTEM");
            System.out.println("=================================\n");
            System.out.println("1. Display All Trains");
            System.out.println("2. Search Trains");
            System.out.println("3. Book Ticket");
            System.out.println("4. Cancel Ticket");
            System.out.println("5. Check PNR Status");
            System.out.println("6. View All Bookings");
            System.out.println("7. Exit\n");
            System.out.print("Enter Choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        service.displayAllTrains();
                        break;
                    case 2:
                        service.searchTrains(scanner);
                        break;
                    case 3:
                        service.bookTicket(scanner);
                        break;
                    case 4:
                        service.cancelTicket(scanner);
                        break;
                    case 5:
                        service.checkPnrStatus(scanner);
                        break;
                    case 6:
                        service.viewAllBookings();
                        break;
                    case 7:
                        System.out.println("Thank you for using the system. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter 1-7.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number.");
                choice = 0;
            }

        } while (choice != 7);

        scanner.close();
    }
}
