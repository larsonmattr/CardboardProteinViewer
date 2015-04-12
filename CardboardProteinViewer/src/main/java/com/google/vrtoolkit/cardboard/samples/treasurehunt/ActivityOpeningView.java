package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import com.squareup.otto.Subscribe;

import org.mlarson.phoneapi.AsyncSearchPDB;
import org.mlarson.phoneapi.AsyncSearchResultEvent;
import org.mlarson.phoneapi.OttoBus;
import org.mlarson.phoneapi.SearchPDB;
import org.mlarson.proteinviewer.data.AppConfig;
import org.mlarson.proteinviewer.data.SerializableAppState;

/**
 * Activity will show a list of downloaded PDB files and
 * Created by larsonmattr on 2/10/2015.
 */
public class ActivityOpeningView extends Activity {
    ListView pdbListView;

    SerializableAppState appState;

    ProgressDialog waiting_dialog;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // appState coming in from an intent - could happen if the user is directed back to login from other activity.
        Intent mIntent = getIntent();

        // Load our application state from a bundle.
        if ((state != null) && (state.getSerializable(AppConfig.key)) != null) {
            appState = (SerializableAppState) state.getSerializable(AppConfig.key);
        } else if (null != mIntent) {
            Bundle extras = getIntent().getExtras();
            if (extras != null)
                appState = (SerializableAppState) extras.getSerializable(AppConfig.key);
        }

        // Otherwise, create an appState.
        if (null == appState) {
            // Create a new appState.
            appState = new SerializableAppState();
        }

        setContentView(R.layout.activity_opening_view);

        // setup a ListView with selectable PDB:ID values.
        pdbListView = (ListView) findViewById(R.id.pdbListView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        pdbListView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Setup the SearchView to handle to submission..
        SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // do the search
                // create an AsyncTask to submit a POST request to the PDB website.
                // process the query response and get

                SearchPDB search = new SearchPDB(query);
                AsyncSearchPDB asyncSearch = new AsyncSearchPDB();
                asyncSearch.execute(search);

                // Setup a dialog box with a spinner while AsyncTask runs.
                createLoadingDialog();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent mIntent;
        switch (item.getItemId()) {
            case R.id.about:
                // TODO: launch about activity or modal dialog
                return true;
            case R.id.help:
                // TODO: launch help activity or modal dialog
                return true;
            case R.id.basic:
                mIntent = new Intent(this, ProteinActivity.class);
                startActivity(mIntent);
                return true;
            case R.id.special:
                mIntent = new Intent(this, FancyProteinActivity.class);
                startActivity(mIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register this instance to receive events
        // Placing registration in onStart/onStop may help with issues related to calls in a different
        // thread occurring from fragments.
        OttoBus.getInstance().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        OttoBus.getInstance().unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(AppConfig.key, appState);
    }

    /**
     * Start a waiting dialog, while the node request goes out.
     */
    public void createLoadingDialog() {
        waiting_dialog = new ProgressDialog(this);
        waiting_dialog.setCancelable(true);
        waiting_dialog.setCanceledOnTouchOutside(false);
        waiting_dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                waiting_dialog.hide();
            }
        });
        ((ProgressDialog) waiting_dialog)
                .setProgressStyle(ProgressDialog.STYLE_SPINNER);
        waiting_dialog.setMessage(getString(R.string.pdb_query_message));

        waiting_dialog.show();
    }

    /**
     *
     * @param event
     */
    @Subscribe
    public void onSearchResults(AsyncSearchResultEvent event) {
        waiting_dialog.hide();

        if (event.getResultMessage().equals("OK")) {

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, event.getPdbIds());

            pdbListView.setAdapter(adapter);
        }
    }
}
