package org.partiql.errors

/** Handles the encountered problem. */
public interface ProblemHandler {
    /** Handles a [problem] */
    public fun handleProblem(problem: Problem)
}
