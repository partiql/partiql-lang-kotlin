/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.util.*

/*
 * WARNING: This whole file is a intended as a non intrusive way to preserve the meta nodes information during
 * evaluation so we can include line number and column number in EvaluationExceptions. This is not a replacement for
 * properly populating the error context
 */

/**
 * Holds the expression line number and column number.
 */
data class NodeMetadata(val line: Long, val column: Long) {
    constructor(struct: IonStruct) : this(struct["line"].longValue(), struct["column"].longValue())

    /**
     * Fill existing errorContext with information present in metadata if that information is not present in the error
     * context already
     *
     * @param errorContext to be filled
     * @return passed in errorContext
     */
    fun fillErrorContext(errorContext: PropertyValueMap): PropertyValueMap {
        if (errorContext[Property.LINE_NUMBER] == null && errorContext[Property.COLUMN_NUMBER] == null) {
            errorContext[Property.LINE_NUMBER] = this.line
            errorContext[Property.COLUMN_NUMBER] = this.column
        }

        return errorContext
    }

    /**
     * creates and fills a new error context with this metadata information
     */
    fun toErrorContext(): PropertyValueMap? {
        return fillErrorContext(PropertyValueMap())
    }

    /**
     * Adds line and column number to the given [PropertyValueMap]
     */
    fun toErrorContext(properties: PropertyValueMap): PropertyValueMap {
        return fillErrorContext(properties)
    }
}

/**
 * Lookup table to find an AST node's metadata after the AST has its meta nodes removed
 */
@Deprecated("NodeMetaDataLookup only exists for backward compatibility with the V0 AST.  " +
            "Please use the V1 AST or ExprNode AST as appropriate.")
class NodeMetadataLookup private constructor() {
    private val lookup: MutableMap<LookupKey, NodeMetadata?> = mutableMapOf()

    /**
     * Lookup key that uses [Object.hashCode]
     */
    private class LookupKey(val value: IonValue) {
        override fun hashCode(): Int {
            return System.identityHashCode(value)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LookupKey

            if (value != other.value) return false

            return true
        }
    }

    operator fun get(key: IonValue) = lookup[LookupKey(key)]

    /**
     * Only mutable during construction in [Companion.extractMetaNode]
     */
    private operator fun set(key: IonValue, value: NodeMetadata?) {
        lookup[LookupKey(key)] = value
    }

    companion object {

        /**
         * Extracts meta nodes form an AST into a lookup table.
         *
         * @param ast AST with meta nodes
         * @return AST without meta nodes and lookup table to access their information
         */
        fun extractMetaNode(ast: IonSexp): Pair<IonSexp, NodeMetadataLookup> {
            val lookup = NodeMetadataLookup()
            val astWithoutMetaNode = ast.extractMetaNodeInto(lookup) as IonSexp

            return Pair(astWithoutMetaNode, lookup)
        }

        /**
         * Visits the AST removing meta nodes and adding their information into the lookup table
         */
        private fun IonValue.extractMetaNodeInto(lookup: NodeMetadataLookup,
                                                 currentMetadata: NodeMetadata? = null): IonValue = when {
            this.isMetaNode() -> {
                // found a new meta node, use it as current
                val metaNode = NodeMetadata(this[2] as IonStruct)

                this[1].extractMetaNodeInto(lookup, metaNode)
            }
            this is IonContainer -> {
                val container: IonContainer = this.map { it.extractMetaNodeInto(lookup, currentMetadata) }

                lookup[container] = currentMetadata
                container
            }
            else -> {
                val value = this.clone()

                lookup[value] = currentMetadata
                value
            }
        }
    }
}

/**
 * A meta node is an SExp with the following format:
 *
 * ```
 * (meta
 *     <annotated-value>
 *     {
 *         line: <number>,
 *         column: <number>
 *     })
 * ```
 */
private fun IonValue.isMetaNode() = this is IonSexp && this.size == 3 && this[0] is IonSymbol && this[0].stringValue() == "meta"

/**
 * Extension to add a map (IonContainer) -> IonContainer. Not in global to scope as there is an IonContainer.map due to
 * [Iterable]
 */
private fun IonContainer.map(transform: (IonValue) -> IonValue): IonContainer = when {
    this.isNullValue -> this.clone()
    this is IonSexp -> {
        val emptySexp = this.system.newEmptySexp()
        emptySexp.setTypeAnnotationSymbols(*this.typeAnnotationSymbols)
        this.mapTo(emptySexp) { transform(it) }
    }
    this is IonList -> {
        val emptyList = this.system.newEmptyList()
        emptyList.setTypeAnnotationSymbols(*this.typeAnnotationSymbols)
        this.mapTo(emptyList) { transform(it) }
    }
    this is IonStruct -> {
        val emptyStruct = this.system.newEmptyStruct()
        emptyStruct.setTypeAnnotationSymbols(*this.typeAnnotationSymbols)
        this.fold(emptyStruct) { acc, it ->
            acc.add(it.fieldNameSymbol, transform(it))
            acc
        }}
    else -> errNoContext("unknown container type $this", internal = true)
}
