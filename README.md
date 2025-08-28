File-Management-System-In-Java

This is a simple file management system written in Java.  
It is a terminal-based application that provides basic functionality for working with files and directories.  

The program uses Java I/O (`java.io`) and NIO (`java.nio.file`) libraries to perform different file operations, while interacting with the user through a console-based menu.

=========================================================

✨ Features
The file manager currently supports the following operations:

- 📄 Create a File  
- ✏️ Write to a File (append mode supported)  
- 📖 Read a File (line by line output to the console)  
- 🗑️ Delete a File  
- 📂 List Files in a Directory  
- 📑 Copy a File (with overwrite support)  
- 🚚 Move a File (with overwrite support)  
- 🔎 Search for a File within a given directory  

=======================================================

📜 How It Works
- On program start, a menu is displayed with numbered options.  
- The user selects an option (1–9).  
- The program executes the corresponding file operation.  
- Input and output are handled entirely through the console (`Scanner` for input, `System.out.println` for output).  
- Invalid inputs are handled gracefully with error messages.  

Example Menu:
===== File Management System =====

Create a File

Write to a File

Read File

Delete File

List Files in Directory

Copy File

Move File

Search File

Exit



---

🏃 How to Run
1. Clone or download this repository.  
2. Open a terminal and navigate to the project folder.  
3. Compile the code:  
   ```bash
   javac FileManagementSystem.java
