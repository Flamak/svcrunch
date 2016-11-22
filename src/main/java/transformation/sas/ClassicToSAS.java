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
package transformation.sas;

import fLib.utils.TestInt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.html.CSS;
import representations.SAS.*;
import representations.classic.Action;
import representations.classic.Atom;
import representations.classic.ClassicGroundTask;
import transformation.sas.overcover.CliqueSearch;
import transformation.sas.overcover.SetCovering;
import transformation.sas.overcover.entities.ActionInstance;
import transformation.sas.overcover.entities.StandardAtom;
import transformation.sas.overcover.planninggraph.MutexAtoms;
import transformation.sas.overcover.planninggraph.PlanningGraph;

/**
 *
 * @author FD
 */
public class ClassicToSAS {

    private static PlanningGraph prepareThePlanningGraph(ClassicGroundTask in) {
        //preparing the planning graph
        PlanningGraph g = new PlanningGraph();
        for (Atom a : in.init) {
            g.init.add(new StandardAtom(a.mName));
        }
        for (Atom a : in.goal) {
            g.goal.add(new StandardAtom(a.mName));
        }
        for (Atom a : in.atoms) {
            g.atoms.add(new StandardAtom(a.mName));
        }
        for (Action a : in.actions) {
            List precons = new LinkedList<>();
            for (Atom at : a.preconditions) {
                precons.add(new StandardAtom(at.mName));
            }
            List posEffects = new LinkedList<>();
            for (Atom at : a.positiveEffects) {
                posEffects.add(new StandardAtom(at.mName));
            }
            List negEffects = new LinkedList<>();
            for (Atom at : a.negativeEffects) {
                negEffects.add(new StandardAtom(at.mName));
            }
            g.actions.add(new ActionInstance(precons, posEffects, negEffects, a.mName, a.mCost));
        }

        //lets construct the graph
        g.Construct();
        g.RemoveNoOps();
        g.RemoveStaticAtomOccurences();

        //System.out.println("Removed atoms: " + (in.atoms.size() - g.cumulativeAtomSet.size()));
        //System.out.println("Removed actions: " + (in.actions.size() - g.cumulativeActionSet.size()));
        return g;
    }

    private static void printHistogram(LinkedList<LinkedList<StandardAtom>> someMS) {
        int n = - 1;
        for (LinkedList<StandardAtom> l : someMS) {
            n = Math.max(n, l.size());
        }
        int[] pl = new int[n + 1];
        for (LinkedList<StandardAtom> l : someMS) {
            pl[l.size()]++;
        }
        for (int i = 0; i < pl.length; i++) {
            if (pl[i] > 0) {
                System.out.println(i + ": " + pl[i]);
            }
        }
    }

    public static SASTask TransformTryOut(ClassicGroundTask in) {
        PlanningGraph g = prepareThePlanningGraph(in);

        //lets find some mutex sets        
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(g.atomMutexLayer.get(g.atomMutexLayer.size() - 1), g.cumulativeAtomSet);
        LinkedList<LinkedList<StandardAtom>> greedyMS = SetCovering.greedySetCover(mutexSets);
        LinkedList<LinkedList<StandardAtom>> smartMS = SetCovering.smartSetCoverN1(mutexSets, g.cumulativeAtomSet);

        System.out.println("Mutex sets histogram follows (size:count)");
        printHistogram(mutexSets);
        System.out.println("greedyMS histogram follows (size:count)");
        printHistogram(greedyMS);
        System.out.println("smartMS histogram follows (size:count)");
        printHistogram(smartMS);

        SASTask task = new SASTask();

        return task;
    }

    public static SASTask TransformGreedyCover(ClassicGroundTask in) {
        PlanningGraph g = prepareThePlanningGraph(in);
        //lets find some mutex sets
        System.out.println("Mutex relations count: " + g.atomMutexLayer.get(g.atomMutexLayer.size() - 1).GetSize());
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(g.atomMutexLayer.get(g.atomMutexLayer.size() - 1), g.cumulativeAtomSet);
        LinkedList<LinkedList<StandardAtom>> greedyMS = SetCovering.greedySetCover(mutexSets);
        SASTask task = createSASTask(greedyMS, in.mName + "//ClassicToSAS.TransformGreedyCover", in);
        return task;
    }

