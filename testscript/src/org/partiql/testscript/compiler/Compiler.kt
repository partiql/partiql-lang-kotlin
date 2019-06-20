package org.partiql.testscript.compiler

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import org.partiql.testscript.*
import org.partiql.testscript.Failure
import org.partiql.testscript.Result
import org.partiql.testscript.Success
import org.partiql.testscript.parser.ast.*

private val SUCCESS = Success(Unit)

private class CompileEnvironment(var testEnvironment: IonStruct) {
    private val skipListDeferred = mutableListOf<() -> Result<Unit>>()
    private val appendDeferred = mutableListOf<() -> Result<Unit>>()

    val expressions = mutableMapOf<String, TestScriptExpression>()

    private fun defer(list: MutableList<() -> Result<Unit>>, compileFunction: () -> Result<Unit>): Success<Unit> {
        list.add(compileFunction)

        return SUCCESS
    }

    fun deferSkipList(compileFunction: () -> Result<Unit>): Success<Unit> = defer(skipListDeferred, compileFunction)

    fun deferAppendTest(compileFunction: () -> Result<Unit>): Success<Unit> = defer(appendDeferred, compileFunction)

    // skip list is executed first since appending a skipped list is no-op.
    // this way if there are multiple appends to the same skipped test they
    // will be no-op regardless of order
    fun invokeDeferred(): List<Result<Unit>> =
            skipListDeferred.union(appendDeferred).map { it.invoke() }
}

/**
 * PTS Compiler 
 */
class Compiler(val ion: IonSystem) {
    fun compile(ast: List<ModuleNode>): List<TestScriptExpression> {

        val cenv = CompileEnvironment(ion.newEmptyStruct())

        val results = ast.flatMap { module ->
            // reinitialize the testEnvironment to empty at each new module
            // when the PTS compile environment gets more complex we should 
            // replace this by a nested compile environment 
            cenv.testEnvironment = ion.newEmptyStruct()

            module.nodes.map { node ->
                when (node) {
                    is SetDefaultEnvironmentNode -> compileSetDefaultEnvironmentNode(cenv, node)
                    is TestNode -> compileTestNode(cenv, node)

                    // we defer the compilation of the node types bellow since they 
                    // operate on top of compiled test nodes that can be defined in 
                    // another PTS file. This way the PTS file processing order does 
                    // not matter 
                    is SkipListNode -> cenv.deferSkipList { compileSkipList(cenv, node) }
                    is AppendTestNode -> cenv.deferAppendTest { compileAppendTest(cenv, node) }

                    // we may support nesting modules if we need to have a robust 
                    // include or need more refined scoping rules for testIds 
                    is ModuleNode -> throw UnsupportedOperationException("Nested modules are not supported")
                }
            }
        }

        val deferredResults = cenv.invokeDeferred()

        val errors = results.union(deferredResults).filterIsInstance<Failure<Unit>>().flatMap { it.errors }

        if (errors.isNotEmpty()) {
            val formattedErrors = errors.joinToString(separator = "\n") { "    $it" }

            throw CompilerException("Errors found when compiling test scripts:\n$formattedErrors")
        }

        return cenv.expressions.values.toList()
    }

    private fun compileSetDefaultEnvironmentNode(cenv: CompileEnvironment, node: SetDefaultEnvironmentNode): Result<Unit> {
        cenv.testEnvironment = node.environment

        return SUCCESS
    }

    private fun compileTestNode(cenv: CompileEnvironment, node: TestNode): Result<Unit> {
        val testExpression = TestExpression(
                id = node.id,
                description = node.description,
                statement = node.statement,
                environment = node.environment ?: cenv.testEnvironment,
                expected = node.expected,
                scriptLocation = node.scriptLocation)

        val expressions = cenv.expressions
        return if (expressions.containsKey(node.id)) {
            Failure(TestIdNotUniqueError(node.id, node.scriptLocation, expressions[node.id]!!.scriptLocation))
        } else {
            expressions[node.id] = testExpression
            SUCCESS
        }
    }

    private fun compileAppendTest(cenv: CompileEnvironment, node: AppendTestNode): Result<Unit> {
        val expressions = cenv.expressions
        val matcher = node.pattern.toPatternRegex()

        val matched = expressions.filter { (testId, _) -> matcher.matches(testId) }

        if (matched.isEmpty()) {
            return Failure(NoTestMatchForAppendTestError(node.pattern, node.scriptLocation))
        }

        val results = matched.values.map { original ->
            when (original) {
                is TestExpression -> {
                    cenv.expressions[original.id] = AppendedTestExpression(
                            original.id,
                            original,
                            node.additionalData,
                            node.scriptLocation)
                    SUCCESS
                }
                is SkippedTestExpression -> {
                    // appending a skipped test is no-op
                    SUCCESS
                }
                is AppendedTestExpression -> {
                    Failure<Unit>(AppendingAppendedTestError(original.id, original.scriptLocation, node.scriptLocation))
                }
            }
        }

        return results.foldToResult { SUCCESS }
    }
    
    private fun compileSkipList(cenv: CompileEnvironment, node: SkipListNode): Result<Unit> {
        val expressions = cenv.expressions
        val matchers = node.patterns.map { it.toPatternRegex() }

        expressions.filter { (testId, _) -> matchers.any { it.matches(testId) } }
                .forEach { (testId, testExpression) ->
                    when (testExpression) {
                        is TestExpression -> {
                            expressions[testId] = SkippedTestExpression(
                                    testId,
                                    testExpression,
                                    node.scriptLocation)
                        }
                        is SkippedTestExpression -> {
                            // no-op
                        }
                        is AppendedTestExpression -> {
                            expressions[testId] = SkippedTestExpression(
                                    testId,
                                    testExpression.original,
                                    node.scriptLocation)
                        }
                    }
                }

        return SUCCESS
    }
}

// TODO decide if we want to only support '.'and '*' or full regex
fun String.toPatternRegex(): Regex = this.toRegex()
