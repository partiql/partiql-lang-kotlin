package org.partiql.eval;

import org.partiql.eval.internal.Parameters;
import org.partiql.eval.internal.Scope;
import org.partiql.eval.operator.Record;

/**
 * This class holds the evaluation environment.
 * <br>
 * This class is necessarily PUBLIC, but every method (and field) SHOULD be internal.
 */
public class Environment {

    // !! IMPORTANT — INTERNAL ONLY !!
    private final Parameters parameters;

    /**
     * TODO INTERNALIZE IN SUBSEQUENT PR
     */
    public final Scope scope;

    /**
     * Default constructor with no parameters.
     */
    protected Environment() {
        this.parameters = new Parameters();
        this.scope = new Scope();
    }

    /**
     * Default constructor with parameters.
     */
    protected Environment(Parameters parameters) {
        this.parameters = parameters;
        this.scope = new Scope();
    }

    /**
     * Private constructor with scope; use next.
     * @param parameters    Parameters
     * @param scope         Top-level scope.
     */
    private Environment(Parameters parameters, Scope scope) {
        this.parameters = parameters;
        this.scope = scope;
    }

    /**
     * TODO make operators use push(scope) and use pop() to avoid extra instantiations.
     */
    public Environment push(Record record) {
        Scope next = new Scope(record, this.scope);
        return new Environment(parameters, next);
    }

    @Override
    public String toString() {
        return scope.toString();
    }
}
