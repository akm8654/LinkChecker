package SiteMap;

import java.sql.SQLException;
import java.util.*;

public class SiteMapMaker {
    /**
     * Turned on if standard output debug message is desired.
     */
    private static final boolean DEBUG = true;
    private Set<String> pagesVisited = new HashSet<String>();
    private List<String> pagesToVisit = new LinkedList<String>();


    /**
     * Print method that does something only if debug is true.
     *
     * @param logMsg the message to log
     */
    private static void dPrint(Object logMsg) {
        if (DEBUG){
            System.out.println(logMsg);
        }
    }
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

    private Boolean isParent(String urlToCheck, String parentURL){
        for(int i = 0 ; i < parentURL.length(); i++){
            if (!(urlToCheck.charAt(i) == parentURL.charAt(i))){
                return false;
            }
        }
        return true;
    }

    private void findPages(String parentURL) throws SQLException {
        pagesToVisit.add(parentURL);
        while (!pagesToVisit.isEmpty()){
            String currentURL;
            currentURL = pagesToVisit.remove(0);
            if (!pagesToVisit.contains(currentURL)) {
                dPrint("Checking: " + currentURL);
                SideConnection side = new SideConnection(currentURL);
                if (!side.check(currentURL)) {
                    side.crawl(currentURL);
                    this.pagesVisited.add(currentURL);
                    for (String page : side.getLinks()) {
                        if (isParent(page, parentURL)) {
                            pagesToVisit.add(page);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args){
        Scanner userIn = new Scanner(System.in);
        System.out.println("Enter top level URL: ");

        String parentURL = userIn.nextLine();


    }
}
