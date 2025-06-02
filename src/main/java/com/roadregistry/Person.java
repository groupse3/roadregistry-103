package com.roadregistry;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// creating Person class with attributes
public class Person {
    private String personID;
    private String firstName;
    private String lastName;
    private String address;
    private String birthday;
    private boolean isSuspended = false;
    private HashMap<LocalDate, Integer> demeritPoints = new HashMap<>();

    // setting filepath to relevant txt file
    private String filePath = "persons.txt";

    public void setFilePath(String path) {
        this.filePath = path;
    }

    // Person constructor
    public Person(String personID, String firstName, String lastName, String address, String birthday) {
        this.personID = personID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthday = birthday;
    }

    // AddPerson method
    public boolean addPerson() {
        if (!isValidID(this.personID) || !isValidAddress(this.address) || !isValidDate(this.birthday)) {
            return false;
        }

        try {
            File file = new File(filePath);

            // checking if file exists and if person already exists in file
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 1 && parts[0].equals(this.personID)) {
                        reader.close();
                        return false;  // Duplicate found
                    }
                }
                reader.close();
            }

            // Appending person to txt file
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.write(toCSV());
            writer.newLine();
            writer.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Update Person Details method
    public boolean updatePersonalDetails(String newID, String newFirstName, String newLastName, String newAddress, String newBirthday) {
        try {
            File inputFile = new File(filePath);
            List<String> finalLines = new ArrayList<>();
            boolean updated = false;

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;

            // splitting file data into parts to obtain details to compare
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 6) {
                    finalLines.add(line);
                    continue;
                }

                String originalID = parts[0];
                String originalFirstName = parts[1];
                String originalLastName = parts[2];
                String originalAddress = parts[3];
                String originalBirthday = parts[4];
                String originalIsSuspended = parts[5];

                // identifying Ids and using boolean to find out which data is changing
                if (originalID.equals(this.personID)) {
                    int age = getAge(originalBirthday);

                    String updatedLine;

                    //checking if birthday is changing
                    boolean birthdayChanging = !newBirthday.equals(originalBirthday);
                    if (birthdayChanging) {
                        finalLines.add(line);

                        updatedLine = String.join(",", originalID, originalFirstName, originalLastName, originalAddress, newBirthday, originalIsSuspended);
                    }

                    //if birthdate is not changing, other details can be changed
                    else {
                        boolean otherChanging = !newID.equals(originalID) ||
                                !newFirstName.equals(originalFirstName) ||
                                !newLastName.equals(originalLastName) ||
                                !newAddress.equals(originalAddress);

                        // Update all changing details
//                        if (birthdayChanging && otherChanging) {
//                            finalLines.add(line);
//                            continue;
//                        }

                        if (age > 18 && !newAddress.equals(originalAddress)) {
                            finalLines.add(line);
                            continue;
                        }

                        if (isEvenDigit(originalID.charAt(0)) && !newID.equals(originalID)) {
                            finalLines.add(line);
                            continue;
                        }

                        if (!isValidID(newID) || !isValidAddress(newAddress) || !isValidDate(newBirthday)) {
                            finalLines.add(line);
                            continue;
                        }

                        updatedLine = String.join(",", newID, newFirstName, newLastName, newAddress, originalBirthday, originalIsSuspended);
                    }

                    finalLines.add(updatedLine);
                    updated = true; // to return a bool value
                } else {
                    finalLines.add(line);
                }
            }

            reader.close();
            // adding this new line to the txt file
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false));
            for (String finalLine : finalLines) {
                writer.write(finalLine);
                writer.newLine();
            }
            writer.close();

            return updated;
        } catch (IOException e) {
            return false;
        }
    }

    // add Demerit points method
    public String addDemeritPoints(String personID, int points, String dateStr) {
        // validating date and demerit points
        if (!dateStr.matches("\\d{2}-\\d{2}-\\d{4}") || points < 1 || points > 6) return "Failed";

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate offenseDate = LocalDate.parse(dateStr, formatter);
            demeritPoints.put(offenseDate, points);

            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();

            boolean updated = false;
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            for (String l : lines) {
                String[] parts = l.split(",", -1);
                if (parts[0].equals(personID)) {
                    int age = getAge(parts[4]);

                    int totalRecentPoints = 0;
                    LocalDate now = LocalDate.now();
                    for (Map.Entry<LocalDate, Integer> entry : demeritPoints.entrySet()) {
                        if (entry.getKey().isAfter(now.minusYears(2))) {
                            totalRecentPoints += entry.getValue();
                        }
                    }

                    boolean suspend = (age < 21 && totalRecentPoints > 6) || (age >= 21 && totalRecentPoints > 12);
                    String newLine = String.join(",", parts[0], parts[1], parts[2], parts[3], parts[4], String.valueOf(suspend), String.valueOf(totalRecentPoints));
                    writer.write(newLine);
                    writer.newLine();
                    updated = true;
                } else {
                    writer.write(l);
                    writer.newLine();
                }
            }

            writer.close();
            return updated ? "Success" : "Failed";
        } catch (Exception e) {
            return "Failed";
        }
    }

    private boolean isValidID(String id) {
        return id.matches("^[2-9]{2}.{1,6}[!@#$%^&()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{2,}.[A-Z]{2}$") && id.length() == 10;
    }

    private boolean isValidAddress(String address) {
        return address.matches("\\d+\\|[^|]+\\|[^|]+\\|Victoria\\|[^|]+");
    }

    private boolean isValidDate(String date) {
        return date.matches("\\d{2}-\\d{2}-\\d{4}");
    }

    private int getAge(String dob) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate birthDate = LocalDate.parse(dob, formatter);
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    private boolean isEvenDigit(char ch) {
        return Character.isDigit(ch) && (ch - '0') % 2 == 0;
    }

    private String toCSV() {
        return String.join(",", personID, firstName, lastName, address, birthday, String.valueOf(isSuspended));
    }
}