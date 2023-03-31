package org.partiql.sprout.parser.ion

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import org.partiql.sprout.model.Imports

/**
 * Holds the map of targets to import statements to be used by generators
 *
 * @property symbols    All valid import symbols
 * @property map        Map a target to its import statements
 *
 * Note you could use a map entry instead of symbols, but the logic is simpler this way
 */
internal class IonImports private constructor(
    val symbols: Set<String>,
    val map: Map<String, Imports>,
) {

    companion object {

        fun build(value: IonStruct?): IonImports {
            if (value == null) {
                return IonImports(emptySet(), emptyMap())
            }
            val symbols = mutableSetOf<String>()
            val map = value.associate { list ->
                if (list !is IonList) {
                    error("import map values must be of type IonList")
                }
                val target = list.fieldName
                val imports = mutableMapOf<String, String>()
                list.forEach {
                    assert(it is IonSymbol)
                    val v = it as IonSymbol
                    val symbol = v.id()
                    if (imports.containsKey(symbol)) {
                        error("target `$target` defines the import `$symbol` more than once")
                    }
                    symbols.add(symbol)
                    imports[symbol] = v.stringValue()
                }
                target to imports
            }
            validateImports(symbols, map)
            return IonImports(symbols, map)
        }

        /**
         * Validates that each import block defines all types
         *
         * @param symbols
         * @param map
         */
        private fun validateImports(symbols: Set<String>, map: Map<String, Imports>) {
            val errs = mutableListOf<String>()
            map.forEach { (target, imports) ->
                val missing = symbols - imports.keys
                if (missing.isNotEmpty()) {
                    val err = "Import `$target` missing definitions for: ${missing.joinToString()}"
                    errs.add(err)
                }
            }
            if (errs.isNotEmpty()) {
                error(errs.joinToString("; "))
            }
        }
    }
}
