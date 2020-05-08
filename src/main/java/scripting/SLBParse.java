package scripting;

import io.pddl.in.PDDLFactory;
import io.pddl.in.objects.*;
import representations.SAS.SASTask;
import representations.classic.ClassicGroundTask;
import transformation.sas.ClassicToSAS;
import transformation.sas.overcover.PlanningTask;
import transformation.standard.ClassicToClassic_KBReductionNaive;
import transformation.standard.IOToCLassic_KBReduction;
import transformation.standard.IOToClassic;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SLBParse {

    private static IOLiteral createNeg(IOLiteral l){
        IOLiteral ret = new IOLiteral();
        ret.positive = true;
        ret.vars = l.vars;
        ret.mName = "not_"+l.mName;
        return ret;
    }

    private static void removeNegativePreconditions(IOProblem p, IODomain d) {
        // Find all negative precodnitions.
        HashMap<String, IOLiteral> negPrec = new HashMap<>();
        for(IOAction a:d.actions){
            List<IOLiteral> remove = new LinkedList<>();
            for(IOLiteral l:a.conditions){
                if(!l.positive){
                    negPrec.put(l.toString(),l);
                    remove.add(l);
                }
            }
            a.conditions.removeAll(remove);
            for(IOLiteral l:remove){
                a.conditions.add(createNeg(l));
            }
        }
        // Extend with negative literals in effects
        for(IOAction a:d.actions){
            List<IOLiteral> add = new LinkedList<>();
            for(IOLiteral l:a.effects){
                if(negPrec.containsKey(l.toString())){
                    IOLiteral neg = createNeg(l);
                    neg.positive = !l.positive;
                    add.add(neg);
                }
            }
            a.effects.addAll(add);
        }
        // Update domain with negative predicates
        List<IOPredicate> add = new LinkedList<>();
        for(IOPredicate pr:d.predicates){
            for(IOLiteral i:negPrec.values()){
                if(i.mName.equals(pr.mName)){
                    IOPredicate pred = new IOPredicate();
                    pred.mName = "not_"+pr.mName;
                    pred.mVars = pr.mVars;
                    pred.isStatic = pr.isStatic;
                    add.add(pred);
                }
            }
        }
        d.predicates.addAll(add);
        // Update initial state

        // Ground all predicates (positive and negative)
        for(IOLiteral pr:negPrec.values()){
            System.out.println(p.toString());
        }



        int xx = 0;

    }


    public static void main(String[] args){
        long start;
        String domPath = "C:\\repos\\exp-planning\\svc\\ibc2019_domain.pddl";
        String prPath = "C:\\repos\\exp-planning\\svc\\generated_negged.pddl";
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

        removeNegativePreconditions(p,d);

        ClassicGroundTask theTask;

//        start = System.currentTimeMillis();
//        System.out.print("IOToCLassic_KBReduction transforming task...");
//        theTask = IOToCLassic_KBReduction.Transform(p, d);
//        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

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

        start = System.currentTimeMillis();
        System.out.print("ClassicToSAS.TransformH2GreedyCover transforming task...");
        SASTask t00 = ClassicToSAS.TransformH2GreedyCover(theTask);
        System.out.println("[" + ((System.currentTimeMillis() - start) / 1000f) + "s]");

    }
}
