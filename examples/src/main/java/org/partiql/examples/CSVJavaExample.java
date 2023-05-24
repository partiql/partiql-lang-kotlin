package org.partiql.examples;

import org.jetbrains.annotations.NotNull;
import org.partiql.examples.util.Example;
import org.partiql.lang.CompilerPipeline;
import org.partiql.lang.eval.BaseExprValue;
import org.partiql.lang.eval.Bindings;
import org.partiql.lang.eval.EvaluationSession;
import org.partiql.lang.eval.ExprValue;
import org.partiql.lang.eval.ExprValueExtensionsKt;
import org.partiql.lang.eval.ExprValueType;
import org.partiql.lang.eval.Expression;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This example executes a PartiQL query against CSV-formatted data.
 */
public class CSVJavaExample extends Example {

    public CSVJavaExample(@NotNull PrintStream out) {
        super(out);
    }

    /**
     * ExprValue represents values in the context of a PartiQL Expression.
     */
    static class CsvRowExprValue extends BaseExprValue {
        private final String rowString;
        private Map<String, ExprValue> rowValues;

        CsvRowExprValue(final String rowString) {
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
                                    ExprValue exprValue = ExprValue.newString(split[index]);
                                    return ExprValueExtensionsKt.namedValue(exprValue, ExprValue.newString("_" + index));
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
    }

    @Override
    public void run() {
        final String CSV = "person_1,32,tag_1" +
                "\nperson_1,27,tag_1" +
                "\nperson_2,24,tag_1,tag_2";

        print("CSV Data:", CSV);

        // CompilerPipeline is the main entry point for the PartiQL lib giving you access to the compiler
        // and value factories
        final CompilerPipeline pipeline = CompilerPipeline.standard();

        final String query = "SELECT * FROM myCsvDocument csv WHERE CAST(csv._1 AS INT) < 30";
        print("PartiQL query:", query);

        // Compiles the query, the resulting expression can be re-used to query multiple data sets
        final Expression selectAndFilter = pipeline.compile(query);

        final EvaluationSession session = EvaluationSession.builder()
                .globals(
                        Bindings.<ExprValue>lazyBindingsBuilder().addBinding("myCsvDocument", () -> {
                            List<CsvRowExprValue> csvValues = Arrays.stream(CSV.split("\n"))
                                    .map(csvLine -> new CsvRowExprValue(csvLine))
                                    .collect(Collectors.toList());
                            return ExprValue.newList(csvValues);
                        }).build()
                ).build();

        final ExprValue selectAndFilterResult = selectAndFilter.eval(session);
        print("PartiQL query result:", selectAndFilterResult);
        // result below
        // <<{_0:"person_1",_1:"27",_2:"tag_1"},{_0:"person_2",_1:"24",_2:"tag_1",_3:"tag_2"}>>
    }
}
