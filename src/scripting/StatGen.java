/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting;

import fLib.utils.io.FileHandling;
import java.io.File;
import java.io.FileFilter;
import java.util.*;

/**
 *
 * @author FD
 */
public class StatGen {

    private static class Task {

        String name;
        List<Integer> domSizes = new LinkedList<>();
        private int totalPreconditions;
        private int totalEffects;

        float getAveragePreconditionsPerAction() {
            float ret = totalPreconditions;
            ret /= actions;
            return ret;
        }

        float getAverageEffectsPerAction() {
            float ret = totalEffects;
            ret /= actions;
            return ret;
        }

        float getAverageStateVariableSize() {
            float ret = totalEffects;
            for (int i : domSizes) {
                ret += i;
            }
            ret /= domSizes.size();
            return ret;
        }

        float getStateSpaceSize() {
            float ret = 1f;
            for (int i : domSizes) {
                ret *= i;
            }
            return ret;
        }
        int actions = -1;
        int atomsCovered = -1;

        String getHistogram() {
            String out = "";
            Collections.sort(domSizes, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o2 - o1;
                }
            });
            for (int i : domSizes) {
                out += i + "|";
            }
            return out.substring(0, out.length() - 1);
        }
    }

    private static void getPathsRec(File uf, LinkedList<String> paths) {
        if (!uf.exists() || !uf.isDirectory()) {
            return;
        }

        File[] pddls = uf.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.getName().contains(".sas") && !fi.isDirectory();
            }
        });

        File[] dirs = uf.listFiles(new FileFilter() {
            @Override
            public boolean accept(File fi) {
                return fi.isDirectory();
            }
        });

        for (File f : pddls) {
            paths.add(f.getAbsolutePath());
        }

        for (File f : dirs) {
            getPathsRec(f, paths);
        }
    }

    public static void gen(String path, String out) {
        LinkedList<String> paths = new LinkedList<>();
        getPathsRec(new File(path), paths);
        List<Task> tasks = new LinkedList<>();
        for (String s : paths) {
            File f = new File(s);
            Task t = new Task();
            t.name = f.getAbsolutePath();
            String in = FileHandling.getFileContents(f);
            String[] ar = in.split("begin_variable");
            boolean first = true;
            for (String st : ar) {
                if (first) {
                    first = false;
                    continue;
                }
                String pt = st.split("\n")[3];
                t.domSizes.add(Integer.parseInt(pt.trim()));
            }
            t.actions = Integer.parseInt(in.split("end_goal")[1].trim().split("\n")[0].trim());
            tasks.add(t);
            //count the average number of preconditions
            int totalPrecCount = 0, totalEffCount = 0;
            for (String op : in.split("begin_operator")) {
                String arr[] = op.split("\n");
                if (arr[0].contains("begin_version")) {
                    continue;
                }
                int precCount = Integer.parseInt(arr[2].trim());
                int transCount = Integer.parseInt(arr[3 + precCount].trim());
                totalPrecCount += precCount + transCount;
                totalEffCount += transCount;
            }
            t.totalPreconditions = totalPrecCount;
            t.totalEffects = totalEffCount;

        }
        String buf = new String();
        
        buf += "name" + ", "
                    + "domSizes.size" + ", "
                    + "actions" + ", "
                    + "getAveragePreconditionsPerAction" + ", "
                    + "getAverageEffectsPerAction" + ", "
                    + "getAverageStateVariableSize" + ", "
                    + "getStateSpaceSize" + ", "
                    + "getHistogram" + "\n";
        
        for (Task t : tasks) {
            buf += t.name + ", "
                    + t.domSizes.size() + ", "
                    + t.actions + ", "
                    + t.getAveragePreconditionsPerAction() + ", "
                    + t.getAverageEffectsPerAction() + ", "
                    + t.getAverageStateVariableSize() + ", "
                    + t.getStateSpaceSize() + ", "
                    + t.getHistogram() + "\n";
        }
        FileHandling.writeFileOutput(out, buf);
    }

    public static void main(String[] args) {

        gen("C:/ROOT/PROJECTS/pddlParser/data/final/", "domainStatistics.txt");

    }
}
