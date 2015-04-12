package org.mlarson.proteinviewer.data;

/**
 * Created by larsonmattr on 4/11/2015.
 */
public class AppConfig {
    public static boolean DEBUG = true;

    public static String key = "ProteinViewer";

    // Don't yet have a development link different than production.
    private static String PROD_LINK = "http://www.rcsb.org/pdb/rest/search";
    private static String DEV_LINK = PROD_LINK;

    /**
     * Get the link for the imgsmartphone server.
     * @return
     */
    public static String getLink() {
        if (DEBUG) return DEV_LINK;
        return PROD_LINK;
    }
}
