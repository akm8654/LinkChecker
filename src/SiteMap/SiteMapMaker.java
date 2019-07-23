package SiteMap;

import java.sql.SQLException;
import java.util.Scanner;

public class SiteMapMaker {
    /**
     * Turned on if standard output debug message is desired.
     */
    private static final boolean DEBUG = true;


    /**
     * Print method that does something only if debug is true.
     *
     * @param logMsg the message to log
     */
    private static void dPrint(Object logMsg) {
        if (DEBUG) {
            System.out.println(logMsg);
        }
    }


    public static void main(String[] args) {
        Scanner userIn = new Scanner(System.in);
        //System.out.println("Enter top level URL: ");

        //String parentURL = userIn.nextLine();
        String parentURL = "https://www.rit.edu/academicaffairs/memos";
        try {
            SideConnection side = new SideConnection(parentURL);
            side.findPages();
        } catch (SQLException e) {
            System.out.println("SQL EXCEPTION FOUND! TERMINATING PROGRAM!");
            System.out.println("Error: " + e.getErrorCode());
            System.out.print("STACK TRACE: ");
            e.printStackTrace();
        }
    }
}
