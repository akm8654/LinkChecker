package SiteMap;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.PreparedStatement;
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

    private int start;
    /**
     * Holds all the pages that need to be visited.
     */
    private Set<String> pagestoVisit = new HashSet<>();

    private List<String[]> pagestoVisitQueue = new LinkedList<>();

    /**
     * Constructor for the side connection.
     *
     * @param initialURL The ultimate 'parent' URL
     * @throws SQLException if there is an issue in the sql code.
     */
    public SideConnection(String initialURL) throws SQLException, IOException {
        this.initialURL = initialURL;
        Connection conn = Jsoup.connect(initialURL);
        conn.userAgent(USER_AGENT);
        Document htmlDoc = conn.get();
        dPrint("Visiting Initial Page: " + initialURL);
        this.htmlDocument = htmlDoc;
        setTitle();
        this.DB = new Database();
        //Use if you want to reset the values.
        //DB.runSql2("TRUNCATE Record;");
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
        names = original.split(":");
        original = names[0];
        original = original.replaceAll("'", "");
        original = original.replaceAll(" ", "");
        original = original.toLowerCase();
        if (original.length() > 64) {
            original = original.substring(0, 63);
        }
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
        if (this.pageTitle.length() > 64) {
            this.pageTitle = this.pageTitle.substring(0, 63);
        }
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
                "SELECT * FROM `crawler`.`record` WHERE `RecordID`='" + makeID(URL) +
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
    public Boolean presentInChecked(String URL) throws SQLException {
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
        text = fixName(text);
        dPrint("Creating table named " + text);

        String sql = "CREATE TABLE `individual names`.`" + text + "` ( `PageTitle` " +
                "TEXT NOT NULL , `RecordID` INT NOT NULL, FOREIGN KEY " +
                "(`RecordID`) " +
                "REFERENCES `crawler`.`record`(`RecordID`)  , `URL` TEXT NOT " +
                "NULL, `ParentLink` " +
                "BOOLEAN NOT NULL DEFAULT FALSE ) ENGINE = " +
                "MyISAM;";
        Statement stmt = DB.conn.createStatement();
        stmt.executeUpdate(sql);

        sql = "INSERT INTO `individual names`.`" + text + "` (`RecordID`, `URL`, `PageTitle`, " +
                "`ParentLink`) " +
                "VALUES ('" + makeID(parentURL) + "', '" + parentURL + "', '" + parentTxt +
                "', '1');";
        stmt.executeUpdate(sql);

        sql = "INSERT INTO `crawler`.`pagetables` (`tableName`, `tableURL`, " +
                "`RecordID`)" +
                " " +
                "VALUE ('" + text + "', '" + URL + "', '" + makeID(URL) + "');";
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
     * Initializes the Visit Queue for use
     *
     * @return the position number that the queue starts at
     * @throws SQLException SOMETHING WENT WRONG.
     */
    private int getQueueStart() throws SQLException {
        String sql =
                "SELECT * FROM `crawler`.`visitqueue` WHERE `POS`=(SELECT " +
                        "MIN(`POS`) FROM `crawler`.`visitqueue`);";
        ResultSet rs = DB.runSql(sql);
        if (rs.next()) {
            return rs.getInt("POS");
        } else {
            return 1;
        }
    }

    /**
     * Takes the queue from the database and copies it to the memory.
     *
     * @return the queue
     */
    private List<String[]> initializeQueue() throws SQLException {
        List<String[]> currentQueue = new LinkedList<>();
        String sql = "SELECT * FROM `crawler`.`visitqueue`";
        ResultSet rs = DB.runSql(sql);
        String[] link = new String[4];
        while (rs.next()) {
            link[0] = rs.getString("URL");
            link[1] = rs.getString("text");
            link[2] = rs.getString("parentURL");
            link[3] = rs.getString("parentText");
            currentQueue.add(link);
            this.pagestoVisit.add(link[0]);
        }
        return currentQueue;
    }

    /**
     * Starts the crawl and recursively looks through websites.
     *
     * @throws SQLException - sometimes things go wrong.
     */
    void beginCrawl() {
        try {
            this.start = getQueueStart();
            this.pagestoVisitQueue = initializeQueue();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        String[] initialURLArray = new String[4];
        initialURLArray[0] = this.initialURL;
        initialURLArray[1] = this.pageTitle;
        initialURLArray[2] = this.initialURL;
        initialURLArray[3] = this.pageTitle;
        if (!this.pagestoVisit.contains(initialURL)) {
            this.pagestoVisit.add(initialURLArray[2]);
            this.pagestoVisitQueue.add(initialURLArray);
        }
        while (pagestoVisitQueue.size() != 0) {
            String[] currentURL;
            currentURL = this.pagestoVisitQueue.remove(0);
            this.pagestoVisit.remove(currentURL);
            try {
                removeFromQueue(this.start);
                this.start++;
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            }
            dPrint(pagestoVisitQueue.size());
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
     * @param URL  the url of the top link
     * @return true if exists, false if not.
     * @throws SQLException if there is an SQL Error.
     */
    int checkPageTable(String text, String URL) throws SQLException {
        text = text.toUpperCase();
        dPrint("Checking for the table " + text);
        String sql = "SELECT * FROM `crawler`.`pagetables` WHERE " +
                "`tablename`='" + text + "';";
        ResultSet rs = DB.runSql(sql);
        boolean looped = false;
        while (rs.next()) {
            looped = true;
            dPrint(URL);
            if (URL.equals(rs.getString("tableURL"))) {
                dPrint("EXIST: YES");
                return 1;
            }
        }
        if (!looped) {
            dPrint("EXIST: NO");
            return 0;
        } else {
            dPrint("EXIST: YES_DIF");
            return 2;
        }
    }

    /**
     * submits the links to the original table.
     *
     * @param tableName
     * @param linkToSubmit
     * @throws SQLException
     */
    public void submitToPageTable(String tableName, String[] linkToSubmit)
            throws SQLException {
        String sql;
        linkToSubmit[1] = fixName(linkToSubmit[1]);
        String URL = linkToSubmit[0];
        String text = linkToSubmit[1];

        Statement stmt = DB.conn.createStatement();
        sql =
                "SELECT * FROM `individual names`.`" + tableName + "` WHERE " +
                        "`RecordID`='" + makeID(URL) +
                        "';";
        ResultSet rs = DB.runSql(sql);
        if (rs.next()) {
        } else {
            dPrint("submitting to page " + tableName);
            sql = "INSERT INTO `individual names`.`" + tableName + "` (`PageTitle`, " +
                    "`RecordID`, " + "`URL`, " + "`ParentLink`) VALUES ('" + text + "', '"
                    + makeID(URL) + "', '" + URL + "'," + " '0');";
            stmt.executeUpdate(sql);
        }
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
    private void addBrokenLink(String[] linkToAdd, int ErrorCode) throws SQLException {
        Statement stmt = DB.conn.createStatement();
        String URL = linkToAdd[0];
        String sql = "INSERT INTO `brokenlinks` (`RecordID`, `URL`, " +
                "`Error`) VALUES " +
                "('" + makeID(URL) + "', '" + URL + "', '" + ErrorCode + "')";
        stmt.executeUpdate(sql);
    }

    /**
     * Checks if the given file is a file extension or not.
     *
     * @param fileExt the file extension
     * @return whether it is or not.
     */
    private boolean docCheck(String fileExt) {
        if (fileExt.contains(".")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds the link to the queue if not already there.
     *
     * @param link the link to add
     * @param i    the number that we are on
     * @throws SQLException in case of an SQLException
     */
    private void addToQueue(String[] link, int i) throws SQLException {
        String URL = link[0];
        String text = link[1];
        String parentURL = link[2];
        String parentText = link[3];

        String sql = "INSERT INTO `crawler`.`visitqueue`(`POS`, `URL`, " +
                "`text`, `parentURL`, `parentText`) VALUES (?, ?, ?, " +
                "?, ?);";
        PreparedStatement stmt = DB.conn.prepareStatement(sql);
        stmt.setInt(1, i);
        stmt.setString(2, URL);
        stmt.setString(3, text);
        stmt.setString(4, parentURL);
        stmt.setString(5, parentText);
        stmt.executeUpdate();
    }

    private void removeFromQueue(int i) throws SQLException {
        String sql = "DELETE FROM `crawler`.`visitqueue` WHERE `POS`='" + i +
                "';";
        DB.runSql2(sql);
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
            text = fixName(text);
            Document htmlDoc;
            Connection conn = Jsoup.connect(currentlink[0]);
            String fileExt = URL.substring(URL.length() - 4,
                    URL.length());
            if (docCheck(fileExt)) {
                if (conn.response().statusCode() >= 400) {
                    check(URL, text);
                    addBrokenLink(currentlink, conn.response().statusCode());
                } else {
                    check(URL, text);
                }
            } else {
                conn.userAgent(USER_AGENT).ignoreContentType(true);
                htmlDoc = conn.get();
                this.htmlDocument = htmlDoc;
                dPrint("Visiting Page: " + URL);
                setTitle();

                String tableTitle;

                int responseCode = conn.response().statusCode();

                check(URL, text);
                if (responseCode == 200) {
                    dPrint("Web Page Received, checking table " + this.pageTitle);
                    int pageCheck = checkPageTable(this.pageTitle, URL);
                    if (pageCheck == 0) {
                        dPrint("Creating page table, and looking for links");
                        tableTitle = this.pageTitle;
                        createPageTable(URL, this.pageTitle, parentURL, parentText);
                    } else if (pageCheck == 1) {
                        dPrint("Table created already, needs to add parent.");
                        tableTitle = this.pageTitle;
                        addParent(this.pageTitle, currentlink);
                    } else {
                        String newTitle = this.pageTitle + text;
                        newTitle = fixName(newTitle);
                        pageCheck = checkPageTable(newTitle, URL);
                        if (pageCheck == 1) {
                            addParent(newTitle, currentlink);
                            tableTitle = newTitle;
                        } else if (pageCheck == 0) {
                            tableTitle = newTitle;
                            createPageTable(URL, newTitle, parentURL, parentText);
                        } else {
                            int i = 1;
                            String newTitle2;
                            while (true) {
                                newTitle2 = newTitle + i;
                                pageCheck = checkPageTable(newTitle2, URL);
                                if (pageCheck == 1) {
                                    addParent(newTitle, currentlink);
                                    tableTitle = newTitle;
                                    break;
                                } else if (pageCheck == 0) {
                                    tableTitle = newTitle2;
                                    createPageTable(URL, tableTitle, parentURL,
                                            parentText);
                                    break;
                                }
                                i++;
                            }
                        }
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
                                if (!pagestoVisit.contains(checkedLink[0])) {
                                    this.pagestoVisit.add(checkedLink[0]);
                                    addToQueue(checkedLink, this.start);
                                    this.pagestoVisitQueue.add(checkedLink);
                                    dPrint("Adding to pagesToVisit");
                                }
                            }
                        }
                        this.submitToPageTable(tableTitle, checkedLink);
                    }
                } else {
                    this.addBrokenLink(currentlink, responseCode);
                }
            }
        } catch (HttpStatusException httpE) {
            int statusCode = httpE.getStatusCode();
            String URL = httpE.getUrl();
            String[] link = new String[4];
            link[0] = URL;
            try {
                addBrokenLink(link, statusCode);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException sqlE) {
            sqlE.printStackTrace();
        }
    }
}
