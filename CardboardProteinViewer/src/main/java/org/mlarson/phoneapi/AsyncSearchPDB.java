package org.mlarson.phoneapi;

import android.os.AsyncTask;

import java.util.List;

/**
 * Created by larsonmattr on 4/11/2015.
 */
public class AsyncSearchPDB extends AsyncTask<SearchPDB, Integer, String> {

    SearchPDB search;

    /**
     * Does the HTTP request to the Protein Databank in the background.
     * @param searchPDBs
     * @return
     */
    @Override
    protected String doInBackground(SearchPDB... searchPDBs) {
        String searchResult = "OK";

        search = searchPDBs[0];

        try {
            search.DoSearch();
        } catch (PhoneApiException ex) {
            // Check what kind of exception we got.
            // Examples should include:
            //  * Bad login / password
            //  * Couldn't connect to server
            //  * Application error.. some exception.
            searchResult = ex.toString();
        }

        return searchResult;
    }

    /**
     * Returns the result via a message to the running Activity.
     * @param result
     */
    protected void onPostExecute(String result) {
        // TODO: pass the result of the search back to the Activity.

        // Using the Otto Bus - send a message back to the UI activity to let it handle outcome.
        OttoBus.getInstance().post(new AsyncSearchResultEvent(result, search.getResults()));
    }
}
