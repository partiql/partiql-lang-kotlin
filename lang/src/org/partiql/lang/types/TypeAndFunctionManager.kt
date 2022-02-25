package org.partiql.lang.types

import kotlin.reflect.KClass

data class FunctionSignatureForLookup(val funcName: String, val params: List<Type>) {
    // TODO: Define a better hash function
    override fun hashCode(): Int {
        var hashCode = funcName.hashCode()
        params.forEachIndexed { index, type ->
            hashCode += index * 7 * type.typeSignature.typeName.hashCode()
        }
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

/**
 * A singleton instance that resolves the types and functions globally.
 */
object TypeAndFunctionManager {
    val types: HashMap<String, KClass<*>> = HashMap()
    val functions: HashMap<FunctionSignatureForLookup, SymbolTableEntry> = HashMap()

    /**
     * Registers new types. Use this API to register builtin as well as external implementations of [Type]
     */
    fun addTypes(newTypes: Map<String, KClass<*>>) {
        newTypes.forEach { (typeName, typeClass) ->
            // TODO: Assert that all the typeClasses are instance of [Type]
            types[typeName.toLowerCase()] = typeClass
        }
    }

    /**
     * Registers new functions. Use this API to register builtin as well as external implementations of [Type]
     */
    fun addFunctions(newFunctions: List<SymbolTableEntry>) {
        newFunctions.forEach {
            functions[FunctionSignatureForLookup(it.funName, it.argTypes)] = it
        }
    }

    /**
     * Returns a type for a type signature.
     */
    fun getType(typeSignature: TypeSignature): Type {
        TODO()
        // Get a reflection class by looking up the typeSignature.typeName in types map.
        // Then create an instance of this reflection class by passing the typeSignature
        // as an argument to the constructor
    }

    /**
     * Returns a function definition for a given function signature.
     */
    fun getFunction(funcName: String, params: List<Type>): SymbolTableEntry? {
        // Create a unique hash from funcName and params and then look it up
        // TODO: Throw an error if the function signature is not found.
        return functions[FunctionSignatureForLookup(funcName, params)]
    }
}