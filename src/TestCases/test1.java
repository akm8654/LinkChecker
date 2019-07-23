package TestCases;

import SiteMap.SideConnection;

public class test1 {
    public static String[] getParentID(String childURL){
        String[] parsed = childURL.split("/");

        return parsed;
    }

    public static void main(String[] args){
        String[] parsed = getParentID("https://www.rit" +
                ".edu/experiential-learning/community-engagement");
        for (int i = 0; i < parsed.length; i++){
            System.out.println(parsed[i]);
        }
    }
}