    public static SASTask TransformGreedyCoverWithLeftOvers(ClassicGroundTask in) {
        PlanningGraph g = prepareThePlanningGraph(in);
        //lets find some mutex sets
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(g.atomMutexLayer.get(g.atomMutexLayer.size() - 1), g.cumulativeAtomSet);
        LinkedList<LinkedList<StandardAtom>> greedyMS = SetCovering.greedySetCoverWithLeftOvers(mutexSets);
        SASTask task = createSASTask(greedyMS, in.mName + "//ClassicToSAS.TransformGreedyCover", in);
        return task;
    }

    private static SASTask createSASTask(LinkedList<LinkedList<StandardAtom>> mutexSets, String taskName, ClassicGroundTask in) {
        SASTask task = new SASTask();
        task.mName = taskName;

        int ct = 0;
        for (LinkedList<StandardAtom> var : mutexSets) {
            SVariable newVar = new SVariable();
            newVar.mName = "var" + (ct++);
            for (StandardAtom a : var) {
                SValue val = new SValue();
                val.mName = a.name;
                newVar.domain.add(val);
            }
            SValue val = new SValue();
            val.mName = "<none of those>" + newVar.mName;
            newVar.domain.add(val);
            task.variables.add(newVar);
        }
        //create mappings .. values (represented by strings) to its variables (in whose domain they are)
        HashMap<String, List<SVariable>> mp = new HashMap<>();
        for (SVariable v : task.variables) {
            for (SValue val : v.domain) {
                List<SVariable> l = mp.get(val.mName);
                if (l == null) {
                    l = new LinkedList<>();
                }
                l.add(v);
                mp.put(val.mName, l);
            }
        }
        //get init
        HashSet<String> varNames = new HashSet<>();
        for (Atom at : in.init) {
            List<SVariable> l = mp.get(at.mName);
            for (SVariable var : l) {
                SAssignment ass = new SAssignment();
                SValue v = new SValue();
                v.mName = at.mName;
                ass.val = v;
                ass.var = var;
                task.init.add(ass);
                varNames.add(ass.var.mName);
            }
        }
        //check - only holds for greedy covering
        /*int cnt = task.init.size();
         for(SAssignment v:task.init){
         cnt -= mp.get(v.val.mName).size();
         }
         if(cnt != 0){
         throw new UnsupportedOperationException("Invalid init.");
         }*/

        //get goal
        varNames = new HashSet<>();
        for (Atom a : in.goal) {
            List<SVariable> l = mp.get(a.mName);
            for (SVariable var : l) {
                SAssignment ass = new SAssignment();
                SValue v = new SValue();
                v.mName = a.mName;
                ass.val = v;
                ass.var = var;
                task.goal.add(ass);
                varNames.add(ass.var.mName);
            }
        }
        for (Action a : in.actions) {
            SAction ac = new SAction();
            ac.mName = a.mName;
            ac.mCost = a.mCost;
            //preconditions
            for (Atom at : a.preconditions) {
                List<SVariable> l = mp.get(at.mName);
                for (SVariable var : l) {
                    SAssignment ass = new SAssignment();
                    ass.var = var;
                    SValue val = new SValue();
                    val.mName = at.mName;
                    ass.val = val;
                    ac.preconditions.add(ass);
                }
            }
            //effects
            for (Atom at : a.positiveEffects) {
                List<SVariable> l = mp.get(at.mName);
                for (SVariable var : l) {
                    SAssignment ass = new SAssignment();
                    ass.var = var;
                    SValue val = new SValue();
                    val.mName = at.mName;
                    ass.val = val;
                    ac.effects.add(ass);
                }
            }
            //negative effects
            //try to check, if there exists some atom in positive effects, that is mutex with me, if it does, i dont have to do anything, if it does not, i have a problem ...
            for (Atom at1 : a.negativeEffects) {
                //we need to explain what happens with the negative effect - it can be omited in following scenarios:
                // 1) there exists a positive effect that is a value in the same state variable as is the negative effect
                boolean allIsFine = false;
                for (Atom at2 : a.positiveEffects) {
                    /*StandardAtom ats1 = new StandardAtom(at1.name);
                     StandardAtom ats2 = new StandardAtom(at2.name);*/
                    List<SVariable> intersection = new LinkedList<>(mp.get(at2.mName));
                    intersection.retainAll(mp.get(at1.mName));
                    if (!intersection.isEmpty()) {
                        allIsFine = true;
                    }
                }
                if (!allIsFine) {
                    //this means, that we have to find other way how to represent this negative effect
                    //lets look, if there is other value in the state variable of our value that can be achieved by this value, if it is, lets assign it ... we do not consider undefined values at all
                    List<SVariable> vars = new LinkedList<>(mp.get(at1.mName));
                    SVariable var = vars.get(0);
                    if (var.domain.size() == 2) {
                        //we choose the other value in this domain
                        SAssignment ass = new SAssignment();
                        ass.var = var;
                        ass.val = var.domain.get(1);
                        ac.effects.add(ass);
                    } else {
                        //this is still ok .. we just assign "none of those" value
                        SAssignment ass = new SAssignment();
                        ass.var = var;
                        ass.val = var.domain.get(var.domain.size() - 1);
                        ac.effects.add(ass);
                    }
                }
            }
            task.actions.add(ac);
            for (SAssignment ass : ac.effects) {
                boolean iAmAlone = true;
                for (SAssignment c : ac.preconditions) {
                    if (c.var.mName.equals(ass.var.mName)) {
                        iAmAlone = false;
                    }
                }
                if (iAmAlone) {
                    System.out.println("There is solo effect.");
                }
            }
        }

        //remove none-of-those
        HashSet<String> vars = new HashSet<>(); //state variables
        for (SAssignment as : task.init) {
            if (as.val.mName.contains("<none of those>")) {
                vars.add(as.var.mName);
            }
        }
        for (SAction a : task.actions) {
            for (SAssignment as : a.preconditions) {
                if (as.val.mName.contains("<none of those>")) {
                    vars.add(as.var.mName);
                }
            }
            for (SAssignment as : a.effects) {
                if (as.val.mName.contains("<none of those>")) {
                    vars.add(as.var.mName);
                }
            }
        }
        
        for(SVariable var:task.variables){
            if(!vars.contains(var.mName)){
                SValue v = null;
                for(SValue e:var.domain){
                    if(e.mName.contains("<none of those>")){
                        v = e;
                    }
                }
                var.domain.remove(v); //we can remove this one, since it is never reached or required
            }
        }

        return task;
    }

