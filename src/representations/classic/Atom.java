/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2012 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package representations.classic;

import io.pddl.in.objects.IOLiteral;
import io.pddl.in.objects.IOVariable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author FD
 */
public final class Atom {

    public static void ResetIndexes() {
        indexer = new HashMap<>();
        inverseIndexer = new HashMap<>();
        counter = 0;
    }

    public void ReIndex() {
        if (!indexer.containsKey(mName)) {
            indexer.put(mName, counter);
            inverseIndexer.put(counter, mName);
            counter++;
        }
        mID = indexer.get(mName);
    }

    public Atom(String name) {
        mName = name;
        ReIndex();
    }

    private static HashMap<String, Integer> indexer = new HashMap<>();
    private static HashMap<Integer, String> inverseIndexer = new HashMap<>();
    private static int counter = 0;

    public static String GetAtomName(int i) {
        return inverseIndexer.get(i);
    }

    public int mID = -1;

    public String mName;

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public boolean equals(Object obj) {
        return ((Atom) obj).mID == mID;
        //return ((Atom)obj).mName.equals(mName);        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + mID;//Objects.hashCode(this.mName);
        return hash;
    }

    public String GetPredicateName() {
        return mName.split("\\(")[0];
    }

    public List<IOVariable> GetObjectNames() {
        List<IOVariable> ret = new LinkedList<>();
        String[] ar = mName.split("\\(")[1].split("\\)")[0].split(",");
        for (int i = 0; i < ar.length; i++) {
            IOVariable lit = new IOVariable();
            lit.mName = ar[i];
            ret.add(lit);
        }
        return ret;
    }
}
