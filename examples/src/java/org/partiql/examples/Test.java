package org.partiql.examples;

import com.amazon.ion.IonSystem;
import com.amazon.ion.Timestamp;
import com.amazon.ion.system.IonSystemBuilder;
import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.Expression;

import java.util.TimeZone;

public class Test {
    public static void main (String[] args){
        final IonSystem ion = IonSystemBuilder.standard().build();
        final CompilerPipeline pipeline = CompilerPipeline.standard(ion);
        final Expression selectAndFilter = pipeline.compile(
                "SELECT ? as b1, f.bar FROM foo f WHERE f.bar = ?");
        final EvaluationSession session = EvaluationSession.standard();

        System.out.println(Timestamp.nowZ());
    }
}
