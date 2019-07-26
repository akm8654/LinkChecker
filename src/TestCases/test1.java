package TestCases;

import SiteMap.SideConnection;

import java.io.IOException;
import java.sql.SQLException;

public class test1 {

    public static void main(String[] args){
        try {
            SideConnection s = new SideConnection("https://www.rit.edu/academicaffairs/student-success/time-graduation");
            System.out.println("https://www.rit.edu/academicaffairs/student-success/time-graduation");
            String[] link = new String[4];
            link[0] = "https://www.rit.edu/academicaffairs/student-success/time-graduation";
            link[1] = "TEST PAGE2";
            s.submitToPageTable("test", link);
        } catch (SQLException e) {
            System.out.println("Error");
        } catch (IOException se) {
            System.out.println("ERROR 2");
        }
    }
}
