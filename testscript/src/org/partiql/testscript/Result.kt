package org.partiql.testscript

import org.partiql.testscript.parser.ScriptLocation

/**
 * Specialized either type to make it easier to accumulate failures when processing test scripts
 */
internal sealed class Result<T>

internal class Success<T>(val value: T) : Result<T>()

internal class Failure<T>(val errors: List<TestScriptError>) : Result<T>() {
    constructor(vararg errors: TestScriptError) : this(errors.toList())
}

internal abstract class TestScriptError {
    protected abstract val scriptLocation: ScriptLocation

    protected abstract val errorMessage: String

    final override fun toString(): String = "$scriptLocation - $errorMessage"

    fun toPtsError(): PtsError = PtsError(scriptLocation, errorMessage)
}

/**
 * Folds an iterable of results into a single [Result]. When there is at least a single failure it merges all
 * errors into a single Failure otherwise it uses `block` to aggregate the success values
 *
 * @param block lambda executed to group all successes into a single result.
 */
internal fun <T, Y> Iterable<Result<T>>.foldToResult(block: (List<Success<T>>) -> Result<Y>): Result<Y> {

    val (successes: List<Result<T>>, failures: List<Result<T>>) = this.partition { it is Success }
    return if (failures.isEmpty()) {
        block(successes.filterIsInstance<Success<T>>())
    } else {
        Failure(failures.filterIsInstance<Failure<T>>().flatMap { it.errors })
    }
}

internal fun <T> Iterable<Result<T>>.foldToResult(): Result<List<T>> {
    return this.foldToResult { successes -> Success(successes.map { it.value }) }
}
