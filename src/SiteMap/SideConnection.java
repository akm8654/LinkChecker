package SiteMap;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class SideConnection {
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

    /**
     * Turned on if standard output debug message is desired.
     */
    private static final boolean DEBUG = true;
    /**
     * The User agent used to connect to the web as if it was a person and
     * not a robot.
     */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64)" +
            " AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
    /**
     * The initial URL is the parent URL
     */
    private String initialURL;
    /**
     * All the links on the page.
     */
    private List<String[]> links = new LinkedList<String[]>();

    /**
     * current Webpage
     */
    private Document htmlDocument;
    /**
     * Current PageTitle
     */
    private String pageTitle;
    /**
     * The database where everything is added too.
     */
    public static Database DB;
    /**
     * Holds all the pages that need to be visited.
     */
    private Set<String[]> pagestoVisit = new HashSet<>();
    private List<String[]> pagestoVisitQueue = new LinkedList<>();

    /**
     * Constructor for the side connection.
     *
     * @param initialURL The ultimate 'parent' URL
     * @throws SQLException if there is an issue in the sql code.
     */
    SideConnection(String initialURL) throws SQLException, IOException {
        this.initialURL = initialURL;
        Connection conn = Jsoup.connect(initialURL);
        conn.userAgent(USER_AGENT);
        Document htmlDoc = conn.get();
        dPrint("Visiting Initial Page: " + initialURL);
        this.htmlDocument = htmlDoc;
        setTitle();
        this.DB = new Database();
        DB.runSql2("TRUNCATE Record;");
    }

    /**
     * Used to convert the given name to the value that can be stored in an
     * SQL query.
     *
     * @param original - the initial string
     * @return the fixed string
     */
    private String fixName(String original) {
        String[] names = original.split("\\|");
        original = names[0];
        original = original.replaceAll("'", "");
        original = original.replaceAll(" ", "");
        original = original.toLowerCase();
        return original;
    }

    /**
     * Called when the stored object title needs to be replaced with one that
     * can be changed to an SQL formatted one. EXAMPLE: "Provost's Memos" ->
     * "provostsmemos"
     */
    private void setTitle() {
        this.pageTitle = this.htmlDocument.title();
        String[] titles = this.pageTitle.split("\\|");
        this.pageTitle = titles[0];
        this.pageTitle = this.pageTitle.replaceAll("'", "");
        this.pageTitle = this.pageTitle.replaceAll(" ", "");
        this.pageTitle = this.pageTitle.toLowerCase();
        dPrint(pageTitle);
    }

    /**
     * Hash the URL, making it an int
     * Not great yet. Will work on it.
     *
     * @param URL - the URL that needs an Id assigned.
     * @return int for value into the storage table.
     */
    private int makeID(String URL) {
        return (URL.hashCode());
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
    private Boolean check(String URL, String text) throws SQLException {
        String sql =
                "SELECT * FROM `record` WHERE `RecordID`='" + makeID(URL) +
                        "';";
        ResultSet rs = DB.runSql(sql);
        if (rs.next()) {
            return true;
        } else {
            //store the URL so it is not used again.
            sql = "INSERT INTO `record` (`RecordID`, `URL`, `Page Title`) " +
                    "VALUES ('" + makeID(URL) + "', '" + URL + "', '" + text + "');";
            Statement stmt = DB.conn.createStatement();
            stmt.executeUpdate(sql);
            return false;
        }
    }

    /**
     * Determines if the page has been visited yet or not.
     *
     * @param URL the URL to be checked
     * @return whether or not it is present in the table
     * @throws SQLException in case of an SQL Error
     */
    private Boolean presentInChecked(String URL) throws SQLException {
        String sql =
                "SELECT * FROM `record` WHERE `RecordID`='" + makeID(URL) +
                        "';";
        ResultSet rs = DB.runSql(sql);
        dPrint("Checking if " + URL + " has been visited");
        if (rs.next()) {
            dPrint("Returning True");
            return true;
        } else {
            dPrint("Returning False");
            return false;
        }
    }

    /**
     * Creates a page for the website, with columns, RecordID, URL,
     * parentLink as a boolean, if it is a parent then the value is true,
     * else false.
     *
     * @param URL  the url for the page
     * @param text the text for the page
     * @throws SQLException in case SQL doesn't work.
     */
    private void createPageTable(String URL, String text, String parentURL,
                                 String parentTxt) throws SQLException {
        String sql = "CREATE TABLE `individual names`.`" + text + "` ( `PageTitle` " +
                "TEXT NOT NULL , `RecordID` INT NOT NULL , `URL` TEXT NOT NULL ," +
                " `ParentLink` BOOLEAN NOT NULL DEFAULT FALSE ) ENGINE = " +
                "MyISAM;";
        Statement stmt = DB.conn.createStatement();
        stmt.executeUpdate(sql);

        sql = "INSERT INTO `individual names`.`" + text + "` (`RecordID`, `URL`, `PageTitle`, " +
                "`ParentLink`) " +
                "VALUES ('" + makeID(parentURL) + "', '" + parentURL + "', '" + parentTxt +
                "', '1');";
        stmt.executeUpdate(sql);
    }


    /**
     * Determines if the URL is a 'parent URL' of the code.
     *
     * @param urlToCheck the url that is being checked
     * @param parentURL  the original code
     * @return whether it is a parent or not.
     */
    private Boolean isParent(String urlToCheck, String parentURL) {
        if (urlToCheck.length() < parentURL.length()) {
            return false;
        }
        for (int i = 0; i < parentURL.length(); i++) {
            if ((urlToCheck.charAt(i) != parentURL.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Starts the crawl and recursively looks through websites.
     *
     * @throws SQLException - sometimes things go wrong.
     */
    void beginCrawl() throws SQLException {
        String[] initialURLArray = new String[4];
        initialURLArray[0] = this.initialURL;
        initialURLArray[1] = this.pageTitle;
        initialURLArray[2] = this.initialURL;
        initialURLArray[3] = this.pageTitle;
        this.pagestoVisit.add(initialURLArray);
        this.pagestoVisitQueue.add(initialURLArray);
        while (pagestoVisit.size() != 0) {
            String[] currentURL;
            currentURL = this.pagestoVisitQueue.remove(0);
            this.pagestoVisit.remove(currentURL);
            if (isParent(currentURL[0], initialURL)) {
                dPrint("Checking: " + currentURL[0]);
                visitPage(currentURL);
            } else {
                dPrint("URL is not a parent, checking the next one.");
            }
        }
    }

    /**
     * Determines if the table exists or not.
     *
     * @param text the table name
     * @return true if exists, false if not.
     * @throws SQLException if there is an SQL Error.
     */
    Boolean checkPageTable(String text) throws SQLException {
        text = text.toUpperCase();
        dPrint("Checking for the table " + text);
        DatabaseMetaData md = DB.conn.getMetaData();
        ResultSet rs = md.getTables("INDIVIDUAL NAMES", null, text, null);
        if (rs.next()) {
            dPrint("Table Found");
            return true;
        } else {
            dPrint("Table Not Found");
            return false;
        }
    }

    /**
     * submits the links to the original table.
     *
     * @param tableName
     * @param linkToSubmit
     * @throws SQLException
     */
    private void submitToPageTable(String tableName, String[] linkToSubmit)
            throws SQLException {
        String sql;
        linkToSubmit[1] = fixName(linkToSubmit[1]);
        String URL = linkToSubmit[0];
        String text = linkToSubmit[1];

        Statement stmt = DB.conn.createStatement();

        dPrint("submitting to page " + tableName);
        sql = "INSERT INTO `individual names`.`" + tableName + "` (`PageTitle`, " +
                "`RecordID`, " + "`URL`, " + "`ParentLink`) VALUES ('" + text + "', '"
                + makeID(URL) + "', '" + URL + "'," + " '0');";
        dPrint(sql);
        stmt.executeUpdate(sql);
    }

    /**
     * Adds the given link as a parent to the page's table. If not present
     * then it prints it.
     *
     * @param tableName  the name of the table to update
     * @param updateLink the string of values to change
     * @throws SQLException If there is an SQL Error.
     */
    private void addParent(String tableName, String[] updateLink) throws SQLException {
        String parentURL = updateLink[2];
        String parentTxt = updateLink[3];
        parentTxt = fixName(parentTxt);
        int parentID = makeID(parentURL);

        String sql =
                "SELECT * FROM `individual names`.`" + tableName + "` WHERE " +
                        "`RecordID`='" + parentID +
                        "';";
        ResultSet rs = DB.runSql(sql);

        if (!rs.next()) {
            //insert the URL into the table.
            sql = "INSERT INTO `individual names`.`" + tableName + "` (`RecordID" +
                    "`, `URL`, `PageTitle`, " +
                    "`ParentLink`) " +
                    "VALUES ('" + parentID + "', '" + parentURL + "', '"
                    + parentTxt + "', '1');";
            Statement stmt = DB.conn.createStatement();
            stmt.executeUpdate(sql);
        } else {
            sql = "SELECT * FROM `individual names`.`" + tableName + "` WHERE " +
                    "`RecordID`='" + parentID +
                    "' AND `ParentLink`='1';";
            rs = DB.runSql(sql);
            if (!rs.next()) {
                sql = "UPDATE `individual names`.`" + tableName + "` SET `ParentLink" +
                        "`='1' WHERE `RecordID`='" + parentID + "'";
                Statement stmt = DB.conn.createStatement();
                stmt.executeUpdate(sql);
            }
        }
    }

    /**
     * Adds the specified link to the broken table.
     *
     * @param linkToAdd the link to add.
     * @throws SQLException If there is an SQL Error
     */
    private void addBrokenLink(String[] linkToAdd) throws SQLException {
        Statement stmt = DB.conn.createStatement();
        String URL = linkToAdd[0];
        String text = linkToAdd[1];
        text = fixName(text);
        String parentURL = linkToAdd[2];
        String sql = "INSERT INTO `broken` (`RecordID`, `Page Title`, `URL`, " +
                "`On Page URL`) VALUES " +
                "('" + makeID(URL) + "', '" + text + "', '" + URL + "', '" +
                parentURL + "')";
        stmt.executeUpdate(sql);
    }

    /**
     * The code that visits the page requested, determining if it needs to
     * access the table and more.
     *
     * @param currentlink - the link to visit.
     */
    void visitPage(String[] currentlink) {
        try {
            String URL = currentlink[0];
            String text = currentlink[1];
            String parentURL = currentlink[2];
            String parentText = currentlink[3];

            Connection conn = Jsoup.connect(currentlink[0]);
            conn.userAgent(USER_AGENT);
            Document htmlDoc = conn.get();
            dPrint("Visiting Page: " + URL);
            this.htmlDocument = htmlDoc;
            setTitle();

            if (conn.response().statusCode() == 200) {
                dPrint("Web Page Received, checking table " + this.pageTitle);
                if (!checkPageTable(this.pageTitle)) {
                    dPrint("Creating page table, and looking for links");
                    createPageTable(URL, this.pageTitle, parentURL, parentText);
                } else {
                    dPrint("TABLE ALREADY CREATED");
                    addParent(this.pageTitle, currentlink);
                }
                Elements linksOnPage = this.htmlDocument.select("a[href]");
                dPrint("Found (" + linksOnPage.size() + ") on page.");
                for (Element link : linksOnPage) {
                    String[] checkedLink = new String[4];
                    checkedLink[0] = link.absUrl("href");
                    checkedLink[1] = link.text();
                    checkedLink[2] = URL;
                    checkedLink[3] = text;
                    if (isParent(checkedLink[0], initialURL)) {
                        if (!presentInChecked(checkedLink[0])) {
                            this.pagestoVisit.add(checkedLink);
                            dPrint("ADDING TO pagesToVisit");
                        }
                        this.submitToPageTable(text, checkedLink);
                    }
                }
                this.check(URL, text);
                this.pagestoVisitQueue.addAll(pagestoVisit);
            } else {
                this.addBrokenLink(currentlink);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
        }
    }
}
