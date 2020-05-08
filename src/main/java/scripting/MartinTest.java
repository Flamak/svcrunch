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
package scripting;

import fLib.utils.io.FileHandling;
import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author FD
 */
public class MartinTest {

    public static void rewriteDomain(String pathIn, String pathOut){
        String in = FileHandling.getFileContents(new File(pathIn));
        String out = in.replaceAll(":precon", ":parameters () :precon");
        
        FileHandling.writeFileOutput(pathOut, out);
        int xx = 0;
        
    }
    
    public static void main(String[] args) throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {
        
        
        rewriteDomain("C:/ROOT/PROJECTS/pddlParser/data/martinsudaTest/operator1.pddl", "C:/ROOT/PROJECTS/pddlParser/data/martinsudaTest/mod_operator1.pddl");
        
//        test.Test.PDDLToSAS(
//                "C:/ROOT/PROJECTS/pddlParser/data/martinsudaTest/mod_operator1.pddl",
//                "C:/ROOT/PROJECTS/pddlParser/data/martinsudaTest/facts1.pddl",
//                "C:/ROOT/PROJECTS/pddlParser/data/martinsudaTest/out.sas");
    }
}
