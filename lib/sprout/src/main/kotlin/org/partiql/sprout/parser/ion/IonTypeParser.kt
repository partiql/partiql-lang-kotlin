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

import com.amazon.ion.IonContainer
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
        val symbols = IonSymbols.build(definitions)
        val imports = IonImports.build(importsValue)

        return Universe(
            id = id,
            types = definitions.map {
                val ctx = Context(symbols.root, imports)
                Visitor.visit(it, ctx)
            },
            imports = imports.map,
        )
    }

    /**
     * Visitor builds a [TypeDef] graph while tracking scope (`ctx.scope`) in the [IonSymbols].
     *
     * Instead of tracking in the symbol graph, we could just search from the root, then do the BFS. That is simpler.
     */
    private object Visitor : IonVisitor<TypeDef, Context> {

        /**
         * Parse a [TypeDef.Sum] or [TypeDef.Enum]
         */
        override fun visit(v: IonList, ctx: Context): TypeDef = ctx.scope(v) {
            val ref = ctx.ref()
            val type = when {
                v.isEnum() -> TypeDef.Enum(
                    ref = ref,
                    values = v.map { (it as IonSymbol).stringValue() }
                )
                else -> {
                    val (variants, types) = visitSumVariants(v, ctx)
                    TypeDef.Sum(ref, variants, types)
                }
            }
            ctx.define(type)
        }

        /**
         * Parse a [TypeDef.Product]
         */
        override fun visit(v: IonStruct, ctx: Context): TypeDef = ctx.scope(v) {
            val ref = ctx.ref()
            val (props, types) = visitProductProps(v, ctx)
            val type = TypeDef.Product(ref, props, types)
            ctx.define(type)
        }

        /**
         * Returns a pair of the product def properties and children
         */
        private fun visitProductProps(v: IonStruct, ctx: Context): Pair<List<TypeProp>, List<TypeDef>> {
            val props = mutableListOf<TypeProp>()
            val types = mutableListOf<TypeDef>()
            v.forEach { field ->
                when {
                    field.isContainer() -> {
                        // Add all definitions in special container field _: [ ]
                        val subtypes = (field as IonContainer).map { visit(it, ctx) }
                        types.addAll(subtypes)
                    }
                    field.isInline() -> {
                        val (symbol, nullable) = field.ref()
                        // DANGER! Mutate annotations to set the definition id as if it weren't an inline
                        field.setTypeAnnotations(symbol)
                        var def = visit(field, ctx)
                        // DANGER! Add back the dropped "optional" annotation
                        if (nullable) {
                            field.setTypeAnnotations("optional", symbol)
                            def = def.nullable()
                        }
                        val prop = TypeProp.Inline(field.fieldName, def)
                        props.add(prop)
                    }
                    else -> {
                        val prop = TypeProp.Ref(field.fieldName, ctx.resolve(field))
                        props.add(prop)
                    }
                }
            }
            return props to types
        }

        /**
         * Returns a pair of the sum variants and children
         */
        private fun visitSumVariants(v: IonList, ctx: Context): Pair<List<TypeDef>, List<TypeDef>> {
            val variants = mutableListOf<TypeDef>()
            val types = mutableListOf<TypeDef>()
            v.forEach { item ->
                when {
                    item.isContainer() -> {
                        // Add all definitions in special container entry _::[ ]
                        val subtypes = (item as IonContainer).map { visit(it, ctx) }
                        types.addAll(subtypes)
                    }
                    else -> {
                        val variant = visit(item, ctx)
                        variants.add(variant)
                    }
                }
            }
            return variants to types
        }

        override fun defaultVisit(v: IonValue, ctx: Context) = error("cannot parse value $v, expect 'struct' or 'list'")
    }

    /**
     * Context tracks the visitor scope to keep position in the [IonSymbols] graph.
     */
    private class Context(
        private val root: IonSymbols.Node,
        private val imports: IonImports,
    ) {

        private var tip: IonSymbols.Node = root
        private val defs: MutableList<TypeDef> = mutableListOf()

        /**
         * Produce a [TypeRef] for the current position in the [IonSymbols]
         */
        fun ref() = TypeRef.Path(ids = tip.path.toTypedArray())

        fun define(type: TypeDef): TypeDef {
            defs.add(type)
            return type
        }

        /**
         * Track position in the [IonSymbols] for type reference searches
         */
        fun scope(v: IonValue, block: Context.() -> TypeDef): TypeDef {
            val id = v.id()
            tip = tip.children.find { it.id == id } ?: error("could not find symbol `$id`")
            val def = block.invoke(this)
            tip = tip.parent!! // never pop root
            return def
        }

        /**
         * Create a TypeRef by searching the symbol graph
         */
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
            val (symbol, nullable) = v.ref()
            val absolute = symbol.startsWith(".")
            // 1. If absolute, search or err
            if (absolute) {
                val path = symbol.trimStart('.').split(".")
                val node = root.search(path)
                return when {
                    node != null -> {
                        TypeRef.Path(
                            nullable = nullable,
                            ids = (node.path.toTypedArray()),
                        )
                    }
                    path.size == 1 && imports.symbols.contains(path.first()) -> {
                        // Import type reference using '.'
                        TypeRef.Import(path.first(), nullable)
                    }
                    else -> {
                        error("type reference `$symbol` not found")
                    }
                }
            }
            // 2. Attempt as scalar
            try {
                return TypeRef.Scalar(
                    type = ScalarType.valueOf(symbol.uppercase()),
                    nullable = nullable,
                )
            } catch (_: IllegalArgumentException) {
            }
            // 3. Attempt to find the symbol relative to the current position
            val node = tip.search(symbol)
            if (node != null) {
                return TypeRef.Path(
                    nullable = nullable,
                    ids = (node.path.toTypedArray()),
                )
            }
            // 4. Attempt to find the symbol in the imports
            if (imports.symbols.contains(symbol)) {
                return TypeRef.Import(symbol, nullable)
            }
            // 5. Error nothing found
            error("type reference `$symbol` not found")
        }

        /**
         * Resolve the collection type
         */
        private fun resolve(v: IonList): TypeRef {
            val (symbol, nullable) = v.ref()
            return when (symbol.lowercase()) {
                "list" -> {
                    assert(v.size == 1) { "list must have exactly one type" }
                    val t = resolve(v[0])
                    TypeRef.List(t, nullable)
                }
                "set" -> {
                    assert(v.size == 1) { "set must have exactly one type" }
                    val t = resolve(v[0])
                    TypeRef.Set(t, nullable)
                }
                "map" -> {
                    assert(v.size == 2) { "map must have exactly two types" }
                    assert(v[0] is IonSymbol) { "map key type parameter must be a symbol" }
                    val kt = resolve(v[0])
                    val vt = resolve(v[1])
                    assert(kt is TypeRef.Scalar) { "map key type `$kt` must a scalar" }
                    assert(!kt.nullable) { "map key type `$kt` cannot be nullable" }
                    TypeRef.Map(kt as TypeRef.Scalar, vt, nullable)
                }
                else -> error("invalid collection type $symbol; must be one of `list`, `set`, or `map`")
            }
        }

        /**
         * Resolve the fully specified property definition
         */
        private fun resolve(v: IonStruct): TypeRef {
            TODO("fully specified property definitions have not been implemented: $v")
        }
    }
}
