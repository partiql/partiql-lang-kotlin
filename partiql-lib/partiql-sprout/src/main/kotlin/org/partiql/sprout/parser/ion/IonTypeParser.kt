/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.parser.ion

import com.amazon.ion.IonList
import com.amazon.ion.IonStruct
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.sprout.model.ScalarType
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeProp
import org.partiql.sprout.model.TypeRef
import org.partiql.sprout.model.Universe
import org.partiql.sprout.parser.SproutParser

/**
 * Parser for the prototype .ion grammar
 */
internal object IonTypeParser : SproutParser {

    private val ion = IonSystemBuilder.standard().build()

    override fun parse(id: String, input: String): Universe {
        val reader = ion.newReader(input)
        var type = reader.next()

        var importsValue: IonStruct? = null
        val definitions: MutableList<IonValue> = mutableListOf()

        while (type != null) {
            val value = ion.newValue(reader)
            when (val identifier = value.id()) {
                "imports" -> {
                    if (importsValue != null) {
                        error("`imports` has already been set")
                    }
                    if (type != IonType.STRUCT) {
                        error("`imports` must be a struct")
                    }
                    if (definitions.isNotEmpty()) {
                        error("`imports` must appear before all definitions")
                    }
                    importsValue = value as IonStruct
                }
                else -> {
                    if (type != IonType.LIST && type != IonType.STRUCT) {
                        error("type definition `$identifier` must be an IonList or IonStruct")
                    }
                    definitions.add(value)
                }
            }
            type = reader.next()
        }

        // Build the symbol graph prior to parsing definitions
        val symbols = IonSymbolGraph.build(definitions)
        val imports = IonImportsMap.build(importsValue)

        return Universe(
            id = id,
            types = definitions.map {
                val ctx = Context(symbols.root, imports)
                Visitor.visit(it, ctx)!!
            },
            domains = emptyList() // TODO
        )
    }

    /**
     * Visitor builds a [TypeDef] graph while tracking scope (`ctx.scope`) in the [IonSymbolGraph].
     */
    private object Visitor : IonVisitor<TypeDef, Context> {

        override fun visit(v: IonList, ctx: Context) = ctx.scope(v) {
            val ref = ctx.ref()
            val type = when {
                v.isEnum() -> TypeDef.Enum(
                    ref = ref,
                    values = v.map { (it as IonSymbol).stringValue() }
                )
                else -> TypeDef.Sum(
                    ref = ref,
                    variants = v.map { t -> visit(t, ctx)!! }
                )
            }
            ctx.define(type)
        }

        override fun visit(v: IonStruct, ctx: Context) = ctx.scope(v) {
            val properties = v.map {
                val name = it.fieldName
                when {
                    it.isInlineEnum() -> {
                        it.setTypeAnnotations(name)
                        TypeProp.Enum(
                            name = name,
                            def = visit(it as IonList, ctx) as TypeDef.Enum
                        )
                    }
                    else -> TypeProp.Ref(
                        name = name,
                        ref = resolve(it)
                    )
                }
            }
            val type = TypeDef.Product(
                ref = ctx.ref(),
                props = properties,
            )
            ctx.define(type)
        }

        override fun defaultVisit(v: IonValue, ctx: Context) = error("cannot parse value $v")
    }

    /**
     * Context encapsulates mutable state to keep the Visitor stateless
     */
    private class Context(
        private val root: IonSymbolGraph.Node,
        private val imports: IonImportsMap,
    ) {

        private var tip: IonSymbolGraph.Node = root
        private val defs: MutableList<TypeDef> = mutableListOf()

        /**
         * Produce a [TypeRef] for the current position in the [IonSymbolGraph]
         */
        fun ref() = TypeRef.Path(ids = tip.path.toTypedArray())

        fun define(type: TypeDef): TypeDef {
            defs.add(type)
            return type
        }

        /**
         * Track position in the [IonSymbolGraph] for type reference searches
         */
        fun scope(v: IonValue, block: Context.() -> TypeDef): TypeDef {
            val id = v.id()
            tip = tip.children.find { it.id == id }!!
            val def = block.invoke(this)
            tip = tip.parent!! // never pop root
            return def
        }

        fun resolve(v: IonValue): TypeRef = when (v) {
            is IonSymbol -> resolve(v)
            is IonList -> resolve(v)
            is IonStruct -> resolve(v)
            else -> error("cannot resolve type for IonType $v")
        }

        /**
         * Resolve a symbolic reference with the given rules
         */
        private fun resolve(v: IonSymbol): TypeRef {
            val (symbol, absolute, nullable) = symbol(v.stringValue())
            // 1. Attempt as scalar
            try {
                return TypeRef.Scalar(
                    type = ScalarType.valueOf(symbol.toUpperCase()),
                    nullable = nullable,
                )
            } catch (_: IllegalArgumentException) {}
            // 2. Attempt to find the symbol in the definitions
            val node = if (absolute) root.search(symbol) else tip.search(symbol)
            if (node != null) {
                return TypeRef.Path(
                    nullable = nullable,
                    ids = (node.path.toTypedArray()),
                )
            }
            // 3. Attempt to find the symbol in the imports
            val import = imports[symbol]
            if (import != null) {
                return TypeRef.Import(
                    namespace = import.namespace,
                    ids = import.ids.toTypedArray(),
                )
            }
            // 4. Error nothing found
            error("symbol `$symbol` not found")
        }

        /**
         * Resolve the collection type
         */
        private fun resolve(v: IonList): TypeRef {
            val (symbol, _, nullable) = symbol(v.id())
            return when (symbol.toLowerCase()) {
                "list" -> {
                    assert(v.size == 1)
                    val t = resolve(v[0])
                    TypeRef.List(t, nullable)
                }
                "set" -> {
                    assert(v.size == 1)
                    val t = resolve(v[0])
                    TypeRef.Set(t, nullable)
                }
                "map" -> {
                    assert(v.size == 2)
                    val kt = resolve(v[0])
                    val vt = resolve(v[1])
                    assert(kt is TypeRef.Scalar)
                    assert(!kt.nullable)
                    TypeRef.Map(kt as TypeRef.Scalar, vt, nullable)
                }
                else -> error("invalid collection type $symbol; must be one of `list`, `set`, or `map`")
            }
        }

        /**
         * Resolve the fully specified property definition
         */
        private fun resolve(v: IonStruct): TypeRef {
            TODO("fully specified property definitions have not been specified")
        }

        private fun symbol(id: String): Triple<String, Boolean, Boolean> {
            val nullable = id.last() == '?'
            val absolute = id.startsWith(".")
            var symbol = id
            symbol = if (nullable) symbol.dropLast(1) else symbol
            symbol = if (absolute) symbol.drop(1) else symbol
            return Triple(symbol, absolute, nullable)
        }
    }
}
