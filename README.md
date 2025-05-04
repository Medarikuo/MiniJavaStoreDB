# MiniJavaStore

A simple Java program that connects to a MySQL database to simulate a mini store system.

## 📁 Files Included

- `MiniJavaStoreDB.java`: Main Java file containing database connection logic.
- `mysql-connector-j-9.3.0.jar`: MySQL JDBC driver required to run the Java program.

## 💻 Requirements

- Java Development Kit (JDK)
- MySQL Server
- MySQL Connector/J (`mysql-connector-j-9.3.0.jar`)
- Windows Command Prompt (CMD)
- Notepad++ or any text editor

## 🛠️ How to Compile and Run
1. Open CMD and navigate to your project folder:
   cd path/to/MiniJavaStore
   For example, if your project is in `C:\Users\Juan\MiniJavaStore`, type:
   cd C:\Users\Juan\MiniJavaStore
3. Compile the Java file:
   javac -cp .;mysql-connector-j-9.3.0.jar MiniJavaStoreDB.java
   note:
   -cp stands for "classpath".
   The . means the current folder.
   ; is used to separate the paths (on Windows).
   This lets your program use the MySQL connector during compilation.
5. Run the program:
   java -cp .;mysql-connector-j-9.3.0.jar MiniJavaStoreDB
   note:
   Same as before: this includes the connector JAR so your program can connect to the database at runtime.
Notes:
Ensure MYSQL is running

     
