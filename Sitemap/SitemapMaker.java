package Sitemap;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.lang.*;

public class SitemapMaker {
    private Set<String> pagesVisited = new HashSet<String>();
    private List<String> pagesToVisit = new LinkedList<String>();

    private String nextURL() {
        String nextURL;
        do {
            nextURL = this.pagesToVisit.remove(0);
        } while(this.pagesToVisit.contains(nextURL));
        this.pagesVisited.add(nextURL);
        return nextURL;
    }

    public void search(String url){
        String currentURL;
        SideConnection connect = new SideConnection();
        if(this.pagesToVisit.isEmpty()){
            currentURL = url;
            this.pagesVisited.add(url);
        } else {
            currentURL = this.nextURL();
        }
        connect.crawl(currentURL);

        this.pagesToVisit.addAll(connect.getLinks());
    } System.out.println()

}
