package org.mlarson.phoneapi;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by larsonmattr on 4/11/2015.
 */
public class SearchPDB {
    public final static String TAG = "SearchPDB";

    private String queryText;
    private List<String> results = new ArrayList<String>();

    /**
     * Pass the text for the PDB that we which to search for.
     * @param queryText
     */
    public SearchPDB(final String queryText) {
        this.queryText = queryText;
    }

    /**
     * Gets the list of PDB ids (post-query).
     * @return
     */
    public List<String> getResults() {
        return results;
    }

    /**
     * DoSearch will get a list of PDB:ID from the Protein Databank.
     */
    public void DoSearch() throws PhoneApiException {
        // Build the parameters for the GET method.
        String parameters = buildParameters();
        results = PhoneApi.sendPostRequest(parameters, TAG);
    }

    private String buildParameters() {
        /* Example for a text search:

            <orgPdbQuery>

            <queryType>org.pdb.query.simple.AdvancedKeywordQuery</queryType>

            <description>Text Search for: actin</description>

            <keywords>actin</keywords>

            </orgPdbQuery>

         */
        String xml =
                "<orgPdbQuery>" +
                    "<queryType>org.pdb.query.simple.AdvancedKeywordQuery</queryType>" +
                    "<description>Text Search for: " + queryText + "</description>" +
                    "<keywords>" + queryText + "</keywords>" +
                "</orgPdbQuery>";

        return xml;
    }
}
