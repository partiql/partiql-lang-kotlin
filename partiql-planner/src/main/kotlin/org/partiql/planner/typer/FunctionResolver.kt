package org.partiql.planner.typer

import org.partiql.plan.Plan
import org.partiql.plan.Rex
import org.partiql.planner.Header
import org.partiql.types.PartiQLValueType
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature

/**
 * Function arguments list.
 */
internal typealias Args = List<Rex.Op.Call.Arg>

/**
 * Function signature match with (possibly) implicitly cast(ed) arguments.
 */
internal typealias Match = Pair<FunctionSignature, Args>

/**
 * Logic for matching signatures to arguments.
 */
internal class FunctionResolver(private val header: Header) {

    /**
     * Functions are sorted by precedence (which is not rigorously defined/specified at the moment).
     */
    public fun match(signatures: List<FunctionSignature>, args: Args): Match? {
        for (signature in signatures) {
            val arguments = match(signature, args)
            if (arguments != null) {
                return signature to arguments
            }
        }
        return null
    }

    /**
     * Attempt to match arguments to the parameters; inserting the implicit casts if necessary.
     */
    public fun match(signature: FunctionSignature, args: Args): Args? {
        if (signature.parameters.size != args.size) {
            return null
        }
        val newArgs = mutableListOf<Rex.Op.Call.Arg>()
        for (i in args.indices) {
            val a = args[i]
            val p = signature.parameters[i]
            when {
                (a is Rex.Op.Call.Arg.Type && p is FunctionParameter.V) -> return null
                (a is Rex.Op.Call.Arg.Value && p is FunctionParameter.T) -> return null
                (a is Rex.Op.Call.Arg.Type && p is FunctionParameter.T) -> newArgs.add(a)
                (a is Rex.Op.Call.Arg.Value && p is FunctionParameter.V) -> {
                    val newArg = a.match(p.type)
                    if (newArg != null) {
                        newArgs.add(newArg)
                    } else {
                        return null
                    }
                }
            }
        }
        // we made a match
        return newArgs
    }

    /**
     * Match the argument to the type; returning null if no match.
     */
    private fun Rex.Op.Call.Arg.Value.match(t2: PartiQLValueType): Rex.Op.Call.Arg.Value? {
        val t1 = this.rex.type.toRuntimeType()
        if (t1 == t2) {
            return this
        }
        val cast = header.lookupCast(t1, t2)
        if (cast == null) {
            return null
        }
        return castArg(this, cast)
    }

    internal companion object {

        /**
         * Rewrite this node with the desired CAST.
         */
        public fun castArg(arg: Rex.Op.Call.Arg.Value, cast: FunctionSignature) = Plan.create {
            val type = cast.returns.toStaticType()
            val value = arg.rex
            rexOpCallArgValue(
                rex(
                    type = type,
                    op = rexOpCall(
                        fn = fnResolved(cast),
                        args = listOf(rexOpCallArgValue(value), rexOpCallArgType(type))
                    )
                )
            )
        }
    }
}
