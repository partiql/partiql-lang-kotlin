package org.partiql.examples;

import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import com.amazon.ion.system.IonSystemBuilder;
import org.jetbrains.annotations.NotNull;
import org.partiql.examples.util.Example;
import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.BaseExprValue;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.ExprValueExtensionsKt;
import org.partiql.lang.eval.ExprValueFactory;
import org.partiql.lang.eval.ExprValueType;
import org.partiql.lang.eval.Expression;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CSVJavaExample extends Example {

    public CSVJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    /**
     * ExprValue represents values in the context of a PartiQL Expression.
     */
    static class CsvRowExprValue extends BaseExprValue {
        private final ExprValueFactory valueFactory;
        private final String rowString;
        private Map<String, ExprValue> rowValues;

        CsvRowExprValue(final ExprValueFactory valueFactory, final String rowString) {
            this.valueFactory = valueFactory;
            this.rowString = rowString;
        }

        private Map<String, ExprValue> rowValues() {
            if (rowValues == null) {
                // This is a very simplified CSV parser
                String[] split = rowString.split(",");

                rowValues = IntStream.range(0, split.length).boxed()
                        .collect(Collectors.toMap(
                                index -> "_" + index,
                                index -> {
                                    ExprValue exprValue = valueFactory.newString(split[index]);
                                    return ExprValueExtensionsKt.namedValue(exprValue, valueFactory.newString("_" + index));
                                }
                        ));
            }

            return rowValues;
        }

        @NotNull
        @Override
        public Iterator<ExprValue> iterator() {
            return rowValues().values().iterator();
        }

        @NotNull
        @Override
        public Bindings<ExprValue> getBindings() {
            return Bindings.ofMap(rowValues());
        }

        @NotNull
        @Override
        public ExprValueType getType() {
            return ExprValueType.STRUCT;
        }

        @NotNull
        @Override
        public IonValue getIonValue() {
            IonSystem ionSystem = valueFactory.getIon();

            final IonStruct struct = ionSystem.newEmptyStruct();
            rowValues().forEach((key, value) -> struct.add(key, ionSystem.newString(value.getScalar().stringValue())));
            struct.makeReadOnly();

            return struct;
        }
    }

    @Override
    public void run() {
        final String CSV = "person_1,32,tag_1" +
                "\nperson_1,27,tag_1" +
                "\nperson_2,24,tag_1,tag_2";

        print("CSV Data:", CSV);

        // Initializes the ion system used by PartiQL
        final IonSystem ion = IonSystemBuilder.standard().build();

        // CompilerPipeline is the main entry point for the PartiQL lib giving you access to the compiler
        // and value factories
        final CompilerPipeline pipeline = CompilerPipeline.standard(ion);

        final String query = "SELECT * FROM myCsvDocument csv WHERE CAST(csv._1 AS INT) < 30";
        print("PartiQL query:", query);

        // Compiles the query, the resulting expression can be re-used to query multiple data sets
        final Expression selectAndFilter = pipeline.compile(query);

        final EvaluationSession session = EvaluationSession.builder()
                .globals(
                        Bindings.<ExprValue>lazyBindingsBuilder().addBinding("myCsvDocument", () -> {
                            List<CsvRowExprValue> csvValues = Arrays.stream(CSV.split("\n"))
                                    .map(csvLine -> new CsvRowExprValue(pipeline.getValueFactory(), csvLine))
                                    .collect(Collectors.toList());
                            return pipeline.getValueFactory().newList(csvValues);
                        }).build()
                ).build();

        final ExprValue selectAndFilterResult = selectAndFilter.eval(session);
        print("PartiQL query result:", selectAndFilterResult);
        // result below
        // <<{_0:"person_1",_1:"27",_2:"tag_1"},{_0:"person_2",_1:"24",_2:"tag_1",_3:"tag_2"}>>
    }
}
