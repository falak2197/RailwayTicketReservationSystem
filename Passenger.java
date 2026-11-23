/**
 * Passenger.java
 * Represents one passenger in a railway booking.
 * Data is kept private (encapsulation) and accessed only through getters/setters.
 */
public class Passenger {

    // Private instance variables (encapsulation)
    private int passengerId;
    private String name;
    private int age;
    private String gender;
    private String phoneNumber;

    // Parameterised constructor
    public Passenger(int passengerId, String name, int age, String gender, String phoneNumber) {
        this.passengerId = passengerId;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
    }

    // ---------- Getters ----------
    public int getPassengerId() {
        return passengerId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    // ---------- Setters ----------
    // No setter for passengerId: an ID is assigned once and should never change.
    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Prints this passenger's details in the required format
    public void displayPassengerDetails() {
        System.out.println("Passenger ID: " + passengerId);
        System.out.println("Name: " + name);
        System.out.println("Age: " + age);
        System.out.println("Gender: " + gender);
        System.out.println("Phone: " + phoneNumber);
    }
}