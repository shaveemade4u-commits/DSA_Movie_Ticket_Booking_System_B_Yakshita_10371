

import java.util.*;
import java.io.*;

public class MovieTicketBooking {

    // 1. Cinema configuration
    static final int ROWS = 5;
    static final int COLS = 5;
    static final String FILE_NAME = "tickets.txt";

    // 2D array to represent cinema seats
    static Booking[][] seats = new Booking[ROWS][COLS];

    // HashMap to search booking using ID
    static HashMap<String, Booking> bookingMap = new HashMap<>();

    // PriorityQueue: Gold first, then Regular
    // Same category follows FIFO
    static PriorityQueue<Booking> waitingQueue =
        new PriorityQueue<>(
            Comparator.comparingInt(
                (Booking b) ->
                    b.category.equals("Gold") ? 0 : 1
            ).thenComparingLong(b -> b.sequence)
        );

    static int bookingCounter = 101;
    static long sequenceCounter = 0;


    // 2. Booking class
    static class Booking {

        String id;
        String name;
        String category;
        String seat;
        String status;
        long sequence;

        Booking(String id, String name, String category,
               String seat, String status, long sequence) {

            this.id = id;
            this.name = name;
            this.category = category;
            this.seat = seat;
            this.status = status;
            this.sequence = sequence;
        }
    }


    // 3. Validate seat
    static boolean isValidSeat(String seat) {

        if (seat == null || seat.length() != 2) {
            return false;
        }

        char row = Character.toUpperCase(seat.charAt(0));
        char number = seat.charAt(1);

        return row >= 'A' && row <= 'E'
                && number >= '1' && number <= '5';
    }

    static int getRowIndex(char row) {
        return Character.toUpperCase(row) - 'A';
    }

    static int getColIndex(char number) {
        return number - '1';
    }


    // 4. Check whether hall is full
    static boolean isHallFull() {

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {

                if (seats[i][j] == null) {
                    return false;
                }
            }
        }

