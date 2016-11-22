/*
 * Author:  Filip Dvořák <filip@dvorak.fr>
 *
 * Copyright (c) 2012 Filip Dvorak <filip@dvorak.fr>, all rights reserved
 * 
 * Using, publishing or providing further the contents of this file is prohibited 
 * without previous written permission of the author.
 *  
 */
package io.pddl.in.objects;

import java.util.LinkedList;

/**
 *
 * @author FD
 */
public class IOPredicate {
    public String mName = null;
    public LinkedList<IOVariable> mVars = new LinkedList<>(); 
    public boolean isStatic;

    @Override
    public String toString() {
        String ret = mName+"(";
        for(IOVariable v:mVars){
            ret += v+", ";
        }
        if(!mVars.isEmpty()){
            ret = ret.substring(0, ret.length()-2);
        }
        ret += ")";
        return ret;
    }

    public boolean IsDerivedFromMe(IOLiteral f) {
        return f.mName.equals(mName);
    }
    
}
