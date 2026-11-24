// Train.java
// Purpose: Represents a single train and its details (data holder class).

public class Train {

    private int trainNumber;
    private String trainName;
    private String source;
    private String destination;
    private int totalSeats;
    private int availableSeats;
    private double farePerSeat;

    public Train(int trainNumber, String trainName, String source, String destination,
                 int totalSeats, int availableSeats, double farePerSeat) {
        this.trainNumber = trainNumber;
        this.trainName = trainName;
        this.source = source;
        this.destination = destination;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.farePerSeat = farePerSeat;
    }

    public int getTrainNumber() { return trainNumber; }
    public void setTrainNumber(int trainNumber) { this.trainNumber = trainNumber; }

    public String getTrainName() { return trainName; }
    public void setTrainName(String trainName) { this.trainName = trainName; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public double getFarePerSeat() { return farePerSeat; }
    public void setFarePerSeat(double farePerSeat) { this.farePerSeat = farePerSeat; }

    /**
     * Books the requested number of seats if available.
     * Reduces availableSeats on success.
     *
     * @param seats number of seats to book (must be > 0)
     * @return true if booking succeeded, false otherwise
     */
    public boolean bookSeats(int seats) {
        if (seats <= 0) {
            return false;
        }
        if (seats <= availableSeats) {
            availableSeats -= seats;
            return true;
        }
        return false;
    }

    /**
     * Cancels the requested number of seats and restores them.
     * Increases availableSeats without exceeding totalSeats.
     *
     * @param seats number of seats to cancel (must be > 0)
     * @return true if cancellation succeeded, false otherwise
     */
    public boolean cancelSeats(int seats) {
        if (seats <= 0) {
            return false;
        }
        if (availableSeats + seats <= totalSeats) {
            availableSeats += seats;
            return true;
        }
        return false;
    }
}
