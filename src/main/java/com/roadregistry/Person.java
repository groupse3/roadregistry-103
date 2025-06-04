package com.roadregistry;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Person {
    private String personID;
    private String firstName;
    private String lastName;
    private String address;
    private String birthday;
    private boolean isSuspended = false;
    private HashMap<LocalDate, Integer> demeritPoints = new HashMap<>();

    private String filePath = "persons.txt";
    private String oldPersonID = null;

    // Sets the old person ID for reference during updates
    public void setOldPersonID(String oldID) {
        this.oldPersonID = oldID;
    }

    // Sets the file path where person data is stored
    public void setFilePath(String path) {
        this.filePath = path;
    }


    // Constructor to initialize a Person object with details
    public Person(String personID, String firstName, String lastName, String address, String birthday) {
        this.personID = personID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthday = birthday;
    }


    // Adds a new person to the file after validation
    public boolean addPerson() {
        try {
            File personsFile = new File(filePath);
            File errorLogFile = new File("error_log.txt");
            Set<String> existingLines = new HashSet<>();

            // Read existing lines from the file if it exists
            if (personsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(personsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    existingLines.add(line.trim());
                }
                reader.close();
            }

            // Prepare writers for appending data to files
            BufferedWriter personWriter = new BufferedWriter(new FileWriter(personsFile, true));
            BufferedWriter errorWriter = new BufferedWriter(new FileWriter(errorLogFile, true));

            // Convert person details to CSV format
            String personRecord = toCSV();

            // Validate the person details and check for duplicates
            if (!isValidID(personID) || !isValidAddress(address) || !isValidDate(birthday) || existingLines.contains(personRecord)) {
                errorWriter.write("Validation failed: " + personRecord);
                errorWriter.newLine();
                errorWriter.close();
                personWriter.close();
                return false; // Return false if validation fails
            }

            // Write the valid person record to the file
            personWriter.write(personRecord);
            personWriter.newLine();

            personWriter.close();
            errorWriter.close();
            return true; // Return true if the person is added successfully

        } catch (IOException e) {
            return false; // Return false if an exception occurs
        }
    }


    // Updates personal details of an existing person in the file
    public boolean updatePersonalDetails() {
        try {
            // Read the original file into a list of lines
            File originalFile = new File(filePath);
            List<String> originalLines = new ArrayList<>();
            boolean updated = false;

            BufferedReader originalReader = new BufferedReader(new FileReader(originalFile));
            String line;
            while ((line = originalReader.readLine()) != null) {
                originalLines.add(line.trim());
            }
            originalReader.close();

            // Iterate through the lines to find and update the matching record
            for (int i = 0; i < originalLines.size(); i++) {
                String[] parts = originalLines.get(i).split(",", -1);
                if (parts.length < 6) continue;

                String originalID = parts[0].trim();
                String idToMatch = (oldPersonID == null || oldPersonID.isEmpty()) ? personID : oldPersonID;

                // Check if the current line matches the person ID
                if (!originalID.equals(idToMatch)) continue;

                // Check if the ID is changing and validate the change
                boolean idChanging = !personID.equals(originalID);
                if (idChanging && isEvenDigit(originalID.charAt(0))) {
                    System.out.println("Skipped: even ID can't be changed");
                    continue;
                }

                // Extract original details for comparison
                String originalFirstName = parts[1].trim();
                String originalLastName = parts[2].trim();
                String originalAddress = parts[3].trim();
                String originalBirthday = parts[4].trim();
                String originalIsSuspended = parts[5].trim();
                String originalDemerits = (parts.length > 6) ? parts[6] : "";

                // Validate changes based on age and other conditions
                int age = getAge(originalBirthday);
                boolean birthdayChanging = !birthday.equals(originalBirthday);
                boolean nameOrAddressChanging = !firstName.equals(originalFirstName)
                        || !lastName.equals(originalLastName)
                        || !address.equals(originalAddress);

                if (age < 18 && !address.equals(originalAddress)) {
                    System.out.println("Skipped: under 18 can't change address");
                    continue;
                }
                if (birthdayChanging && nameOrAddressChanging) {
                    System.out.println("Skipped: changing birthday and other fields");
                    continue;
                }
                if (!isValidID(personID) || !isValidAddress(address) || !isValidDate(birthday)) {
                    continue;
                }

                // Update the line with new details
                String updatedLine = String.join(",", personID, firstName, lastName, address, birthday, originalIsSuspended, originalDemerits);
                originalLines.set(i, updatedLine);
                updated = true;
                break;
            }

            // Write the updated lines back to the file if changes were made
            if (updated) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                for (String updatedLine : originalLines) {
                    writer.write(updatedLine);
                    writer.newLine();
                }
                writer.close();
            }

            return updated; // Return true if the update was successful

        } catch (IOException e) {
            e.printStackTrace();
            return false;  // Return false if an exception occurs
        }
    }

    // Adds demerit points to a person and updates suspension status if necessary
    public String addDemeritPoints(String personID, int points, String dateStr) {
        if (!dateStr.matches("\\d{2}-\\d{2}-\\d{4}") || points < 1 || points > 6) return "Failed";
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate offenseDate = LocalDate.parse(dateStr, formatter);
            List<String> lines = new ArrayList<>();

            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();

            boolean updated = false;
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            // Iterate through the lines to find and update the matching record
            for (String l : lines) {
                String[] parts = l.split(",", -1);
                if (parts[0].equals(personID)) {
                    int age = getAge(parts[4]);
                    String existingDemerits = (parts.length > 6) ? parts[6] : "";
                    HashMap<LocalDate, Integer> allDemerits = new HashMap<>();

                    // Parse existing demerit points
                    if (!existingDemerits.isEmpty()) {
                        for (String entry : existingDemerits.split(";")) {
                            String[] pair = entry.split(":");
                            if (pair.length == 2) {
                                allDemerits.put(LocalDate.parse(pair[0], formatter), Integer.parseInt(pair[1]));
                            }
                        }
                    }

                    // Add the new demerit points
                    allDemerits.put(offenseDate, points);

                    // Calculate total recent points and determine suspension status
                    int totalRecentPoints = allDemerits.entrySet().stream().filter(e -> e.getKey().isAfter(LocalDate.now().minusYears(2))).mapToInt(Map.Entry::getValue).sum();
                    boolean suspend = (age < 21 && totalRecentPoints > 6) || (age >= 21 && totalRecentPoints > 12);

                    // Update the line with new details
                    List<String> updatedEntries = new ArrayList<>();
                    for (Map.Entry<LocalDate, Integer> entry : allDemerits.entrySet()) {
                        updatedEntries.add(entry.getKey().format(formatter) + ":" + entry.getValue());
                    }
                    String newLine = String.join(",", parts[0], parts[1], parts[2], parts[3], parts[4], String.valueOf(suspend), String.join(";", updatedEntries));
                    writer.write(newLine);
                    writer.newLine();
                    updated = true;
                } else {
                    writer.write(l);
                    writer.newLine();
                }
            }

            writer.close();
            return updated ? "Success" : "Failed"; // Return success or failure

        } catch (Exception e) {
            return "Failed"; // Return failure if an exception occurs
        }
    }

    // Validates if the person ID meets the required format
    private boolean isValidID(String id) {
        if (id == null || id.length() != 10) return false;
        if (!id.substring(0, 2).matches("[2-9]{2}")) return false;
        if (!id.substring(8).matches("[A-Z]{2}")) return false;
        String middle = id.substring(2, 8);
        int specialCount = 0;
        for (char c : middle.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                specialCount++;
            }
        }
        return specialCount >= 2;
    }

    // Checks if the address is in the correct format
    private boolean isValidAddress(String address) {
        return address.matches("\\d+\\|[^|]+\\|[^|]+\\|Victoria\\|[^|]+");
    }

    // Checks if the date is in the correct foramt.
    private boolean isValidDate(String date) {
        return date.matches("\\d{2}-\\d{2}-\\d{4}");
    }

    // Determines the age of a person based on their date of birth
     private int getAge(String dob) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate birthDate = LocalDate.parse(dob, formatter);
        return LocalDate.now().getYear() - birthDate.getYear();
    } 

    // Checks if a character is an even digit
    private boolean isEvenDigit(char ch) {
        return Character.isDigit(ch) && (ch - '0') % 2 == 0;
    }

    // Converts the person's details to a format string
    private String toCSV() {
        return String.join(",", personID, firstName, lastName, address, birthday, String.valueOf(isSuspended));
    }
}
