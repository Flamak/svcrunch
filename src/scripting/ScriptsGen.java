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
package scripting;

import fLib.utils.io.FileHandling;
import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.LinkedList;
import org.antlr.runtime.RecognitionException;

/**
 *
 * @author FD
 */
public class ScriptsGen {

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

    public static void helmert2011() {
        StringBuilder s = new StringBuilder();
        s.append("set path=%path%;C:\\BIN\\python33\n");
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File("C:/ROOT/SYNC/TIDEL/data_ipc2011/seq-opt-pddl"), paths);
        for (String path : paths) {
            if(!path.contains("parcprinter")){
                continue;
            }
            s.append("python C:/ROOT/SYNC/TIDEL/data_ipc2011/scripts/fastdownward-translate/translate.py \"");
            String[] parts = path.split("\\\\");
            String name = parts[parts.length - 3] + "-" + parts[parts.length - 1].split("\\.")[0];
            String domain = path.replace(parts[parts.length-1], "domain-"+parts[parts.length-1]);
            s.append(domain).append("\" \"").append(path).append("\"\n");
            s.append("move output.sas \"C:\\ROOT\\SYNC\\TIDEL\\data_ipc2011\\seq-opt-sas\\seq-opt-sas_").append(name).append(".sas\"\n");
        }
        s.append("pause\n");
        FileHandling.writeFileOutput("helmertGen.bat", s.toString());
    }
    
    public static void helmert2008() {
        StringBuilder s = new StringBuilder();
        s.append("set path=%path%;C:\\BIN\\python33\n");
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File("C:/ROOT/SYNC/TIDEL/data_ipc2008/seq-opt-pddl"), paths);
        for (String path : paths) {
            s.append("python C:/ROOT/SYNC/TIDEL/data_ipc2011/scripts/fastdownward-translate/translate.py \"");
            String[] parts = path.split("\\\\");
            String name = parts[parts.length - 2] + "-" + parts[parts.length - 1].split("\\.")[0];
            String domain = path.replace(".pddl", "-domain.pddl");
            s.append(domain).append("\" \"").append(path).append("\"\n");
            s.append("move output.sas \"C:\\ROOT\\SYNC\\TIDEL\\data_ipc2008\\seq-opt-sas\\seq-opt-sas_").append(name).append(".sas\"\n");
        }
        s.append("pause\n");
        FileHandling.writeFileOutput("helmertGen.bat", s.toString());
    }

    public static void generateme(String inputDirectory, String outputDirectory) {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File(inputDirectory), paths);
        //just first three of each rule
        HashMap<String, Integer> mp = new HashMap<>();
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
            test.Test.PDDLToSAS(domName, path, outputDirectory + name + ".sas");
            long diff = System.currentTimeMillis() - start;
            System.out.println("Translating " + path + " took " + diff + "ms");
        }
    }

    public static void main(String[] args) {
        helmert2008();
        /*long now = System.currentTimeMillis();
        String inputDirectory = "C:/ROOT/PROJECTS/pddlParser/data/ipcSVN_2011/seq-opt";
        String outputDirectory = "C:/ROOT/PROJECTS/pddlParser/data/mySAS/";
        if (args.length == 2) {
            inputDirectory = args[0];
            outputDirectory = args[1];
        }
        generateme(inputDirectory, outputDirectory);
        now = System.currentTimeMillis() - now;
        double nowD = now;
        nowD /= 1000;
        System.out.println("Total time: " + nowD);*/
        //generateme(args[0], args[1]);
        //generateme("C:/ROOT/PROJECTS/pddlParser/data/reductions", "C:/ROOT/PROJECTS/pddlParser/data/prekladRedukce/");
    }
}
