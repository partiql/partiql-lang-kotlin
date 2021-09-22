package org.partiql.examples;

import com.amazon.ion.IonSystem;
import com.amazon.ion.system.IonSystemBuilder;
import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.Expression;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;

public class Test {
    public static void main (String[] args){
//        final IonSystem ion = IonSystemBuilder.standard().build();
//        final CompilerPipeline pipeline = CompilerPipeline.standard(ion);
//        final EvaluationSession session = EvaluationSession.standard();
//        final String query = "1 + 1";
//        Expression e = pipeline.compile(query);
//        ExprValue result = e.eval(session);
//
//        System.out.println(result);

        //OffsetTime time1 = OffsetTime.parse("10:15:30", DateTimeFormatter.ISO_TIME);
        LocalTime time2 = LocalTime.parse("10:15:30-05:30", DateTimeFormatter.ISO_TIME);
        //System.out.println (time1.getOffset());
    }
}
