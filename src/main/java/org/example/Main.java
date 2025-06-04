package org.example;

import com.roadregistry.Person;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {


        Person frodo = new Person("23fr##bgFR", "Frodo", "Baggins", "1|Bagshot Row|Shire|Victoria|Australia", "12-10-1985");
        frodo.setFilePath("persons.txt");
        System.out.println("Add Frodo: " + frodo.addPerson());


        Person samUpdated = new Person("34sw$$gmGM", "Samwise", "Gamgee", "20|Gardener St|Shire|Victoria|Australia", "01-02-1987");
        samUpdated.setFilePath("persons.txt");
        samUpdated.setOldPersonID("34sw$$gmGM");
        System.out.println("Update Sam's address: " + samUpdated.updatePersonalDetails()); // Expect: true*/



        String suspendStatus = frodo.addDemeritPoints("45ar%%rnAR", 6, "01-03-2024"); // +7 -> 13 points
        System.out.println("Aragorn suspended after demerits: " + suspendStatus); // Expect: "Success"

    }
}