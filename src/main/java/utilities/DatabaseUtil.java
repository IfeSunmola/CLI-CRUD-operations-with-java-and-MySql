package utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

import static utilities.UserInputUtil.*;
import static utilities.ValidateUtil.sendVerificationCode;

/**
 * This class contains all the functions related to MySql operations<br>All the related exceptions will be caught in
 * the Main class
 *
 * @author Ife Sunmola
 */
public final class DatabaseUtil {
    private static final int MAX_ELAPSED_MINUTES = 720;
    // column names
    private static final String PHONE_NUMBER = "phone_number";
    private static final String USER_NAME = "user_name";
    private static final String DATE_OF_BIRTH = "date_of_birth";
    private static final String Age = "age";
    private static final String GENDER = "gender";
    private static final String DATE_OF_REG = "date_of_reg";
    private static final String TIME_OF_REG = "time_of_reg";
    private static final String LAST_LOGIN_DATE = "last_login_time"; // change to last login date
    private static final int NUM_COLUMNS = 8;

    /**
     * Method to connect to the database. The data needed (driver, url, etc.) is saved in environment variables.
     *
     * @return Connection object a connection was formed OR null if a connection could not be formed.
     * @throws SQLException           if there was a problem with the sql server itself
     * @throws ClassNotFoundException if the jdbc class could not be found
     */
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Connection connection; //null will be returned if it could not be connected

        String driver = System.getenv("SQL_DRIVER");
        String url = System.getenv("SQL_URL");
        String username = System.getenv("SQL_USERNAME");
        String password = System.getenv("SQL_PASSWORD");

