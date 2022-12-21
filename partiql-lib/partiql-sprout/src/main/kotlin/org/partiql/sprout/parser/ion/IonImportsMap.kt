package org.partiql.sprout.parser.ion

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol

/**
 * Construct
 */
internal class IonImportsMap private constructor(private val imports: Map<String, Import>) {

    operator fun get(alias: String): Import? = imports[alias]

    companion object {

        fun build(value: IonStruct?): IonImportsMap {
            if (value == null) {
                return IonImportsMap(emptyMap())
            }
            val imports = value.map {
                if (it !is IonList) {
                    error("import map values must be of type IonList")
                }
                when (it.fieldName) {
                    "kotlin" -> it.kotlin()
                    else -> error("import target `${it.fieldName}` is unsupported")
                }
            }
            // KISS hack since only Kotlin is supported
            return IonImportsMap(imports.first())
        }

        /**
         * Parses an import using the Java ServiceLoader syntax
         */
        private fun IonList.kotlin(): Map<String, Import> = associate {
            assert(it is IonSymbol)
            val symbol = it as IonSymbol
            val alias = symbol.id()
            val parts = symbol.stringValue().split("#")
            assert(parts.size == 2)
            alias to Import(
                namespace = parts[0],
                ids = parts[1].split(".")
            )
        }
    }

    internal class Import(
        val namespace: String,
        val ids: List<String>,
    )
}
