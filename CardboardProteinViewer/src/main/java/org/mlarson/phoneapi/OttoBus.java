package org.mlarson.phoneapi;

import com.squareup.otto.Bus;

/**
 * Created by mlarson on 11/10/2014.
 * To connect AsyncTasks to Activity's that manage the UI, I am trying to use Square's Otto library
 *
 * Otto provides the functionality to separate AsyncTasks into their own classes, rather than trying
 * to use an AsyncTask as an inner class of an Activity.  The Bus provided acts similarly to the
 * Intent system but within our application only.
 *
 * It is recommended to implement it as a Singleton.
 *
 * To have an Activity receive an event from the bus use "@Subscribe" with a method and register your instance
 *
 * Ex: @Subscribe public void onAsyncTaskResult(AsyncTaskResultEvent event) {}
 * Have Activity register in onCreate:   OttoBus.getInstance().register(this);
 * And in onDestory: OttoBus.getInstance().unregister(this);  super.onDestroy();
 *
 * AsyncTask can communicate that it finished back to the activity with:
 * OttoBus.getInstance().post(new AsyncTaskResultEvent(result));
 */
public class OttoBus {
    /* For inter-thread events, use the Otto Bus
    *  Recommended to be used as a Singleton */
    private static final Bus BUS = new Bus();

    /**
     * Get access to the application Bus Singleton..
     * @return
     */
    public static Bus getInstance() {
        return BUS;
    }

}
