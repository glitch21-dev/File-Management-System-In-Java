import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class FileManagementSystem {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n===== File Management System =====");
            System.out.println("1. Create File");
            System.out.println("2. Write to File");
            System.out.println("3. Read File");
            System.out.println("4. Delete File");
            System.out.println("5. List Files in Directory");
            System.out.println("6. Copy File");
            System.out.println("7. Move File");
            System.out.println("8. Search File");
            System.out.println("9. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
//Using switch case
            switch (choice) {
                case 1: createFile(); break;
                case 2: writeFile(); break;
                case 3: readFile(); break;
                case 4: deleteFile(); break;
                case 5: listFiles(); break;
                case 6: copyFile(); break;
                case 7: moveFile(); break;
                case 8: searchFile(); break;
                case 9: System.out.println("Exiting..."); System.exit(0);
                default: System.out.println("Invalid choice! Try again.");
            }
        }
    }
//Creating methods listed in the options

    //create file method
    private static void createFile() {
        System.out.print("Enter filename to create: ");
        String filename = scanner.nextLine();
        try {
            File file = new File(filename);
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("This File already exists.");
            }
        } catch (IOException e) { //failed to create custom exceptions
            System.out.println("An error occurred while creating the file.");
        }
    }
    //write file method
    private static void writeFile() {
        System.out.print("Enter filename to write: ");
        String filename = scanner.nextLine();
        System.out.print("Enter content to write: ");
        String content = scanner.nextLine();
        try (FileWriter writer = new FileWriter(filename, true)) {
            writer.write(content + "\n");
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
        }
    }
    // readfile method
    private static void readFile() {
        System.out.print("Enter filename to read: ");
        String filename = scanner.nextLine();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            System.out.println("\n--- File Content ---");
            while ((line = reader.readLine()) != null) { //this loop iterates through every line
                System.out.println(line);
            }
            System.out.println("............End of File..........");
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file.");
        }
    }
    //deletefile method
    private static void deleteFile() {
        System.out.print("Enter filename to delete: ");
        String filename = scanner.nextLine();
        File file = new File(filename);
        if (file.delete()) { //theres a delete method in java already
            System.out.println("Deleted the file: " + filename);
        } else {
            System.out.println("Failed to delete the file.");
        }
    }
    //list available files in the particular directory
    private static void listFiles() {
        System.out.print("Enter directory path: ");
        String path = scanner.nextLine();
        File folder = new File(path);
        if (folder.isDirectory()) {
            String[] files = folder.list();
            System.out.println("\n--- Files in Directory ---");
            for (String file : files) {
                System.out.println(file);
            }
        } else {
            System.out.println("Invalid directory path.");
        }
    }
    //copyfile method
    private static void copyFile() {
        System.out.print("Enter source filename: ");
        String source = scanner.nextLine();
        System.out.print("Enter destination filename: ");
        String dest = scanner.nextLine();
        try {
            Files.copy(Paths.get(source), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File copied successfully.");
        } catch (IOException e) {
            System.out.println("Failed to copy file: " + e.getMessage());
        }
    }
        //movefile method
    private static void moveFile() {
        System.out.print("Enter source filename: ");
        String source = scanner.nextLine();
        System.out.print("Enter destination filename: ");
        String dest = scanner.nextLine();
        try {
            Files.move(Paths.get(source), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File moved successfully.");
        } catch (IOException e) {
            System.out.println("Failed to move file: " + e.getMessage());
        }
    }
    //the search method
    private static void searchFile() {
        System.out.print("Enter directory path to search in: ");
        String path = scanner.nextLine();
        System.out.print("Enter filename to search: ");
        String filename = scanner.nextLine();
        File folder = new File(path);
        if (folder.isDirectory()) {
            String[] files = folder.list(); //using an array to search through
            boolean found = false;
            for (String file : files) {
                if (file.equalsIgnoreCase(filename)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                System.out.println("File found in directory.");
            } else {
                System.out.println("File Not found.");
            }
        } else {
            System.out.println("Invalid directory path.");
        }
    }
}//close
