package org.mlarson.phoneapi;

import java.util.List;

/**
 * Created by larsonmattr on 4/11/2015.
 */
public class AsyncSearchResultEvent {

    private String result = null;
    private List<String> pdbIds = null;

    public AsyncSearchResultEvent(String result, List<String> ids) {
        this.result = result;
        this.pdbIds = ids;
    }

    public String getResultMessage() {
        return result;
    }
    public List<String> getPdbIds() { return pdbIds; }
}
