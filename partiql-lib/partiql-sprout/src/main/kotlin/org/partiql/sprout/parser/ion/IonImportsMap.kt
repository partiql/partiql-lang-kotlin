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
            // KISS hack since only Kotlin is implemented
            return IonImportsMap(imports.first())
        }

        /**
         * Parses an import using the Java ClassLoader syntax
         */
        private fun IonList.kotlin(): Map<String, Import> = associate {
            assert(it is IonSymbol)
            val symbol = it as IonSymbol
            val alias = symbol.id()
            val path = symbol.stringValue()
            val i = path.lastIndexOf(".")
            val packageName = path.substring(0, i)
            val simpleNames = path.substring(i + 1).split("$")
            alias to Import(
                namespace = packageName,
                ids = simpleNames
            )
        }
    }

    internal class Import(
        val namespace: String,
        val ids: List<String>,
    )
}
