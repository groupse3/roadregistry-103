package org.example;

import com.roadregistry.Person;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Person p = new Person(
                "28ab!!zzPP",
                "Neville",
                "Longbottom",
                "10|La trobe Street|Melbourne|Victoria|Australia",
                "30-07-1990"
        );

        boolean added = p.addPerson();
        if (added) {
            System.out.println("Person added successfully.");
        } else {
            System.out.println("Failed to add person.");
        }
    }
}