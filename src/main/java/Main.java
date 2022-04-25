import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import static utilities.DatabaseUtil.*;
import static utilities.UserInputUtil.*;

/*
 * Todo:
 *  create account/log in with github
 *  change gender to show all options
 *  move createUsersTable to private class in DatabaseUtil
 *  login by password
 *  name should be in format Firstname
 *  change api keys to store in db
 *  name should not have spaces, clean up user input
 * */

/**
 * Main/Driver class for local server
 *
 * @author Ife Sunmola
 */
public class Main {
    public static void main(String[] args) {
        System.out.println(mainMenu());
        // no need to close inputReader and connection since the try is used like this
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in)); Connection connection = getConnection()) {
//            if (connection == null) {
//                System.err.println("Connection failed.");
//                return;
//            }
//            createUsersTable(connection);
//            boolean selectionWasValid = false;
//            String userInput;
//
//            while (!selectionWasValid) {
//                System.out.print("Your response: ");
//                userInput = inputReader.readLine().strip();
//                switch (userInput) {
//                    case "1" -> {
//                        createAccount(inputReader, connection);
//                        selectionWasValid = true;
//                    }
//                    case "2" -> {
//                        login(inputReader, connection);
//                        selectionWasValid = true;
//                    }
//                    case "3" -> {
//                        deleteAccount(inputReader, connection);
//                        selectionWasValid = true;
//                    }
//                    case "0" -> {
//                        System.out.println("Have a nice day");
//                        selectionWasValid = true;
//                    }
//                    default -> System.out.println("Make a valid selection");
//                }
//            }
        }
        catch (IOException | SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}
