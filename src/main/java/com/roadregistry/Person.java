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
    // using hash map to store the demerits points to the
    private HashMap<LocalDate, Integer> demeritPoints = new HashMap<>();

    // Use this global filepath if the path is not set by the user
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
                // check the ID is matching to any avaialble IDs this variable is use to handle
                // if user try to change id
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
                boolean nameOrAddressOrIdChanging = !firstName.equals(originalFirstName)
                        || !lastName.equals(originalLastName)
                        || !address.equals(originalAddress) ||idChanging;

                // Validate if the age is below 18 address cannot be changed
                if (age < 18 && !address.equals(originalAddress)) {
                    System.out.println("Skipped: under 18 can't change address");
                    continue;
                }
                // if birthday is changing you cannot change any other values
                if (birthdayChanging && nameOrAddressOrIdChanging) {
                    System.out.println("Skipped: changing birthday and other fields");
                    continue;
                }
                // need to check the new values are valid ID , Valid Address Format , Valid
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

            for (String l : lines) {
                String[] parts = l.split(",", -1);
                String currentID = parts[0].trim();

                if (currentID.equals(personID.trim())) {
                    // Extract data
                    int age = getAge(parts[4].trim());
                    String existingDemerits = (parts.length > 6) ? parts[6].trim() : "";
                    HashMap<LocalDate, Integer> allDemerits = new HashMap<>();

                    if (!existingDemerits.isEmpty()) {
                        for (String entry : existingDemerits.split(";")) {
                            String[] pair = entry.split(":");
                            if (pair.length == 2) {
                                allDemerits.put(LocalDate.parse(pair[0].trim(), formatter), Integer.parseInt(pair[1].trim()));
                            }
                        }
                    }

                    // Add new demerit point
                    allDemerits.put(offenseDate, points);

                    // This line of code adds up all the demerit points a person got in the last two years.
                    // It looks through a list of demerits, where each one has a date and a number of points.
                    // It filters out the old ones and keeps only those that happened in the past two years.
                    // Then, it takes the points from those recent demerits and adds them together.
                    // This total is used to check if the person should be suspended, based on their age.
                    int totalRecentPoints = allDemerits.entrySet().stream()
                            .filter(e -> e.getKey().isAfter(LocalDate.now().minusYears(2)))
                            .mapToInt(Map.Entry::getValue)
                            .sum();
                    System.out.println(totalRecentPoints);
                    boolean suspend = (age < 21 && totalRecentPoints > 6) || (age >= 21 && totalRecentPoints > 12);

                    // Build updated line
                    List<String> updatedEntries = new ArrayList<>();
                    for (Map.Entry<LocalDate, Integer> entry : allDemerits.entrySet()) {
                        updatedEntries.add(entry.getKey().format(formatter) + ":" + entry.getValue());
                    }

                    String newLine = String.join(",", parts[0], parts[1], parts[2], parts[3], parts[4], String.valueOf(suspend), String.join(";", updatedEntries));
                    writer.write(newLine);
                    writer.newLine();
                    updated = true;

                } else {
                    // Write the original unmodified line
                    writer.write(l.trim());
                    writer.newLine();
                }
            }

            writer.close();
            return updated ? "Success" : "Failed";

        } catch (Exception e) {
            return "Failed";
        }
    }

    // Validates if the person ID meets the required format
    private boolean isValidID(String id) {
        //Id length should equal 10
        if (id == null || id.length() != 10) return false;

        if (!id.substring(0, 2).matches("[2-9]{2}")) return false;

        // check the last 2 digts in the ID are two capital letters
        if (!id.substring(8).matches("[A-Z]{2}")) return false;

        // checking the special character count in the middle
        String middle = id.substring(2, 8);
        int specialCount = 0;
        for (char c : middle.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                specialCount++;
            }
        }
        return specialCount >= 2;
    }

    /*

   We introduced the following functions to promote reusability in the addPerson and
    updatePersonalDetails features,
   and to support potential future enhancements more effectively.

     */


    // Checks if the address is in the correct format
    private boolean isValidAddress(String address) {
        // check the address is in the correct format using regex function and must include victoria in state field
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