    public static SASTask TransformSmartCover(ClassicGroundTask in) {
        PlanningGraph g = prepareThePlanningGraph(in);
        //lets find some mutex sets
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(g.atomMutexLayer.get(g.atomMutexLayer.size() - 1), g.cumulativeAtomSet);
        LinkedList<LinkedList<StandardAtom>> smartMS = SetCovering.smartSetCoverN1(mutexSets, g.cumulativeAtomSet);
        SASTask task = createSASTask(smartMS, in.mName + "//ClassicToSAS.TransformGreedyCover", in);
        return task;
    }

    public static SASTask TransformH2GreedyCover(ClassicGroundTask in) {
        long start = System.currentTimeMillis();
        LinkedHashSet<Atom> allAtoms = MutexGraph.GetReachableAtoms(in);
        MutexAtoms mx = MutexGraph.GenerateAndReduce(in, allAtoms);
        System.out.print("mutexGraph[" + ((System.currentTimeMillis() - start) / 1000f) + "s]" + "...");
        LinkedHashSet<StandardAtom> hs = new LinkedHashSet<>();
        for (Atom a : allAtoms) {
            hs.add(new StandardAtom(a.mName));
        }
        //lets find some mutex sets
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(mx, hs);
        LinkedList<LinkedList<StandardAtom>> cvr = SetCovering.greedySetCover(mutexSets);
        SASTask task = createSASTask(cvr, in.mName + "//ClassicToSAS.TransformH2GreedyCover", in);
        return task;
    }

    public static SASTask TransformH2GreedyCoverWLO(ClassicGroundTask in) {
        long start = System.currentTimeMillis();
        LinkedHashSet<Atom> allAtoms = MutexGraph.GetReachableAtoms(in);
        MutexAtoms mx = MutexGraph.GenerateAndReduce(in, allAtoms);
        System.out.print("mutexGraph[" + ((System.currentTimeMillis() - start) / 1000f) + "s]" + "...");
        LinkedHashSet<StandardAtom> hs = new LinkedHashSet<>();
        for (Atom a : allAtoms) {
            hs.add(new StandardAtom(a.mName));
        }
        //lets find some mutex sets
        LinkedList<LinkedList<StandardAtom>> mutexSets = CliqueSearch.GetMutexSetsLimited(mx, hs);
        LinkedList<LinkedList<StandardAtom>> cvr = SetCovering.greedySetCoverWithLeftOvers(mutexSets);
        SASTask task = createSASTask(cvr, in.mName + "//ClassicToSAS.TransformH2GreedyCoverWLO", in);
        return task;
    }
}
