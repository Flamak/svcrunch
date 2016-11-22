package scripting;

import fLib.utils.io.FileHandling;

import java.io.File;
import java.util.*;

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
 * Date: 19.9.13
 * Time: 23:56
 */
public class Transposition {

    private static class DataPack{
        int actions, stateVars;
        double dimension;
        String histogram;
    }

    public static void main(String[] args) {
        String st = FileHandling.getFileContents(new File("C:/ROOT/PROJECTS/pddlParser/domainStatistics.txt"));
        String ar[] = st.split("\n");

        HashMap<String, List<DataPack>> mp = new HashMap<>();
        for(String s: ar){
            String name = "-1";
            DataPack pk = new DataPack();
            String[] names = {"barman", "elevators", "floortile", "openstacks", "parcprinter", "pegsol", "transport"};
            for(String str: names){
                if(s.contains(str)){
                    name = str + s.split(str)[1].split("\\.")[0];
                }
            }
            if(name.equals("-1")){
                throw new UnsupportedOperationException();
            }
            if(!mp.containsKey(name)){
                mp.put(name,new LinkedList<DataPack>());
            }
            List l = mp.get(name);
            String arr[] = s.split(", ");
            pk.stateVars = Integer.parseInt(arr[1]);
            pk.actions = Integer.parseInt(arr[2]);
            pk.dimension = Float.parseFloat(arr[3]);
            pk.histogram = arr[4].trim();
            l.add(pk);
        }

        String out = "";
        LinkedList<String> li = new LinkedList<>(mp.keySet());
        Collections.sort(li);
        for(String k: li){
            List<DataPack> l = mp.get(k);
            out += k + ", ";
            for(DataPack p:l){
                out += p.actions + ", " + p.stateVars + /*", " + p.dimension +*/ ", " + p.histogram + ", ";
            }
            out += "\n";
        }

        FileHandling.writeFileOutput("newStats.txt", out);

        //gen("C:/ROOT/PROJECTS/pddlParser/data/final/", "domainStatistics.txt");
    }
}
