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
    private String newPersonsFilePath = null;
    private String updatePersonsFilePath = null;

    public void setFilePath(String path) {
        this.filePath = path;
    }

    public void setNewPersonsFilePath(String path) {
        this.newPersonsFilePath = path;
    }

    public void setUpdatePersonsFilePath(String path) {
        this.updatePersonsFilePath = path;
    }

    public Person(String personID, String firstName, String lastName, String address, String birthday) {
        this.personID = personID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthday = birthday;
    }

    public boolean addPerson() {
        try {
            File personsFile = new File(filePath);
            File errorLogFile = new File("error_log.txt");
            Set<String> existingLines = new HashSet<>();

            if (personsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(personsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    existingLines.add(line.trim());
                }
                reader.close();
            }

            BufferedWriter personWriter = new BufferedWriter(new FileWriter(personsFile, true));
            BufferedWriter errorWriter = new BufferedWriter(new FileWriter(errorLogFile, true));

            boolean added = false;
            if (newPersonsFilePath != null) {
                BufferedReader newReader = new BufferedReader(new FileReader(newPersonsFilePath));
                String newLine;
                while ((newLine = newReader.readLine()) != null) {
                    String[] parts = newLine.split(",", -1);
                    if (parts.length != 6 ||
                            !isValidID(parts[0].trim()) ||
                            !isValidAddress(parts[3].trim()) ||
                            !isValidDate(parts[4].trim()) ||
                            existingLines.contains(newLine.trim())) {
                        errorWriter.write("Validation or duplicate failed: " + newLine);
                        errorWriter.newLine();
                        continue;
                    }
                    personWriter.write(newLine.trim());
                    personWriter.newLine();
                    added = true;
                }
                newReader.close();
            } else {
                String personRecord = toCSV();
                if (!isValidID(personID) || !isValidAddress(address) || !isValidDate(birthday) || existingLines.contains(personRecord)) {
                    errorWriter.write("Validation failed: " + personRecord);
                    errorWriter.newLine();
                    errorWriter.close();
                    personWriter.close();
                    return false;
                }
                personWriter.write(personRecord);
                personWriter.newLine();
                added = true;
            }

            personWriter.close();
            errorWriter.close();
            return added;

        } catch (IOException e) {
            return false;
        }
    }

    public boolean updatePersonalDetails() {
        try {
            File originalFile = new File(filePath);
            File updateFile = updatePersonsFilePath != null ? new File(updatePersonsFilePath) : null;

            List<String> originalLines = new ArrayList<>();
            boolean updated = false;

            // Load original persons.txt
            BufferedReader originalReader = new BufferedReader(new FileReader(originalFile));
            String line;
            while ((line = originalReader.readLine()) != null) {
                originalLines.add(line.trim());
            }
            originalReader.close();

            // Load updates
            List<String[]> updates = new ArrayList<>();
            if (updateFile != null && updateFile.exists()) {
                BufferedReader updateReader = new BufferedReader(new FileReader(updateFile));
                while ((line = updateReader.readLine()) != null) {
                    updates.add(Arrays.stream(line.split(",", -1)).map(String::trim).toArray(String[]::new));
                }
                updateReader.close();
            } else {
                updates.add(new String[]{personID.trim(), personID.trim(), firstName.trim(), lastName.trim(), address.trim(), birthday.trim()});
            }

            // Apply updates
            for (String[] updateParts : updates) {

                String oldID, newID, newFirstName, newLastName, newAddress, newBirthday;
                if (updateParts.length == 6) {
                    // Correct assumption: 6 fields means full update with new and old ID
                    oldID = updateParts[0];
                    newID = updateParts[1];
                    newFirstName = updateParts[2];
                    newLastName = updateParts[3];
                    newAddress = updateParts[4];
                    newBirthday = updateParts[5];
                } else if (updateParts.length == 5) {
                    // If only 5 fields, assume same ID
                    oldID = updateParts[0];
                    newID = updateParts[0];
                    newFirstName = updateParts[1];
                    newLastName = updateParts[2];
                    newAddress = updateParts[3];
                    newBirthday = updateParts[4];
                } else {
                    System.out.println("Skipped malformed update line: " + Arrays.toString(updateParts));
                    continue;
                }

                for (int i = 0; i < originalLines.size(); i++) {
                    String[] parts = originalLines.get(i).split(",", -1);
                    if (parts.length < 6) continue;

                    String originalID = parts[0].trim();
                    if (!originalID.equals(oldID)) continue;

                    String originalFirstName = parts[1].trim();
                    String originalLastName = parts[2].trim();
                    String originalAddress = parts[3].trim();
                    String originalBirthday = parts[4].trim();
                    String originalIsSuspended = parts[5].trim();
                    String originalDemerits = (parts.length > 6) ? parts[6] : "";

                    // Validation
                    int age = getAge(originalBirthday);
                    boolean birthdayChanging = !newBirthday.equals(originalBirthday);
                    boolean idChanging = !newID.equals(originalID);
                    boolean nameOrAddressChanging = !newFirstName.equals(originalFirstName)
                            || !newLastName.equals(originalLastName)
                            || !newAddress.equals(originalAddress);

                    if (age < 18 && !newAddress.equals(originalAddress)) {
                        System.out.println("Skipped: under 18 can't change address");
                        continue;
                    }
                    if (birthdayChanging && nameOrAddressChanging) {
                        System.out.println("Skipped: changing birthday and other fields");
                        System.out.println("Comparing:");
                        System.out.println("Original Birthday: '" + originalBirthday + "'");
                        System.out.println("New Birthday:      '" + newBirthday + "'");
                        continue;
                    }
                    if (idChanging && isEvenDigit(originalID.charAt(0))) {
                        System.out.println("Skipped: even ID can't be changed");
                        continue;
                    }
                    if (!isValidID(newID.trim()) || !isValidAddress(newAddress) || !isValidDate(newBirthday)) {
                        System.out.println("Skipped: invalid input data");
                        System.out.println("Check input:");
                        System.out.println("Old ID: " + oldID);
                        System.out.println("New ID: " + newID);
                        System.out.println("Valid ID? " + isValidID(newID));
                        System.out.println("Valid Address? " + isValidAddress(newAddress));
                        System.out.println("Valid Date? " + isValidDate(newBirthday));
                        continue;
                    }

                    // Replace line
                    String updatedLine = String.join(",", newID, newFirstName, newLastName, newAddress, newBirthday, originalIsSuspended, originalDemerits);
                    System.out.println("Updating line: " + updatedLine);
                    originalLines.set(i, updatedLine);
                    updated = true;
                    break;
                }
            }

            // Write updated lines back to file
            if (updated) {
                System.out.println("Writing updates to: " + new File(filePath).getAbsolutePath());
                BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                for (String updatedLine : originalLines) {
                    writer.write(updatedLine);
                    writer.newLine();
                }
                writer.close();
            }

            return updated;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

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
                if (parts[0].equals(personID)) {
                    int age = getAge(parts[4]);
                    String existingDemerits = (parts.length > 6) ? parts[6] : "";
                    HashMap<LocalDate, Integer> allDemerits = new HashMap<>();

                    if (!existingDemerits.isEmpty()) {
                        for (String entry : existingDemerits.split(";")) {
                            String[] pair = entry.split(":");
                            if (pair.length == 2) {
                                allDemerits.put(LocalDate.parse(pair[0], formatter), Integer.parseInt(pair[1]));
                            }
                        }
                    }

                    allDemerits.put(offenseDate, points);
                    int totalRecentPoints = allDemerits.entrySet().stream().filter(e -> e.getKey().isAfter(LocalDate.now().minusYears(2))).mapToInt(Map.Entry::getValue).sum();
                    boolean suspend = (age < 21 && totalRecentPoints > 6) || (age >= 21 && totalRecentPoints > 12);

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
            return updated ? "Success" : "Failed";

        } catch (Exception e) {
            return "Failed";
        }
    }

    private boolean isValidID(String id) {
        if (id == null || id.length() != 10) return false;

        // First 2 digits must be between 2–9
        if (!id.substring(0, 2).matches("[2-9]{2}")) return false;

        // Last 2 characters must be uppercase letters
        if (!id.substring(8).matches("[A-Z]{2}")) return false;

        // Middle 6 characters must include at least 2 special characters
        String middle = id.substring(2, 8);
        int specialCount = 0;
        for (char c : middle.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                specialCount++;
            }
        }

        return specialCount >= 2;
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
