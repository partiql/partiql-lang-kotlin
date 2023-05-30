@file:JvmName("Main")

package org.partiql.examples.util

import org.partiql.examples.CSVJavaExample
import org.partiql.examples.CsvExprValueExample
import org.partiql.examples.CustomFunctionsExample
import org.partiql.examples.CustomProceduresExample
import org.partiql.examples.EvaluationJavaExample
import org.partiql.examples.EvaluationWithBindings
import org.partiql.examples.EvaluationWithLazyBindings
import org.partiql.examples.ParserErrorExample
import org.partiql.examples.ParserExample
import org.partiql.examples.ParserJavaExample
import org.partiql.examples.PartiQLCompilerPipelineExample
import org.partiql.examples.PartiQLCompilerPipelineJavaExample
import org.partiql.examples.PartialEvaluationVisitorTransformExample
import org.partiql.examples.PreventJoinVisitorExample
import org.partiql.examples.S3JavaExample
import org.partiql.examples.SimpleExpressionEvaluation
import java.io.PrintStream

private val examples = mapOf(
    // Java Examples
    CSVJavaExample::class.java.simpleName to CSVJavaExample(System.out),
    S3JavaExample::class.java.simpleName to S3JavaExample(System.out),
    EvaluationJavaExample::class.java.simpleName to EvaluationJavaExample(System.out),
    ParserJavaExample::class.java.simpleName to ParserJavaExample(System.out),
    PartiQLCompilerPipelineJavaExample::class.java.simpleName to PartiQLCompilerPipelineJavaExample(System.out),

    // Kotlin Examples
    CsvExprValueExample::class.java.simpleName to CsvExprValueExample(System.out),
    CustomFunctionsExample::class.java.simpleName to CustomFunctionsExample(System.out),
    CustomProceduresExample::class.java.simpleName to CustomProceduresExample(System.out),
    EvaluationWithBindings::class.java.simpleName to EvaluationWithBindings(System.out),
    EvaluationWithLazyBindings::class.java.simpleName to EvaluationWithLazyBindings(System.out),
    ParserErrorExample::class.java.simpleName to ParserErrorExample(System.out),
    ParserExample::class.java.simpleName to ParserExample(System.out),
    PartialEvaluationVisitorTransformExample::class.java.simpleName to PartialEvaluationVisitorTransformExample(System.out),
    PreventJoinVisitorExample::class.java.simpleName to PreventJoinVisitorExample(System.out),
    SimpleExpressionEvaluation::class.java.simpleName to SimpleExpressionEvaluation(System.out),
    PartiQLCompilerPipelineExample::class.java.simpleName to PartiQLCompilerPipelineExample(System.out)
)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("args must have at least one example name")
        printHelp(System.err)
        System.exit(1)
    }

    args.forEach { exampleName ->
        val example = examples[exampleName] ?: throw RuntimeException("unknown example name: $exampleName")

        println("Running example: $exampleName")
        example.run()
        println("End of example: $exampleName")
        println()
    }
}

fun printHelp(out: PrintStream) {
    out.println("./gradlew :examples:run --args=\"<${examples.keys.joinToString("|")}>\"")
}
