package com.roadregistry;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

public class PersonTest {

    @BeforeEach
    public void setup() throws Exception {
        new FileWriter("test_persons.txt", false).close(); // Clear file before each test
    }

    @AfterEach
    public void tearDown() throws Exception {
        new FileWriter("test_persons.txt", false).close(); // Clear file after each test
    }

    // ----------------- addPerson Tests -----------------

    // validating add person input
    @Test
    public void testAddPerson_validInput_shouldReturnTrue() {
        Person p = new Person("36cc$%xyER", "Danush", "Bala", "1|Elizabeth Street|Melbourne|Victoria|Australia", "12-11-1989");
        p.setFilePath("test_persons.txt");
        assertTrue(p.addPerson());
    }

    // validating add person input - ID
    @Test
    public void testAddPerson_invalidID_shouldReturnFalse() {
        Person p = new Person("1245", "Pasan", "Wije", "123A|La Trobe St|Melbourne|Victoria|Australia", "15-11-1995");
        p.setFilePath("test_persons.txt");
        assertFalse(p.addPerson());
    }

    // validating add person input - Address
    @Test
    public void testAddPerson_invalidAddress_shouldReturnFalse() {
        Person p = new Person("45u_d%&fAB", "Bruce", "Lee", "34A|Church Road|Melbourne|NSW|Australia", "20-12-2000");
        p.setFilePath("test_persons.txt");
        assertFalse(p.addPerson());
    }

    // validating add person input - Date
    @Test
    public void testAddPerson_invalidDate_shouldReturnFalse() {
        Person p = new Person("67z_z&hGTT", "Liam", "Jones", "10|Collins St|Melbourne|Victoria|Australia", "2000-01-01");
        p.setFilePath("test_persons.txt");
        assertFalse(p.addPerson());
    }

    // validating add person input - duplicate entry
    @Test
    public void testAddPerson_duplicateEntry_shouldReturnFalse() {
        Person p1 = new Person("34dd!@qwEE", "Emma", "Stone", "88|Lygon St|Melbourne|Victoria|Australia", "25-07-1988");
        p1.setFilePath("test_persons.txt");
        p1.addPerson();
        Person p2 = new Person("34dd!@qwEE", "Emma", "Stone", "88|Lygon St|Melbourne|Victoria|Australia", "25-07-1988");
        p2.setFilePath("test_persons.txt");
        assertFalse(p2.addPerson());
    }

    // ----------------- addDemeritPoints Tests -----------------

    // validating add demerit points
    @Test
    public void testAddDemeritPoints_valid_shouldReturnSuccess() {
        Person p = new Person("22aa!!rrYY", "Nina", "Brown", "55|Bourke St|Melbourne|Victoria|Australia", "15-04-2000");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        assertEquals("Success", p.addDemeritPoints("22aa!!rrYY", 4, "01-06-2024"));
    }

    // validating add demerit points - Date format
    @Test
    public void testAddDemeritPoints_invalidDateFormat_shouldReturnFailed() {
        Person p = new Person("77zz%%rrLL", "Tom", "Smith", "22|King St|Melbourne|Victoria|Australia", "15-03-2001");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        assertEquals("Failed", p.addDemeritPoints("77zz%%rrLL", 3, "2024/06/01"));
    }

    // validating add demerit points - Points
    @Test
    public void testAddDemeritPoints_invalidPoints_shouldReturnFailed() {
        Person p = new Person("89kk@@ppRR", "Ava", "Williams", "9|Queen St|Melbourne|Victoria|Australia", "12-12-1995");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        assertEquals("Failed", p.addDemeritPoints("89kk@@ppRR", 8, "01-06-2024"));
    }

