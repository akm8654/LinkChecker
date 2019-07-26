package TestCases;

import SiteMap.Database;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class resetSQL {
    public static void main(String[] args) {
        Database DB = new Database();
        String sql = "TRUNCATE TABLE `crawler`.`brokenlinks`;";
        try {
            DB.runSql2(sql);
            sql = "TRUNCATE TABLE `crawler`.`record`;";
            DB.runSql2(sql);
            sql = "TRUNCATE TABLE `crawler`.`pagetables`;";
            DB.runSql2(sql);
            DatabaseMetaData md = DB.conn.getMetaData();
            ResultSet rs = md.getTables("`individual names`", null, "%", null);
            while (rs.next()) {
                sql = "DROP TABLE `individual names`.`" + rs.getString(3) + "`;";
                DB.runSql2(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