        Class.forName(driver);
        connection = DriverManager.getConnection(url, username, password);
        return connection;
    }

    //methods to create tables

    /**
     * Method to create a table for the users using the database connection.
     * todo: make this private
     *
     * @param connection the connection to the database
     * @throws SQLException if there was a problem with the sql server itself.
     */
    public static void createUsersTable(Connection connection) throws SQLException {
        // if users_table does not exist in the database, create it
        PreparedStatement create = connection.prepareStatement(
                """
                        CREATE TABLE IF NOT EXISTS users_table(
                        phone_number VARCHAR(10) PRIMARY KEY UNIQUE NOT NULL,
                        user_name VARCHAR(10) NOT NULL,
                        date_of_birth DATE NOT NULL,
                        age INT NOT NULL,
                        gender VARCHAR(10) NOT NULL,
                        date_of_reg Date NOT NULL,
                        time_of_reg TIME NOT NULL,
                        last_login_time DATETIME DEFAULT '2000-11-24 01:01:01'
                        );""");
        create.executeUpdate();
//    set the last login time to an old date as the default value so the user won't get logged in automatically if
//    they haven't logged in before
    }

    // methods to log the user in
    private static LocalDateTime getLastLoginTime(Connection connection, String userPhoneNumber) throws SQLException {
        PreparedStatement getLastLoginTime = connection.prepareStatement(
                "SELECT last_login_time FROM users_table WHERE phone_number= '" + userPhoneNumber + "';");
        ResultSet result = getLastLoginTime.executeQuery();
        LocalDateTime lastTime = null;
        if (result.next()) {
            String temp = result.getString("last_login_time");
            lastTime = LocalDateTime.parse(temp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return lastTime;
    }

    private static boolean sessionTimedOut(Connection connection, String userPhoneNumber) throws SQLException {
        // returns true if the session has timed out and false if not
        // session has timed out if 720 minutes (12 hours)  has passed since the last login time
        LocalDateTime lastLoginTime = getLastLoginTime(connection, userPhoneNumber);
        long elapsed = ChronoUnit.MINUTES.between(lastLoginTime, LocalDateTime.now());
        return elapsed >= MAX_ELAPSED_MINUTES;
    }

    private static void doLogin(BufferedReader inputReader, Connection connection, String userPhoneNumber) throws SQLException, IOException {
        System.out.println("Your session has timed out, log in again");
        String code = sendVerificationCode(userPhoneNumber); //returns the verification code that was sent

        System.out.print("Enter the verification code that was sent: ");
        String userCode = inputReader.readLine().strip();

        if (userCode.equals(code)) {
            System.out.println("Account found, Log in successful");
            PreparedStatement setLastLoginTime = connection.prepareStatement(
                    "UPDATE users_table SET last_login_time= '" + getCurrentDate() + " " + getCurrentTime() +
                            "' WHERE phone_number='" + userPhoneNumber + "'");
            setLastLoginTime.executeUpdate();
            Menu.doLoginMenu(userPhoneNumber);
        }
        else {
            System.out.println("Wrong code. Log in failed.");
            Menu.doMainMenu();
        }
    }

    /**
     * Method to allow an existing user to log in to their account. A verification code will be sent to the user's
     * phone number with twilio. todo: log in with github, login with password, don't let user keep logging in everytime
     *
     * @param inputReader to get the user input
     * @param connection  the connection to the database
     * @throws IOException  if the user input could not be read
     * @throws SQLException if there was a problem with the sql server itself
     */
    public static void login(BufferedReader inputReader, Connection connection) throws IOException, SQLException {
        System.out.println("** Login to an existing account **");
        String userPhoneNumber = getPhoneNumber(inputReader);// ask for the user's phone number to log in

        if (numberExistsInDB(userPhoneNumber, connection)) { // user has an account
            if (sessionTimedOut(connection, userPhoneNumber)) { // session has timed out
                doLogin(inputReader, connection, userPhoneNumber);// log the user in
            }
            else {// session has NOT timed out
                System.out.println("Still in session, no need to log in.");
                Menu.doLoginMenu(userPhoneNumber);
            }
        }
        else { // user does not have an account
            System.out.println("Account not found. Log in failed");
            Menu.doMainMenu();
        }
    }

    // methods to create an account

    /**
     * Method to create an account for a user. The account will not be created if an account has already been
     * created with the same phone number.<br> The age will be calculated in the database, rather than directly in the
     * code
     *
     * @param inputReader to get the user input
     * @param connection  the connection to the database
     * @throws IOException  if the user input could not be read
     * @throws SQLException if there was a problem with the sql server itself
     */
    public static void createAccount(BufferedReader inputReader, Connection connection) throws IOException, SQLException {
        System.out.println("** Creating an account **");
        String name = getName(inputReader);
        System.out.println("--------------");
        String dateOfBirth = getDateOfBirth(inputReader);
        System.out.println("--------------");
        String phoneNumber = getPhoneNumber(inputReader);
        String gender = getGenderIdentity(inputReader);

        if (!numberExistsInDB(phoneNumber, connection)) {// the user does not have an account, create one
            PreparedStatement addUser = connection.prepareStatement(
                    "INSERT INTO users_table " +
                            "(phone_number, user_name, date_of_birth, age, gender, date_of_reg, time_of_reg) " +
                            "VALUES('" + phoneNumber + "', '" + name + "', '" + dateOfBirth + "', TIMESTAMPDIFF(YEAR, date_of_birth, CURDATE()), " +
                            "'" + gender + "', '" + getCurrentDate() + "', '" + getCurrentTime() + "');");
            // executeUpdate returns the amount of rows that was updated
            if (addUser.executeUpdate() == 1) {// the account was created if the number of rows updated is 1
                System.out.println("------------------------------------------");
                System.out.println("Account created Successfully");
                System.out.println("Log in to your account");
                System.out.println("------------------------------------------");
            }
            else {// failed
                // shouldn't happen but just in case
                System.err.println("Account could not be created (executeUpdate returned number != 1)");
            }
        }
        else {
            System.out.println("You already have an account. Log in instead.");
        }
    }

    // methods to delete an account

    /**
     * Method to allow the user to delete their account.
     *
     * @param inputReader to get the user input
     * @param connection  the connection to the database
     * @throws IOException  if the user input could not be read
     * @throws SQLException if there was a problem with the sql server itself
     */
    public static void deleteAccount(BufferedReader inputReader, Connection connection) throws IOException, SQLException {
        System.out.println("** Deleting an account **");
        String phoneNumber = getPhoneNumber(inputReader);// get account to delete

        if (numberExistsInDB(phoneNumber, connection)) {// there is an account to delete
            String userInput = "";
            while (!userInput.equals("Y") && !userInput.equals("N")) {// confirm if the user wants to delete their account
                System.out.println("YOUR ACCOUNT CANNOT BE RECOVERED AFTER DELETION");
                System.out.print("Are you sure you want to delete your account? This process is IRREVERSIBLE (y/n): ");
                userInput = inputReader.readLine().toUpperCase().strip();
            }
            if (userInput.equals("Y")) {// delete the account if confirmed
                PreparedStatement deleteUser = connection.prepareStatement(
                        "DELETE FROM users_table WHERE phone_number = '" + phoneNumber + "';");
                // executeUpdate returns the amount of rows that was updated
                if (deleteUser.executeUpdate() == 1) {// the account was deleted if the number of rows updated is 1
                    System.out.println("Account deleted successfully");
                }
                else {
                    // shouldn't happen but just in case
                    System.err.println("Account could not be deleted (executeUpdate returned number != 1)");
                }
            }
            else {// not confirmed, don't delete the account
                System.out.println("Account not deleted");
            }
        }
        else {// there is no account to delete
            System.out.println("Account not found. Delete failed");
        }
    }

    // method to show the profile
    public static void showProfile(Connection connection, String userPhoneNumber) throws SQLException, IOException {
        HashMap<String, String> result = getUserDetails(connection, userPhoneNumber);

        System.out.println("** Showing profile for " + result.get(USER_NAME) + ": **");
        System.out.println("Phone number: " + result.get(PHONE_NUMBER));
        System.out.println("Date of birth (Age): " + result.get(DATE_OF_BIRTH) + " (" + result.get(Age) + ")");
        System.out.println("Gender: " + result.get(GENDER));
        System.out.println("Date registered: " + formatDateAndTime(result.get(DATE_OF_REG) + " " + result.get(TIME_OF_REG)));
        Menu.doLoginMenu(userPhoneNumber);
    }

    private static HashMap<String, String> getUserDetails(Connection connection, String userPhoneNumber) throws SQLException {
        PreparedStatement getUserDetails = connection.prepareStatement(
                "SELECT * FROM users_table where phone_number = '" + userPhoneNumber + "'");
        ResultSet resultSet = getUserDetails.executeQuery();
        HashMap<String, String> userDetails = new HashMap<>();
        while (resultSet.next()) {
            for (int i = 1; i < NUM_COLUMNS; i++) {
                userDetails.put(resultSet.getMetaData().getColumnName(i), resultSet.getString(i));
            }
        }
        return userDetails;
    }

    private static String formatDateAndTime(String dateAndTimeOfReg) {
        DateTimeFormatter in = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime date = LocalDateTime.parse(dateAndTimeOfReg, in);// convert the input to a DateTime
        DateTimeFormatter out = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a"); // convert the DateTime to the needed to be output
        return date.format(out);
    }
    //misc

    /**
     * Method to check if a phone number exists in the database
     *
     * @param userPhoneNumber the phone number to check
     * @param connection      the connection to the database
     * @return true if the phone number is already in the database OR false if it's not
     * @throws SQLException if there was a problem with the sql server itself
     */
    private static boolean numberExistsInDB(String userPhoneNumber, Connection connection) throws SQLException {
        PreparedStatement getPhoneNumber = connection.prepareStatement(
                //get the user's phone number from db.
                "SELECT phone_number FROM users_table WHERE phone_number= '" + userPhoneNumber + "';");
        ResultSet result = getPhoneNumber.executeQuery();// store the result
        String numberInDb = "";
        if (result.next()) {//.next returns true if there is a data in the ResultSet.
            numberInDb = result.getString("phone_number");// phone_number is the column name
        }
        return numberInDb.equals(userPhoneNumber);
    }

}