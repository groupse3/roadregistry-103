package com.roadregistry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import java.io.FileWriter;

public class PersonTest {

    // --- addPerson Tests ---
    @BeforeEach
    public void setup() throws Exception {
        new FileWriter("test_persons.txt", false).close(); // Clear the file before every test
    }


    @Test
    public void testAddPerson_validInput_shouldReturnTrue() {
        Person p = new Person("36cc$%xyER", "Danush", "Bala", "1|Elizabath Street|Melbourne|Victoria|Australia", "12-11-1989");
        p.setFilePath("test_persons.txt");
        assertTrue(p.addPerson());
    }

    @Test
    public void testAddPerson_invalidID_shouldReturnFalse() {
        Person p = new Person("1245", "Pasan", "Wije", "123A|La Trobe St|Melbourne|Victoria|Australia", "15-11-1995");
        p.setFilePath("test_persons.txt");
        assertFalse(p.addPerson());
    }

    @Test
    public void testAddPerson_invalidAddress_shouldReturnFalse() {
        Person p = new Person("45u_d%&fAB", "Bruse", "Lee", "34A|CHruch Road|Melbourne|NSW|Australia", "20-12-2000");
        p.setFilePath("test_persons.txt");
        assertFalse(p.addPerson());
    }

    @Test
    public void testAddDemeritPoints_exceedLimitUnder21_shouldSuspend() {
        Person p = new Person("34zz##yyRR", "Tom", "Cruise", "103|Beach Rd|Melbourne|Victoria|Australia", "15-03-2001");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        p.addDemeritPoints("34zz##yyRR", 3, "01-05-2024");
        assertEquals("Success", p.addDemeritPoints("34zz##yyRR", 4, "01-05-2024"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        new FileWriter("test_persons.txt", false).close(); // Optional: clear after as well
    }

}