    // validating suspension trigger
    @Test
    public void testAddDemeritPoints_triggerSuspendUnder21_shouldReturnSuccess() {
        Person p = new Person("56uu$$ddGG", "Mia", "Chen", "3|John St|Melbourne|Victoria|Australia", "01-01-2006");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        p.addDemeritPoints("56uu$$ddGG", 3, "01-01-2024");
        assertEquals("Success", p.addDemeritPoints("56uu$$ddGG", 4, "01-03-2024"));
    }

    // validating nonexistent demerit points
    @Test
    public void testAddDemeritPoints_notInFile_shouldReturnFailed() {
        Person p = new Person("93hh!!wwUU", "John", "Doe", "6|Long St|Melbourne|Victoria|Australia", "10-10-1985");
        p.setFilePath("test_persons.txt");
        assertEquals("Failed", p.addDemeritPoints("unknownID", 3, "01-01-2023"));
    }

    // ----------------- updatePersonalDetailsFromFile Tests -----------------

    // updating valid name
    @Test
    public void testUpdateValidNameOnly_shouldPass() {
        Person p = new Person("79dd$#ggMM", "George", "Russell", "50|Ocean Ave|Melbourne|Victoria|Australia", "01-01-1995");
        p.setFilePath("test_persons.txt");
        p.addPerson();

        Person p1 = new Person("79dd$#ggMM", "George", "Smith", "50|Ocean Ave|Melbourne|Victoria|Australia", "01-01-1995");
        p1.setFilePath("test_persons.txt");
        assertTrue(p1.updatePersonalDetails());
    }

    // updating ID
    @Test
    public void testUpdateChangeEvenID_shouldFail() {
        Person p = new Person("24bb$%ttAA", "Carlos", "Sainz", "10|Market St|Melbourne|Victoria|Australia", "20-03-1998");
        p.setFilePath("test_persons.txt");
        p.addPerson();
        Person p1 = new Person("34bb$%ttAA", "Carlos", "Sainz", "10|Market St|Melbourne|Victoria|Australia", "20-03-1998");
        assertFalse(p1.updatePersonalDetails());
    }

    // updating birthday OR other details validation
    @Test
    public void testUpdateBirthdayAndOtherDetails_shouldFail() {
        Person p = new Person("55zz@@nnGG", "Charles", "Leclerc", "11|Wall St|Melbourne|Victoria|Australia", "15-09-1997");
        p.setFilePath("test_persons.txt");
        p.addPerson();

        // Attempt to update birthday and another field (e.g., last name)
        Person p1 = new Person("55zz@@nnGG", "Charles", "Lec", "11|Wall St|Melbourne|Victoria|Australia", "01-01-2000");
        p1.setFilePath("test_persons.txt");
        assertFalse(p1.updatePersonalDetails());
    }

    // updating under 18 address
    @Test
    public void testUpdateUnder18Address_shouldFail() {
        Person p = new Person("78cc!#yyWW", "Max", "Verstappen", "100|Main Rd|Geelong|Victoria|Australia", "10-08-2010");
        p.setFilePath("test_persons.txt");
        p.addPerson();

        // Try changing address for under-18
        Person p1 = new Person("78cc!#yyWW", "Max", "Verstappen", "200|New Rd|Geelong|Victoria|Australia", "10-08-2010");
        p1.setFilePath("test_persons.txt");
        assertFalse(p1.updatePersonalDetails());
    }

// updating address
        @Test
        public void testUpdateValidAddressDifferentPerson_shouldPass() {
            // Add a new person

            Person p = new Person("65zx@#rtKL", "Lewis", "Hamilton", "77|Speedway Ave|Geelong|Victoria|Australia", "05-01-1985");
            p.setFilePath("test_persons.txt");
            p.addPerson();

            // Try changing address (valid case, age > 18, same birthday)
            Person p1 = new Person("65zx@#rtKL", "Lewis", "Hamilton", "88|Track Rd|Geelong|Victoria|Australia", "05-01-1985");
            p1.setFilePath("test_persons.txt");
            assertTrue(p1.updatePersonalDetails());
        }
}

