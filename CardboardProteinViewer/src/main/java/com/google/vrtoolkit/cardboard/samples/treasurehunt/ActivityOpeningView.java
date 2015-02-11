package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Activity will show a list of downloaded PDB files and
 * Created by larsonmattr on 2/10/2015.
 */
public class ActivityOpeningView extends Activity {
    Bundle appState;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        appState = state;

        setContentView(R.layout.activity_opening_view);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

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
}
