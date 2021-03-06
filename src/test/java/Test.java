package test;

import fLib.utils.io.FileHandling;
import io.pddl.in.PDDLFactory;
import io.pddl.in.objects.IODomain;
import io.pddl.in.objects.IOProblem;
import io.sas.out.HelmertSASv3Generator;
import java.io.File;
import java.io.FileFilter;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import representations.SAS.SASTask;
import representations.classic.Atom;
import representations.classic.ClassicGroundTask;
import transformation.sas.ClassicToSAS;
import transformation.standard.ClassicToClassic_KBReductionNaive;
import transformation.standard.IOToClassic;
import transformation.sas.overcover.PlanningTask;
import transformation.standard.IOToCLassic_KBReduction;

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
/**
 *
 * @author FD
 */
public class Test {

    private static void getPathsRec(File uf, LinkedList<String> paths) {
        if (!uf.exists() || !uf.isDirectory()) {
            return;
        }

        File[] pddls = uf.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.getName().contains(".pddl") && !fi.isDirectory();
            }
        });

        File[] dirs = uf.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.isDirectory() && !fi.getAbsolutePath().contains("tempo-sat") && !fi.getAbsolutePath().contains("satellite") && !fi.getAbsolutePath().contains("market");//&& !fi.getName().contains("openstacks"); //openstacks do not type variables in action descriptions ...
            }
        });

        for (File f : pddls) {
            //tryParseTree(FileHandling.getFileContents(f));
            paths.add(f.getAbsolutePath());
        }

        for (File f : dirs) {
            getPathsRec(f, paths);
        }
    }

    public static void massASTParsingTest() throws Exception {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File("C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-sat"), paths);
        int sucCt = 0;
        for (String s : paths) {
            try {
                CommonTree t = PDDLFactory.getTree(FileHandling.readFileContents(s));
                sucCt++;
            } catch (Exception e) {
                System.out.println("Error in: " + s);
                System.out.println("Ct:" + sucCt);
                throw e;
            }
        }
    }

    private static void parsePADtest() {
        //IODomain d = PDDLFactory.parseDomain("C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-sat/woodworking/domain/domain.pddl");
        //IOProblem p = PDDLFactory.parseProblem("C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-sat/barman/problems/pfile06-021.pddl");

        parseAllTest();

        int xx = 0;
    }

    private static void parseAllTest() {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File("C:/ROOT/PROJECTS/pddlParser/data"), paths);
        int ct = 0;
        int onePercent = paths.size() / 100;

        for (String s : paths) {
            if (ct++ % onePercent == 0) {
                System.out.println("Progress: " + ct / onePercent + "%");
            }
            try {
                if (s.contains("domain")) {
                    IODomain d = PDDLFactory.parseDomain(s);
                } else {
                    IOProblem p = PDDLFactory.parseProblem(s);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error in: " + s);
            }
        }

        System.out.println("Total: " + ct);
    }

    public static void tryParseAllToClassic() {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File("C:/ROOT/PROJECTS/pddlParser/data"), paths);

        System.out.println("Parsing domains ...");
        HashMap<String, IODomain> doms = new HashMap<>();
        LinkedList<IOProblem> problems = new LinkedList<>();
        {
            int ct = 0;
            int onePercent = paths.size() / 100;
            for (String s : paths) {
                if (ct++ % onePercent == 0) {
                    System.out.println("Progress: " + ct / onePercent + "%");
                }
                try {
                    if (s.contains("domain")) {
                        IODomain d = PDDLFactory.parseDomain(s);
                        doms.put(d.domainName, d);
                    } else {
                        IOProblem p = PDDLFactory.parseProblem(s);
                        problems.add(p);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error in: " + s);
                }
            }
        }
        List<ClassicGroundTask> tasks = new LinkedList<>();
        for (IOProblem p : problems) {
            long n = System.currentTimeMillis();
            ClassicGroundTask t = IOToClassic.Transform(p, doms.get(p.domainName));
            double timeInSec = (System.currentTimeMillis() - n) / 1000f;
            System.out.println("Processing of the task: " + t + " took: " + timeInSec + "s");
            long k = System.currentTimeMillis();
            ClassicGroundTask t2 = ClassicToClassic_KBReductionNaive.Transform(t);
            timeInSec = (System.currentTimeMillis() - k) / 1000f;
            System.out.println("Processing of the task: " + t2 + " took: " + timeInSec + "s");
            tasks.add(t2);
        }
        int xx = 0;
    }

    public static void tryParseClassic2SAS() {
        long n = System.currentTimeMillis();
        IODomain d = PDDLFactory.parseDomain("C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-opt/elevators/domain/domain.pddl");
        IOProblem p = PDDLFactory.parseProblem("C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-opt/elevators/problems/p01.pddl");

        ClassicGroundTask t = IOToClassic.Transform(p, d);
        System.out.println("Task:" + t);
        ClassicGroundTask t2 = ClassicToClassic_KBReductionNaive.Transform(t);
        System.out.println("Task:" + t2);
        SASTask t3 = ClassicToSAS.TransformGreedyCover(t2);
        HelmertSASv3Generator.Output(t3, "testik.sas");

        int xx = 0;
    }

    public static void PDDLToSAS(String domPath, String prPath, String outPath) {
        long start;
        System.out.println("Translating " + prPath + "...");
        System.out.println("Running h2 mutex graph building ...");

        {
            File out = new File(outPath);
            if (out.exists()) {
                System.out.println("Already translated... Skipping.");
                return;
            }
        }

        //parse domain
        start = System.currentTimeMillis();
        System.out.print("Parsing domain...");
        IODomain d = PDDLFactory.parseDomain(domPath);
        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

        //parse problem
        start = System.currentTimeMillis();
        System.out.print("Parsing problem...");
        IOProblem p = PDDLFactory.parseProblem(prPath);
        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

        ClassicGroundTask theTask;

        boolean tr = false;
        if (tr) { //this is the combined step of the algorithm
            //trans new
            start = System.currentTimeMillis();
            System.out.print("IOToCLassic_KBReduction transforming task...");
            theTask = IOToCLassic_KBReduction.Transform(p, d);
            System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");
        } else {
            //trans
            start = System.currentTimeMillis();
            System.out.print("IOToClassic transforming task...");
            ClassicGroundTask tx = IOToClassic.Transform(p, d);
            System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

            //trans
            start = System.currentTimeMillis();
            System.out.print("ClassicToClassic_KBReductionNaive transforming task...");
            theTask = ClassicToClassic_KBReductionNaive.Transform(tx);
            System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

            //reindexing
        }

        /**
         * test case
         */
        //List<Atom> l = new LinkedList<>(tz.atoms);
        //l.removeAll(ty.atoms);
        //trans1
        /*start = System.currentTimeMillis();
         System.out.print("ClassicToSAS.TransformGreedyCover transforming task...");
         SASTask t3 = ClassicToSAS.TransformGreedyCover(theTask);
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

         //output1
         start = System.currentTimeMillis();
         System.out.print("Writing task...");
         HelmertSASv3Generator.Output(t3, outPath + "_greedy.sas");
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");*/
        //trans h2
        start = System.currentTimeMillis();
        System.out.print("ClassicToSAS.TransformH2GreedyCover transforming task...");
        SASTask t00 = ClassicToSAS.TransformH2GreedyCover(theTask);
        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

        //output00
        start = System.currentTimeMillis();
        System.out.print("Writing task...");
        HelmertSASv3Generator.Output(t00, outPath + "_h2_greedy.sas");
        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

        /*
         start = System.currentTimeMillis();
         System.out.print("ClassicToSAS.TransformH2GreedyCoverLeftOvers transforming task...");
         SASTask t00 = ClassicToSAS.TransformH2GreedyCoverWLO(theTask);
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

         //output00
         start = System.currentTimeMillis();
         System.out.print("Writing task...");
         HelmertSASv3Generator.Output(t00, outPath + "_h2_greedy_wlo.sas");
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");
         */
        /*
         //trans2
         start = System.currentTimeMillis();
         System.out.print("ClassicToSAS.TransformSmartCover transforming task...");
         SASTask t4 = ClassicToSAS.TransformSmartCover(t2);
         System.out.println("["+((System.currentTimeMillis()-start)/1000f)+"s]");
        
         //output2
         start = System.currentTimeMillis();
         System.out.print("Writing task...");
         HelmertSASv3Generator.Output(t4, outPath+"oc.sas");
         System.out.println("["+((System.currentTimeMillis()-start)/1000f)+"s]");
         */
        /*
         //trans3
         start = System.currentTimeMillis();
         System.out.print("ClassicToSAS.TransformGreedyCoverWithLeftOvers transforming task...");
         SASTask t4 = ClassicToSAS.TransformGreedyCoverWithLeftOvers(theTask);
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

         //output3
         start = System.currentTimeMillis();
         System.out.print("Writing task...");
         HelmertSASv3Generator.Output(t4, outPath + "_greedy_wlo.sas");
         System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");*/
    }

    public static void main(String[] args) throws RecognitionException, Exception {
        if (args.length < 3) {
            System.out.println("Not enough arguments! Use: domainFile problemFile outputFile");
        } else {
            PDDLToSAS(args[0], args[1], args[2]);
        }

        //tryParseAllToClassic();
        //tryParseClassic2SAS();
    }
}
