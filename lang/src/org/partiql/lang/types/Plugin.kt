package org.partiql.lang.types

import org.partiql.lang.eval.ExprValue
import kotlin.reflect.KClass

/**
 * The symbol table entry for a function is a unique function signature that is uniquely
 * identified by the name and the order of the arguments and the function definition itself.
 * The function signature also supports overloading the functions.
 * PartiQ's evaluator will look up the symbol table to uniquely a
 * symbol table entry and [invoke] this function to get an [ExprValue].
 */
interface SymbolTableEntry {
    /**
     * Name of the function.
     */
    val funName: String

    /**
     * List of argument types the function accepts in that order.
     */
    val argTypes: List<Type>

    /**
     * The callback invoked by PartiQL's evaluator to execute the function returning an [ExprValue]
     */
    fun invoke(exprValues: List<ExprValue>): ExprValue
}

/**
 * A type and function registry plugin interface to enable external customers to register types and functions supported
 * on these types. Implementers are expected to provide the correct class definitions of [Type] and the functions supported on it.
 */
interface Plugin {

    /**
     * The types are registered as a map of Class definitions keyed by the unique type name.
     * Implementations are expected to have the type names unique in
     * PartiQL's global (built in + User-defined) type system.
     * For example, a user may not define a type with name "STRING" as it is already present in
     * PartiQL's built in types.
     * Implementations are also expected to provide the correct Class definitions of types which implement
     * the [Type] interface.
     *
     */
    fun getTypes(): Map<String, KClass<*>>

    /**
     * The functions supported on the above types are registered as a list of [SymbolTableEntry].
     */
    fun getFunctions(): List<SymbolTableEntry>
}