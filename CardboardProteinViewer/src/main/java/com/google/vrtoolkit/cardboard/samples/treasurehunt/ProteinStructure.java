package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import android.content.Context;
import android.support.annotation.NonNull;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.io.PDBFileParser;
import org.biojava.bio.structure.io.PDBFileReader;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by larsonmattr on 12/14/2014.
 *
 * Reads in a protein structure
 *
 */
public class ProteinStructure {
    private Structure pdb;
    FloatBuffer vertices;

    public ProteinStructure(Context context) {
        // Use BioJava to read in a local filename
        // also works for gzip compressed files

        // Could also use AtomCache, and get the structures from the PDB
        String filename = "raw/structure.pdb";

        PDBFileParser parser = new PDBFileParser();
        InputStream pdbfile = context.getResources().openRawResource(R.raw.structure);

        pdb = null;

        try {
            pdb = parser.parsePDBFile(pdbfile);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Should be a simple class to create a list of Vertices for OpenGL to be able to
     * display a structure in a 3D viewer.
     *
     * @param chain
     * @return a FloatBuffer : Vec3 of atom coordinates of a protein chain.
     */
    public FloatBuffer getVertices(String chain) {
        final int floats_per_vertex = 3;
        final int bytes_per_float = 4;

        if (null != pdb) {
            List<Group> groups = getAtomGroups(chain);
            List<Atom> atoms = getAtoms(groups);
            final int length = atoms.size();

            // We are creating a direct ByteBuffer, which benefits from accelerated operations
            // This is followed by creating a FloatBuffer sharing this memory space.  FloatBuffer
            // doesn't have an allocateDirect method, which is why we start with ByteBuffer.

            // Byte * 4 = Float
            // We can neatly chain allocation of a ByteBuffer with setting up as FloatBuffer.
            vertices = ByteBuffer.allocateDirect(length * bytes_per_float * floats_per_vertex)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

            //  iterate through the atoms, get their 3D positions.
            //  Will create a Vec3 of coordinates.
            int i = 0;
            for (Atom an_atom : atoms) {
                vertices.put(i, (float)an_atom.getX());
                vertices.put(i+1, (float) an_atom.getY());
                vertices.put(i+2, (float) an_atom.getZ());
                // vertices.put(i+3, (float)1.0);
                i += floats_per_vertex;
            }

            return vertices;
        }

        return null;
    }

    /**
     * Gets a list of Atom Groups from a chain
     * @return
     */
    List<Group> getAtomGroups(String chain) {
        if (null != pdb) {
            try {
                Chain chainA = pdb.getChainByPDB(chain);
                return chainA.getAtomGroups();
            } catch (StructureException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Get all the atoms from a list of Groups
     *
     * @param atomGroups
     * @return
     */
    List<Atom> getAtoms(List<Group> atomGroups) {
        ArrayList<Atom> atomList = new ArrayList<Atom>();

        Iterator<Group> iter = atomGroups.iterator();

        while (iter.hasNext()) {
            Group aGroup = iter.next();
            List<Atom> atoms = aGroup.getAtoms();
            Iterator<Atom> iterAtom = atoms.iterator();
            while (iterAtom.hasNext()) {
                Atom anAtom = iterAtom.next();
                atomList.add(anAtom);
            }
        }
        return atomList;
    }
}
