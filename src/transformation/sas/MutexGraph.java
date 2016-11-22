/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package transformation.sas;

import fLib.utils.Pair;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import representations.classic.Action;
import representations.classic.Atom;
import representations.classic.ClassicGroundTask;
import transformation.sas.overcover.entities.StandardAtom;
import transformation.sas.overcover.planninggraph.MutexAtoms;

/**
 *
 * @author FD
 */
class MutexGraph {

    //private HashMap<Atom, Integer> atom2index;
    private static int project(int i, int j) {
        if (i < j) {
            return j * (j - 1) / 2 + i;
        } else {
            return i * (i - 1) / 2 + j;
        }
    }

    private boolean[] doubleCosts; // cost of pair - true: reachable, false: unreachable
    private boolean[] singleCosts; // cost of one

    //private HashSet<Pair<Integer, Integer>> test = new HashMap<>();
    private void init(int numberOfAtoms) {
        int n = (numberOfAtoms * numberOfAtoms - numberOfAtoms) / 2;
        doubleCosts = new boolean[n];
        singleCosts = new boolean[numberOfAtoms];
    }

    private void setValue(int i, int j, boolean value) {
        if (i == j) {
            singleCosts[i] = value;

        } else {
            doubleCosts[project(i, j)] = value;

        }
    }

    private boolean getValue(int i, int j) {
        if (i == j) {
            return singleCosts[i];
        } else {
            return doubleCosts[project(i, j)];
        }

    }

    static MutexAtoms GenerateAndReduce(ClassicGroundTask in, LinkedHashSet<Atom> allAtoms) {

        //prepare data structures
        int numberOfAtoms = allAtoms.size();
        MutexGraph g = new MutexGraph();
        g.init(numberOfAtoms);       
        
        //initialize the initial state for single atoms
        HashSet<Atom> init = new HashSet<>(in.init);
        for (Atom a : allAtoms) {
            int atomIndex = a.mID;
            if (init.contains(a)) {
                g.setValue(atomIndex, atomIndex, true);
            } else {
                g.setValue(atomIndex, atomIndex, false);
            }
        }

        //initialize the initial state for pair of atoms
        for (int i = 0; i < numberOfAtoms; i++) {
            for (int j = 0; j < i; j++) {
                g.setValue(i, j, false);
            }
        }
        for (Atom a : in.init) {
            for (Atom b : in.init) {
                if (a.mName.compareTo(b.mName) < 0) { //symmetry breaking
                    g.setValue(a.mID, b.mID, true);
                }
            }
        }

        long start = System.currentTimeMillis();
        //main loop
        boolean done = false;
        while (!done) {
            done = true;
            for (Action a : in.actions) {
                if (!g.mutex(a.preconditions)) {
                    for (int i = 0; i < numberOfAtoms; i++) {
                        if (contains(a.positiveEffects, i)) {
                            if (!g.getValue(i, i)) {
                                g.setValue(i, i, true);
                                done = false;
                            }
                            for (Atom at : a.positiveEffects) {
                                if (!g.getValue(at.mID, i)) {
                                    g.setValue(at.mID, i, true);
                                    done = false;
                                }
                            }
                        } else if (g.getValue(i, i) && !contains(a.negativeEffects, i)) {
                            boolean ok = true;
                            for (Atom at : a.preconditions) {
                                if (!g.getValue(at.mID, i)) {
                                    ok = false;
                                }
                            }
                            if (ok) {
                                for (Atom at : a.positiveEffects) {
                                    if (!g.getValue(at.mID, i)) {
                                        g.setValue(at.mID, i, true);
                                        done = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        System.out.println("[Total time spent looking up mutexes: "+((System.currentTimeMillis() - start)/1000d) + "s ]");
        List<Action> actions = new LinkedList<>();
        //take only non-mutex actions
        for (Action a : in.actions) {
            if (g.mutex(a.preconditions)) {
                //removeAction.add(a);
            } else {
                actions.add(a);
            }
        }

        //take only reachable atoms
        /*HashSet<Integer> removeAtom = new HashSet<>();        
         for (int i = 0; i < numberOfAtoms; i++) {
         if (!g.singleCosts[i]) {
         removeAtom.add(i);
         }
         }*/
        //then we run reachability on atoms
        List<Action> actionsToTry = new LinkedList<>(actions);
        List<Action> appliedActions = new LinkedList<>();
        HashSet<Atom> kb = new HashSet<>(in.init);
        int oldSize = -1;
        while (oldSize != kb.size()) {
            oldSize = kb.size();
            Iterator<Action> it = actionsToTry.iterator();
            while (it.hasNext()) {
                Action ac = it.next();
                if (isApplicable(kb, ac)) {
                    apply(kb, ac);
                    it.remove();
                    appliedActions.add(ac);
                }
            }
        }

        in.atoms = new LinkedList<>(kb);
        HashSet<Integer> selectedAtoms = new HashSet<>();
        for (Atom a : in.atoms) {
            selectedAtoms.add(a.mID);
        }
        in.actions = new LinkedList<>(appliedActions);

        //remove actions that are instantiated with the same constant for two parameters
        /*List<Action> remove = new LinkedList<>();
        for (Action a : in.actions) {
            for(int i = 0; i < a.preconditions.size(); i++){
                for(int j = 0; j < i; j++){
                    if(a.preconditions.get(i).mID == a.preconditions.get(j).mID){
                        remove.add(a);
                    }
                }
            }
        }
        in.actions.removeAll(remove);*/

        MutexAtoms ma = new MutexAtoms();
        for (int i = 0; i < numberOfAtoms; i++) {
            for (int j = 0; j < i; j++) {
                if (!g.getValue(i, j)) {
                    if (selectedAtoms.contains(j) && selectedAtoms.contains(i)) {
                        ma.Add(new StandardAtom(Atom.GetAtomName(i)), new StandardAtom(Atom.GetAtomName(j)));
                    }
                }
            }
        }
        return ma;
    }

    private static boolean contains(List<Atom> l, int atom) {
        for (Atom a : l) {
            if (a.mID == atom) {
                return true;
            }
        }
        return false;
    }

    static LinkedHashSet<Atom> GetReachableAtoms(ClassicGroundTask in) {
        List<Action> actionsToTry = new LinkedList<>(in.actions);
        List<Action> appliedActions = new LinkedList<>();
        HashSet<Atom> kb = new HashSet<>(in.init);
        int oldSize = -1;
        while (oldSize != kb.size()) {
            oldSize = kb.size();
            Iterator<Action> it = actionsToTry.iterator();
            while (it.hasNext()) {
                Action ac = it.next();
                if (isApplicable(kb, ac)) {
                    apply(kb, ac);
                    it.remove();
                    appliedActions.add(ac);
                }
            }
        }
        return new LinkedHashSet<>(kb);
    }

    private static boolean isApplicable(HashSet<Atom> kb, Action a) {
        for (Atom at : a.preconditions) {
            if (!kb.contains(at)) {
                return false;
            }
        }
        return true;
    }

    private static void apply(HashSet<Atom> kb, Action a) {
        for (Atom at : a.positiveEffects) {
            kb.add(at);
        }
    }

    private boolean mutex(List<Atom> preconditions) {
        for (Atom a : preconditions) {
            int index = a.mID;
            if (!singleCosts[index]) {
                return true; //this is a mutex
            }
        }
        for (Atom a : preconditions) {
            for (Atom b : preconditions) {
                if (a.mID < b.mID) { // symmetry breaking
                    if (!doubleCosts[project(a.mID, b.mID)]) {
                        return true; //this is a mutex
                    }
                }
            }
        }
        return false;
    }

}