        return true;
    }


    // 5. Display seat arrangement
    static void viewSeats() {

        System.out.println("\n========== SEAT LAYOUT ==========");

        System.out.println("    1   2   3   4   5");

        for (int i = 0; i < ROWS; i++) {

            System.out.print((char) ('A' + i) + "  ");

            for (int j = 0; j < COLS; j++) {

                if (seats[i][j] == null) {
                    System.out.print("[ ] ");
                } else {
                    System.out.print("[X] ");
                }
            }

            System.out.println();
        }

        System.out.println("[ ] = Available");
        System.out.println("[X] = Booked");
    }


    // 6. Book seat or join waiting list
    static void bookSeat(Scanner sc) {

        System.out.print("Enter customer name: ");
        String name = sc.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Customer name cannot be empty.");
            return;
        }

        // Avoid file delimiter problems
        name = name.replace("|", "/");

        // Check hall status FIRST
        if (isHallFull()) {

            // Hall full: customer joins waiting list
            System.out.println("\nHall is full!");

            System.out.print("Enter category (Gold/Regular): ");
            String category = sc.nextLine().trim();

            if (!category.equalsIgnoreCase("Gold")
                    && !category.equalsIgnoreCase("Regular")) {

                System.out.println("Invalid category.");
                System.out.println("Enter Gold or Regular.");
                return;
            }

            if (category.equalsIgnoreCase("Gold")) {
                category = "Gold";
            } else {
                category = "Regular";
            }

            // Generate waiting-list ID
            String id = "T" + bookingCounter++;

            Booking b = new Booking(
                id,
                name,
                category,
                "-",
                "WAITING",
                sequenceCounter++
            );

            bookingMap.put(id, b);
            waitingQueue.offer(b);

            System.out.println(name + " added to waiting list.");
            System.out.println("Booking ID: " + id);
            System.out.println("Category: " + category);

            saveData();
            return;
        }

        // Hall has available seats
        System.out.print("Enter seat (Example: B3): ");
        String seat = sc.nextLine().trim().toUpperCase();

        // Validate seat
        if (!isValidSeat(seat)) {

            System.out.println(
                "Invalid seat. Choose rows A-E and seats 1-5."
            );
            return;
        }

        int row = getRowIndex(seat.charAt(0));
        int col = getColIndex(seat.charAt(1));

        // Prevent duplicate booking
        if (seats[row][col] != null) {

            System.out.println("This seat is already booked.");
            System.out.println("Please choose another seat.");
            return;
        }

        // Create booking
        String id = "T" + bookingCounter++;

        Booking b = new Booking(
            id,
            name,
            "Regular",
            seat,
            "BOOKED",
            sequenceCounter++
        );

        seats[row][col] = b;
        bookingMap.put(id, b);

        System.out.println(
            "Booking confirmed. ID: " + id
            + " | " + name
            + " | Seat " + seat
        );

        if (isHallFull()) {
            System.out.println("Hall is now full!");
        }

        saveData();
    }


    // 7. Cancel booking
    static void cancelBooking(Scanner sc) {

        System.out.print("Enter Booking ID: ");
        String id = sc.nextLine().trim().toUpperCase();

        Booking b = bookingMap.get(id);

        if (b == null) {
            System.out.println("Booking ID not found.");
            return;
        }

        if (b.status.equals("CANCELLED")) {
            System.out.println("This booking is already cancelled.");
            return;
        }

        // Cancel waiting-list customer
        if (b.status.equals("WAITING")) {

            b.status = "CANCELLED";

            System.out.println("Waiting list booking cancelled.");
            System.out.println("Booking ID: " + id);

            saveData();
            return;
        }

        // Cancel confirmed booking
        if (b.status.equals("BOOKED")) {

            int row = getRowIndex(b.seat.charAt(0));
            int col = getColIndex(b.seat.charAt(1));

            // Release the seat
            seats[row][col] = null;
            b.status = "CANCELLED";

            System.out.println("Booking " + id + " cancelled.");

            // Assign seat to highest priority waiting customer
            assignSeatFromWaitingList(row, col);

            saveData();
        }
    }


    // 8. Assign released seat to waiting customer
    static void assignSeatFromWaitingList(int row, int col) {

        Booking next = null;

        // Remove cancelled customers and find next eligible customer
        while (!waitingQueue.isEmpty()) {

            Booking candidate = waitingQueue.poll();

            if (candidate.status.equals("WAITING")) {
                next = candidate;
                break;
            }
        }

        // No waiting customers
        if (next == null) {

            System.out.println("Seat is now available.");
            return;
        }

        // Assign the released seat
        String seat = "" + (char) ('A' + row) + (col + 1);

        next.seat = seat;
        next.status = "BOOKED";

        seats[row][col] = next;

        System.out.println(
            "Seat " + seat + " given to "
            + next.name + " (" + next.category + ")."
        );

        System.out.println("Booking ID: " + next.id);
    }


    // 9. View waiting list
    static void viewWaitingList() {

        System.out.println("\n========== WAITING LIST ==========");

        // Copy PriorityQueue so original is not changed
        PriorityQueue<Booking> temp =
            new PriorityQueue<>(waitingQueue);

        int position = 1;

        while (!temp.isEmpty()) {

            Booking b = temp.poll();

            if (b.status.equals("WAITING")) {

                System.out.println(
                    position + ". " + b.name
                    + " | " + b.category
                    + " | ID: " + b.id
                );

                position++;
            }
        }

        if (position == 1) {
            System.out.println("Waiting list is empty.");
        }
    }


    // 10. Search booking using HashMap
    static void searchBooking(Scanner sc) {

        System.out.print("Enter Booking ID: ");
        String id = sc.nextLine().trim().toUpperCase();

        Booking b = bookingMap.get(id);

        if (b == null) {
            System.out.println("Booking not found.");
            return;
        }

        System.out.println("\n========== BOOKING DETAILS ==========");
        System.out.println("Booking ID: " + b.id);
        System.out.println("Customer: " + b.name);
        System.out.println("Category: " + b.category);
        System.out.println("Status: " + b.status);

        if (b.status.equals("BOOKED")) {
            System.out.println("Seat: " + b.seat);
        } else if (b.status.equals("WAITING")) {
            System.out.println("Status: Waiting for seat.");
        } else {
            System.out.println("This booking is cancelled.");
        }
    }


    // 11. View confirmed bookings with sorting
    static void viewConfirmedBookings() {

        System.out.println("\n====== CONFIRMED BOOKINGS ======");

        ArrayList<Booking> confirmed = new ArrayList<>();

        // Collect confirmed bookings
        for (Booking b : bookingMap.values()) {

            if (b.status.equals("BOOKED")) {
                confirmed.add(b);
            }
        }

        if (confirmed.isEmpty()) {
            System.out.println("No confirmed bookings.");
            return;
        }

        // Sort bookings by booking number
        confirmed.sort(
            Comparator.comparingInt(
                b -> Integer.parseInt(b.id.substring(1))
            )
        );

        System.out.printf(
            "%-8s %-15s %-10s %-8s%n",
            "ID", "Name", "Category", "Seat"
        );

        for (Booking b : confirmed) {

            System.out.printf(
                "%-8s %-15s %-10s %-8s%n",
                b.id, b.name, b.category, b.seat
            );
        }
    }


    // 12. Save data to file
    static void saveData() {

        try (PrintWriter writer =
                new PrintWriter(new FileWriter(FILE_NAME))) {

            writer.println(bookingCounter);
            writer.println(sequenceCounter);

            for (Booking b : bookingMap.values()) {

                writer.println(
                    b.id + "|" +
                    b.name + "|" +
                    b.category + "|" +
                    b.seat + "|" +
                    b.status + "|" +
                    b.sequence
                );
            }

        } catch (IOException e) {

            System.out.println("Error saving data.");
        }
    }


    // 13. Load data from file
    static void loadData() {

        File file = new File(FILE_NAME);

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_NAME))) {

            String line;

            // Restore booking counter
            line = reader.readLine();

            if (line != null) {
                bookingCounter = Integer.parseInt(line);
            }

            // Restore sequence counter
            line = reader.readLine();

            if (line != null) {
                sequenceCounter = Long.parseLong(line);
            }

            // Restore booking records
            while ((line = reader.readLine()) != null) {

                String[] data = line.split("\\|", -1);

                if (data.length != 6) {
                    continue;
                }

                Booking b = new Booking(
                    data[0],
                    data[1],
                    data[2],
                    data[3],
                    data[4],
                    Long.parseLong(data[5])
                );

                bookingMap.put(b.id, b);

                // Restore confirmed seats
                if (b.status.equals("BOOKED")
                        && isValidSeat(b.seat)) {

                    int row = getRowIndex(b.seat.charAt(0));
                    int col = getColIndex(b.seat.charAt(1));

                    seats[row][col] = b;
                }

                // Restore waiting queue
                else if (b.status.equals("WAITING")) {

                    waitingQueue.offer(b);
                }
            }

        } catch (IOException | NumberFormatException e) {

            System.out.println("Error loading saved data.");
        }
    }


    // 14. Main method
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        // Load previous data
        loadData();

        System.out.println("==================================");
        System.out.println("    MOVIE TICKET BOOKING SYSTEM");
        System.out.println("==================================");

        while (true) {

            System.out.println("\n1. View Seats");
            System.out.println("2. Book Seat");
            System.out.println("3. Cancel Booking");
            System.out.println("4. View Waiting List");
            System.out.println("5. Search Booking");
            System.out.println("6. View Confirmed Bookings (Sorted)");
            System.out.println("7. Exit");

            System.out.print("\nEnter your choice: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1":
                    viewSeats();
                    break;

                case "2":
                    bookSeat(sc);
                    break;

                case "3":
                    cancelBooking(sc);
                    break;

                case "4":
                    viewWaitingList();
                    break;

                case "5":
                    searchBooking(sc);
                    break;

                case "6":
                    viewConfirmedBookings();
                    break;

                case "7":
                    saveData();

                    System.out.println("Data saved successfully.");
                    System.out.println("Thank you!");

                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice. Enter 1-7.");
            }
        }
    }
}
