package TestCases;

import SiteMap.SideConnection;

import java.io.IOException;
import java.sql.SQLException;

public class test1 {

    public static void main(String[] args){
        try {
            SideConnection s = new SideConnection("https://www.rit.edu/academicaffairs/memos/2379");
            System.out.println(("https://www.rit.edu/academicaffairs/memos" +
                    "/2379").hashCode());
            System.out.println(s.presentInChecked("https://www.rit.edu/academicaffairs/memos" +
                    "/2379"));
        } catch (SQLException e) {
            System.out.println("Error");
        } catch (IOException se) {
            System.out.println("ERROR 2");
        }
    }
}
