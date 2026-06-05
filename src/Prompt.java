import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;

public class Prompt {
    public static File getInputFile() { // checks if the file exists and is readable
        File file = new File("Registration.txt");
        if (file.exists() && file.canRead()) {
            return file;
        }
        System.out.println("Looking in: " + file.getAbsolutePath());
        return file;
    }

    public static Scanner getInputScanner() { // opens a scanner
        File inputFile = getInputFile();
        try {
            return new Scanner(inputFile);
        } catch (FileNotFoundException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }

    public static PrintWriter getPrintWriter() { // opens a print writer to write to the file
        String filename = "Registration.txt";
        try {
            FileWriter fw = new FileWriter(filename, true);
            return new PrintWriter(fw);
        } catch (IOException e) {
            System.out.println("Error creating file: " + e.getMessage());
            return null;
        }
    }
}