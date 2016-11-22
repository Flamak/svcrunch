package scripting;

import fLib.utils.io.FileHandling;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 * <p/>
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 * <p/>
 * Publishing, providing further or using of this program is prohibited
 * without previous written permission of author. Publishing or providing further
 * of the contents of this file is prohibited without previous written permission
 * of the author.
 * <p/>
 * User: FD
 * Date: 18.9.13
 * Time: 23:03
 */
public class ProduceGraphs {

    public static void gen(String inputFileSAS, String outputGraphFile){
        File in = new File(inputFileSAS);
        String cont = FileHandling.getFileContents(in);

        String out = "graph G {\n";
        String ar[] = cont.split("begin_variable");
        for(int i = 1; i < ar.length; i++){
            String arr[] = ar[i].split("<none of those>")[0].split("Atom");
            LinkedList<String> cluster = new LinkedList<>();
            for(int j = 1; j < arr.length; j++){
                cluster.add(arr[j].trim().replaceAll("[\\(\\),l-]",""));
            }
            for(String one : cluster){
                for(String two : cluster){
                    if(one.compareTo(two) < 0){
                        out += one + " -- " + two + ";\n";
                    }
                }
            }
        }




        out += "}";
        FileHandling.writeFileOutput(outputGraphFile, out);
        int xx = 0;
    }

    public static void main(String[] args) {
        gen("C:/ROOT/PROJECTS/pddlParser/data/mySAS/barman-pfile01-001.sas_greedy.sas", "C:/ROOT/PROJECTS/pddlParser/data/prekladRedukce/test.gv");
    }
}
