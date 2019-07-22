package SiteMap;

import java.sql.SQLException;
import java.util.*;

public class SiteMapMaker {
    private Set<String> pagesVisited = new HashSet<String>();
    private List<String> pagesToVisit = new LinkedList<String>();

    /**
     * Returns the nextURL to visit, in the order that it was found. It also
     * checks that it doesn't return an already visited URL.
     *
     * @return the nextURL.
     */
    private String nextURL() {
        String nextURL;
        do {
            nextURL = this.pagesToVisit.remove(0);
        } while(this.pagesVisited.contains(nextURL));
        this.pagesVisited.add(nextURL);
        return nextURL;
    }

    private void findPages(String parentURL) throws SQLException {
        pagesToVisit.add(parentURL);
        while (!pagesToVisit.isEmpty()){
            String currentURL;
            SideConnection side = new SideConnection(parentURL);

        }
    }

    public static void main(String[] args){
        Scanner userIn = new Scanner(System.in);
        System.out.println("Enter top level URL: ");

        String parentURL = userIn.nextLine();


    }
}
