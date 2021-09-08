package org.partiql.testscript.compiler

import com.amazon.ion.*
import org.partiql.testscript.Failure
import org.partiql.testscript.Result
import org.partiql.testscript.Success
import org.partiql.testscript.foldToResult
import org.partiql.testscript.parser.ast.*
import java.io.File

/**
 * Since compile functions don't produce a result but mutate the current compilation environment we use a
 * singleton success value to indicate that the compile function completed successfully.
 */
private val SUCCESS = Success(Unit)

/**
 * Encapsulates the current compile environment which is comprised of:
 * * The current test environment, which can be mutated by a [SetDefaultEnvironmentNode] and must be reset at each new
 * [ModuleNode]
 * * The compiled [TestScriptExpression]'s
 * * Deferred compiled lambdas that must be executed after the whole AST is processed. These are lambdas that make
 * references to other [TestScriptExpression]'s so we execute them at the end to ensure the referred
 * [TestScriptExpression] was generated
 */
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
 * PTS Compiler.
 */
class Compiler(val ion: IonSystem) {
    fun compile(ast: List<ModuleNode>): List<TestScriptExpression> {

        val compileEnvironment = CompileEnvironment(ion.newEmptyStruct())

        val results = ast.flatMap { module ->
            // reinitialize the testEnvironment to empty at each new module.
            // When the PTS compile environment gets more complex we should 
            // replace this by a nested compile environment 
            compileEnvironment.testEnvironment = ion.newEmptyStruct()

            module.nodes.map { node ->
                when (node) {
                    is SetDefaultEnvironmentNode -> compileSetDefaultEnvironmentNode(compileEnvironment, node)
                    is TestNode -> compileTestNode(compileEnvironment, node)

                    // we defer the compilation of the node types below since they
                    // operate on top of compiled test nodes that can be defined in 
                    // another PTS file. This way the PTS file processing order does 
                    // not matter 
                    is SkipListNode -> compileEnvironment.deferSkipList { compileSkipList(compileEnvironment, node) }
                    is AppendTestNode -> compileEnvironment.deferAppendTest { compileAppendTest(compileEnvironment, node) }

                    // we may support nesting modules if we need to have a robust 
                    // include or need more refined scoping rules for testIds 
                    is ModuleNode -> throw UnsupportedOperationException("Nested modules are not supported")
                }
            }
        }

        val deferredResults = compileEnvironment.invokeDeferred()

        val errors = results.union(deferredResults)
                .filterIsInstance<Failure<Unit>>()
                .flatMap { it.errors }

        if (errors.isNotEmpty()) {
            throw CompilerException(errors.map { it.toPtsError() })
        }

        return compileEnvironment.expressions.values.toList()
    }

    /**
     * Changes the current test environment affecting subsequent AST nodes until a new Module is started.
     */
    private fun compileSetDefaultEnvironmentNode(
            compileEnvironment: CompileEnvironment,
            node: SetDefaultEnvironmentNode): Result<Unit> = when (node) {
        is InlineSetDefaultEnvironmentNode -> {
            compileEnvironment.testEnvironment = node.environment

            SUCCESS
        }
        is FileSetDefaultEnvironmentNode -> {
            val dirPath = File(node.scriptLocation.inputName).parent
            val file = File("$dirPath/${node.environmentRelativeFilePath}")
            
            val lazyDatagram = lazy(LazyThreadSafetyMode.PUBLICATION) { ion.loader.load(file) }

            when {
                !file.exists() -> {
                    Failure(FileSetDefaultEnvironmentNotExists(file.absolutePath, node.scriptLocation))
                }
                lazyDatagram.value.size != 1 -> {
                    Failure(FileSetDefaultEnvironmentNotSingleValue(file.absolutePath, node.scriptLocation))
                }
                lazyDatagram.value[0].type != IonType.STRUCT -> {
                    Failure(FileSetDefaultEnvironmentNotStruct(
                            file.absolutePath,
                            lazyDatagram.value[0].type,
                            node.scriptLocation))
                }
                else -> {
                    compileEnvironment.testEnvironment = lazyDatagram.value[0] as IonStruct
                    SUCCESS
                }
            }
        }
    }


    /**
     * Generates and register a [TestExpression] into the compile environment.
     */
    private fun compileTestNode(compileEnvironment: CompileEnvironment, node: TestNode): Result<Unit> {
        val testExpression = TestExpression(
                id = node.id,
                description = node.description,
                statement = node.statement,
                environment = node.environment ?: compileEnvironment.testEnvironment,
                expected = makeExpectedResult(node.expected),
                scriptLocation = node.scriptLocation)

        val expressions = compileEnvironment.expressions
        return if (expressions.containsKey(node.id)) {
            Failure(TestIdNotUniqueError(node.id, node.scriptLocation, expressions[node.id]!!.scriptLocation))
        } else {
            expressions[node.id] = testExpression
            SUCCESS
        }
    }

    // sexp was validated by the parser 
    private fun makeExpectedResult(sexp: IonSexp): ExpectedResult {
        val tag = (sexp[0] as IonSymbol).stringValue()
        return when (tag) {
            "success" -> ExpectedSuccess(sexp[1])
            "error" -> ExpectedError
            else -> throw IllegalArgumentException("Invalid expected s-exp tag: $tag")
        }
    }

    /**
     * Defers the generation and registering of a [AppendedTestExpression] since it must reference a [TestExpression]
     */
    private fun compileAppendTest(compileEnvironment: CompileEnvironment, node: AppendTestNode): Result<Unit> {
        val expressions = compileEnvironment.expressions
        val matcher = node.pattern.toPatternRegex()

        val matched = expressions.filter { (testId, _) -> matcher.matches(testId) }

        if (matched.isEmpty()) {
            return Failure(NoTestMatchForAppendTestError(node.pattern, node.scriptLocation))
        }

        val results = matched.values.map { original ->
            when (original) {
                is TestExpression -> {
                    compileEnvironment.expressions[original.id] = AppendedTestExpression(
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

    /**
     * Defers the generation and registering of a [SkippedTestExpression] since it must reference a [TestExpression]
     */
    private fun compileSkipList(compileEnvironment: CompileEnvironment, node: SkipListNode): Result<Unit> {
        val expressions = compileEnvironment.expressions
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

// TODO decide if we want to only support '.'and '*' or full regex.
private fun String.toPatternRegex(): Regex = this.toRegex()
