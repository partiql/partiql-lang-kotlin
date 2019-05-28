@file:JvmName("Main")

package org.partiql.examples.util

import org.partiql.examples.*
import java.io.PrintStream
import java.lang.RuntimeException


private val examples = mapOf(
        // Java Examples
        CSVJavaExample::class.java.simpleName to CSVJavaExample(System.out),
        S3Example::class.java.simpleName to S3Example(System.out),

        // Kotlin Examples
        CsvExprValueExample::class.java.simpleName to CsvExprValueExample(System.out),
        CustomFunctionsExample::class.java.simpleName to CustomFunctionsExample(System.out),
        EvaluationWithBindings::class.java.simpleName to EvaluationWithBindings(System.out),
        EvaluationWithLazyBindings::class.java.simpleName to EvaluationWithLazyBindings(System.out),
        ParserErrorExample::class.java.simpleName to ParserErrorExample(System.out),
        ParserExample::class.java.simpleName to ParserExample(System.out),
        PartialEvaluationRewriterExample::class.java.simpleName to PartialEvaluationRewriterExample(System.out),
        PreventJoinVisitorExample::class.java.simpleName to PreventJoinVisitorExample(System.out),
        SimpleExpressionEvaluation::class.java.simpleName to SimpleExpressionEvaluation(System.out)
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

