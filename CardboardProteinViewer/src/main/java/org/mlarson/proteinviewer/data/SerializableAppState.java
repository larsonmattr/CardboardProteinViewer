package org.mlarson.proteinviewer.data;

import com.google.vrtoolkit.cardboard.samples.treasurehunt.ProteinStructure;

import org.biojava.bio.structure.Structure;

import java.io.Serializable;

/**
 * Created by larsonmattr on 4/11/2015.
 *
 * Describes the application state with data classes such as the current protein structure being viewed, etc.
 * This class is not meant to hold user preferences.
 *
 */
public class SerializableAppState implements Serializable {
    private String pdbId = null;

    public String getPdb() {
        return pdbId;
    }

    public void setPdb(String pdb) {
        this.pdbId = pdb;
    }
}
