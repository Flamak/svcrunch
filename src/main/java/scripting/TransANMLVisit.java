/*
 * Author:  Filip Dvorak <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvorak <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package scripting;

import fLib.utils.io.FileHandling;
import io.pddl.in.PDDLFactory;
import io.pddl.in.objects.IOLiteral;
import io.pddl.in.objects.IOPredicate;
import io.pddl.in.objects.IOProblem;
import io.pddl.in.objects.IOVariable;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author FD
 */
public class TransANMLVisit {
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
            if (!f.getAbsolutePath().contains("domain")) {
                paths.add(f.getAbsolutePath());
            }
        }

        for (File f : dirs) {
            getPathsRec(f, paths);
        }
    }

    public static void generatemeHierarchy(String inputDirectory, String outputDirectory, String templatePath) {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File(inputDirectory), paths);
        //just first three of each rule
        HashMap<String, Integer> mp = new HashMap<>();
        int cnt = 0;
        for (String path : paths) {
            String[] parts;
            if (path.contains("\\")) {
                parts = path.split("\\\\");
            } else {
                parts = path.split("/");
            }

            String name = parts[parts.length - 3] + "-" + parts[parts.length - 1].split("\\.")[0];

            Integer in = mp.get(parts[parts.length - 3]);
            if (in != null && in >= 21) {
                continue;
            } else {
                if (in == null) {
                    in = 0;
                }
                in++;
                mp.put(parts[parts.length - 3], in);
            }

            String domName = path.replace(parts[parts.length - 1], "domain.pddl");
            File domTest = new File(domName);
            if (!domTest.exists()) {
                domName = path.replace(".pddl", "-domain.pddl");
            }
            domTest = new File(domName);
            if (!domTest.exists()) {
                String arr[];
                if (path.contains("\\")) {
                    arr = path.split("\\\\");
                } else {
                    arr = path.split("/");
                }

                domName = "";
                String problem = "";
                for (String st : arr) {
                    if (st.contains(".pddl")) {
                        problem = st;
                        continue;
                    }
                    domName += st + "/";//"\\";
                }
                domName += "domain-" + problem;
            }

            long start = System.currentTimeMillis();
            IOProblem p = PDDLFactory.parseProblem(path);

            String passObjects = "";
            String elevObjects = "";
            String floorObjects = "";

            for (IOVariable v : p.objects) {
                if (v.mName.startsWith("p")) {
                    passObjects += v.mName + ",";
                } else if (v.mName.startsWith("n")) {
                    floorObjects += v.mName + ",";
                } else {
                    elevObjects += v.mName.replaceAll("-", "") + ",";
                }
            }
            passObjects = passObjects.substring(0, passObjects.length() - 1);
            elevObjects = elevObjects.substring(0, elevObjects.length() - 1);
            floorObjects = floorObjects.substring(0, floorObjects.length() - 1);

            String goals = "";
            String anonymousVars = "";
            for (IOLiteral l : p.goal) {
                String who = l.vars.get(0).mName;
                String where = l.vars.get(1).mName;
                goals += "transport(@"+who+"@, "+who+", @f"+who+"@, "+where+");\n";
            }
            
            String capacityInit = "";
            String pasLocationInit = "";
            String elevLocationInit = "";
            String connectivityInit = "";
            String reachbilityInit = "";
            HashMap<String, Integer> maxCapacity = new HashMap<>();
            //int cntr = 0;
            for (IOPredicate pr : p.predicateInit) {
                if (pr.mName.equals("above")) {
                    connectivityInit += "connected(" + pr.mVars.get(0).mName + "," + pr.mVars.get(1).mName + ") := true;\n";
                    connectivityInit += "connected(" + pr.mVars.get(1).mName + "," + pr.mVars.get(0).mName + ") := true;\n";
                } else if (pr.mName.equals("lift-at")) {
                    elevLocationInit += pr.mVars.get(0).mName.replaceAll("-", "") + ".location := " + pr.mVars.get(1).mName + ";\n";
                } else if (pr.mName.equals("can-hold")) {
                    String nm = pr.mVars.get(0).mName.replaceAll("-", "");
                    Integer i = maxCapacity.get(nm);
                    int value = Integer.parseInt(pr.mVars.get(1).mName.replaceAll("n", ""));
                    if (i == null || i < value) {
                        maxCapacity.put(nm, value);
                    }
                } else if (pr.mName.equals("reachable-floor")) {
                    //reachbilityInit += "reachable(" + pr.mVars.get(0).mName.replaceAll("-", "") + "," + pr.mVars.get(1).mName + ") := true;\n";
                } else if (pr.mName.equals("passenger-at")) {
                    String who = pr.mVars.get(0).mName;
                    String from = "n"+pr.mVars.get(1).mName.replaceAll("n", "");
                    pasLocationInit += pr.mVars.get(0).mName + ".location := n" + Integer.parseInt(pr.mVars.get(1).mName.replaceAll("n", "")) + ";\n";
                    goals = goals.replaceAll("@"+who+"@", "any"+who);
                    goals = goals.replaceAll("@f"+who+"@", from);
                    anonymousVars += "constant Elevator any"+who+";\n";
                    //cntr++;
                }
            }
            for(String f:floorObjects.split(",")){
                 for(String e:elevObjects.split(",")){
                     reachbilityInit += "reachable(" + e + "," + f + ") := true;\n";
                 }
            }
            for (String nm : maxCapacity.keySet()) {
                capacityInit += nm + ".occupancy := " + maxCapacity.get(nm) + ";\n";
            }
    

            String output = FileHandling.getFileContents(new File(templatePath));
            output = output.replaceAll("@capacity_init@", capacityInit);
            output = output.replaceAll("@paslocation_init@", pasLocationInit);
            output = output.replaceAll("@elevlocation_init@", elevLocationInit);
            output = output.replaceAll("@connectivity_init@", connectivityInit);
            output = output.replaceAll("@reachability_init@", reachbilityInit);
            output = output.replaceAll("@elevator_unbound@", anonymousVars);
            output = output.replaceAll("@transports@", goals);
            output = output.replaceAll("@floor_instances@", floorObjects);
            output = output.replaceAll("@elevator_instances@", elevObjects);
            output = output.replaceAll("@pas_instances@", passObjects);
            cnt++;
            String ar[] = path.split("/");
            String counter = ((cnt > 9) ? Integer.toString(cnt) : "0" + cnt);
            FileHandling.writeFileOutput(outputDirectory + "p" + counter + ".anml", output);
            long diff = System.currentTimeMillis() - start;
            System.out.println("Translating " + path + " took " + diff + "ms");

        }
    }
    
    public static void generateme(String inputDirectory, String outputDirectory, String templatePath) {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File(inputDirectory), paths);
        //just first three of each rule
        HashMap<String, Integer> mp = new HashMap<>();
        int cnt = 0;
        for (String path : paths) {
            String[] parts;
            if (path.contains("\\")) {
                parts = path.split("\\\\");
            } else {
                parts = path.split("/");
            }

            String name = parts[parts.length - 3] + "-" + parts[parts.length - 1].split("\\.")[0];

            Integer in = mp.get(parts[parts.length - 3]);
            if (in != null && in >= 21) {
                continue;
            } else {
                if (in == null) {
                    in = 0;
                }
                in++;
                mp.put(parts[parts.length - 3], in);
            }

            String domName = path.replace(parts[parts.length - 1], "domain.pddl");
            File domTest = new File(domName);
            if (!domTest.exists()) {
                domName = path.replace(".pddl", "-domain.pddl");
            }
            domTest = new File(domName);
            if (!domTest.exists()) {
                String arr[];
                if (path.contains("\\")) {
                    arr = path.split("\\\\");
                } else {
                    arr = path.split("/");
                }

                domName = "";
                String problem = "";
                for (String st : arr) {
                    if (st.contains(".pddl")) {
                        problem = st;
                        continue;
                    }
                    domName += st + "/";//"\\";
                }
                domName += "domain-" + problem;
            }

            long start = System.currentTimeMillis();
            IOProblem p = PDDLFactory.parseProblem(path);

            String locationObjects = "";
            String elevObjects = "";
            String floorObjects = "";

            for (IOVariable v : p.objects) {
                locationObjects += v.mName.replaceAll("-","") + ",";
            }
            locationObjects = locationObjects.substring(0, locationObjects.length() - 1);

            String goals = "";
            for (IOLiteral l : p.goal) {
                String who = l.vars.get(0).mName.replaceAll("-","");
                goals += "visited("+who+") == true;\n";
            }

            String visitedInit = "";
            String pasLocationInit = "";
            String locInit = "";
            String connectivityInit = "";
            String reachbilityInit = "";
            HashMap<String, Integer> maxCapacity = new HashMap<>();
            for (IOPredicate pr : p.predicateInit) {
                if (pr.mName.equals("visited")) {
                    String nm = pr.mVars.get(0).mName.replaceAll("-", "");
                    visitedInit += "visited(" + nm + ") := true;\n";
                    //connectivityInit += "connected(" + pr.mVars.get(0).mName + "," + pr.mVars.get(1).mName + ") := true;\n";
                    //connectivityInit += "connected(" + pr.mVars.get(1).mName + "," + pr.mVars.get(0).mName + ") := true;\n";
                } else if (pr.mName.equals("at-robot")) {
                    String nm = pr.mVars.get(0).mName.replaceAll("-", "");
                    locInit += "at := "+nm+ ";\n";
                } else if (pr.mName.equals("can-hold")) {
                    String nm = pr.mVars.get(0).mName.replaceAll("-", "");
                    Integer i = maxCapacity.get(nm);
                    int value = Integer.parseInt(pr.mVars.get(1).mName.replaceAll("n", ""));
                    if (i == null || i < value) {
                        maxCapacity.put(nm, value);
                    }
                } else if (pr.mName.equals("connected")) {
                    connectivityInit += "connected(" + pr.mVars.get(0).mName.replaceAll("-", "") + "," + pr.mVars.get(1).mName.replaceAll("-", "") + ") := true;\n";
                } else if (pr.mName.equals("passenger-at")) {
                    pasLocationInit += pr.mVars.get(0).mName + ".location := n" + Integer.parseInt(pr.mVars.get(1).mName.replaceAll("n", "")) + ";\n";
                }
            }
            for(String f:floorObjects.split(",")){
                 for(String e:elevObjects.split(",")){
                     reachbilityInit += "reachable(" + e + "," + f + ") := true;\n";
                 }
            }
            for (String nm : maxCapacity.keySet()) {
                visitedInit += nm + ".occupancy := " + maxCapacity.get(nm) + ";\n";
            }

            String output = FileHandling.getFileContents(new File(templatePath));
            output = output.replaceAll("@object_init@", locationObjects);
            output = output.replaceAll("@location_init@", locInit);
            output = output.replaceAll("@visited_init@", visitedInit);
            output = output.replaceAll("@connectivity_init@", connectivityInit);
            output = output.replaceAll("@goals@", goals);
            /*output = output.replaceAll("@floor_instances@", floorObjects);
            output = output.replaceAll("@elevator_instances@", elevObjects);
            output = output.replaceAll("@pas_instances@", locationObjects);*/
            cnt++;
            String ar[] = path.split("/");
            String counter = ((cnt > 9) ? Integer.toString(cnt) : "0" + cnt);
            FileHandling.writeFileOutput(outputDirectory + "p" + counter + ".anml", output);
            long diff = System.currentTimeMillis() - start;
            System.out.println("Translating " + path + " took " + diff + "ms");

        }
    }

    public static void main(String[] args) {
        //helmert();
        long now = System.currentTimeMillis();
        String inputDirectory = "C:/ROOT/PROJECTS/fape/FAPE/problems/visitall/pddl/";
        String outputDirectory = "C:/ROOT/PROJECTS/fape/FAPE/problems/visitall/anml/";
        String templatePath = "C:/ROOT/PROJECTS/fape/FAPE/problems/visitall/visitallTemplate.anml";
        if (args.length == 2) {
            inputDirectory = args[0];
            outputDirectory = args[1];
        }
        generateme(inputDirectory, outputDirectory, templatePath);
        //generatemeHierarchy(inputDirectory, outputDirectory, templatePath);
        now = System.currentTimeMillis() - now;
        double nowD = now;
        nowD /= 1000;
        System.out.println("Total time: " + nowD);
    }
}
