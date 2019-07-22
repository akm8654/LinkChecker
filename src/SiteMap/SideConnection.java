package SiteMap;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;


public class SideConnection {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64)" +
            " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

    /**
     * The initial URL is the parent URL
     */
    private String initialURL;
    /**
     * All the links on the page.
     */
    private List<String> links = new LinkedList<String>();
    /**
     * current Webpage
     */
    private Document htmlDocument;
    /**
     * The database where everything is added too.
     */
    public static Database DB = new Database();

    public SideConnection(String initialURL) throws SQLException {
        this.initialURL = initialURL;
        DB.runSql2("TRUNCATE Record;");
    }

    /**
     * Checks the database for the URL, if it is not present, it adds it.
     * Returns false if not present, true if it is. Adapted from online to
     * fit personal needs.
     *
     * @param URL -the URL value to see if it is in the database.
     * @return True if URL is present, False if it isn't and adds it to the
     * database.
     * @throws SQLException - in case of an SQL error.
     */
    public Boolean check(String URL) throws SQLException {
        String sql = "SELECT * from 'Record' WHERE URL = '" + URL + "'";
        ResultSet rs = DB.runSql(sql);
        if (rs.next()) {
            return true;
        } else {
            //store the URL so it is not used again.
            sql =
                    "INSERT INTO 'Crawler'.'Record' " + "('URL' VALUES " +
                            "(?);";
            PreparedStatement stmt = DB.conn.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, URL);
            stmt.execute();

            return false;
        }
    }

    public boolean crawl(String URL){
        try {
            Connection conn = Jsoup.connect((URL));
            conn.userAgent(USER_AGENT);
            Document htmlDoc = conn.get();
            this.htmlDocument = htmlDoc;
            if(conn.response().statusCode() == 200) // 200 is the HTTP OK status
                // code
            {
                System.out.println("\n**Visiting** Received web page at " + URL);
            }
            if(!conn.response().contentType().contains("text/html"))
            {
                System.out.println("**Failure** Retrieved something other than HTML");
                return false;
            }
            Elements linksOnPage = htmlDocument.select("a[href]");
            System.out.println("Found (" + linksOnPage.size() + ") links");
            for(Element link : linksOnPage)
            {
                this.links.add(link.absUrl("href"));
            }
            return true;
        }
        catch(IOException ioe)
        {
            // We were not successful in our HTTP request
            return false;
        }
    }

    public List<String> getLinks() {
        return this.links;
    }


}
