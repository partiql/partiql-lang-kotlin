/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.cli.format

import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.pig.runtime.DomainNode

internal object TreeFormatter : NodeFormatter {

    private val EOL = System.lineSeparator()
    private const val INDENTS = "   "
    private const val INDENT_ROOT = "⚬"
    private const val INDENT_PIPE = "──"
    private const val INDENT_T = "├$INDENT_PIPE"
    private const val INDENT_I = "│  "
    private const val INDENT_ELBOW = "└$INDENT_PIPE"

    override fun format(input: DomainNode) = formatSupport(input)

    internal fun format(input: DomainNode, lineSeparator: String) = formatSupport(input, lineSeparator = lineSeparator)

    private data class PropertyInfo(val item: Any?, val param: String? = null)

    internal fun formatSupport(
        input: Any?,
        level: Int = 0,
        levels: Set<Int> = emptySet(),
        isLast: Boolean = true,
        param: String? = null,
        lineSeparator: String = EOL
    ): String = buildString {
        // Build Current Level String
        val name = when (input) {
            null -> "null"
            is Collection<*>, is DomainNode -> input.javaClass.simpleName
            else -> input.toString()
        }
        append(getLead(level, levels, isLast))
        append(' ')
        if (param != null) {
            append(param)
            append(": ")
        }
        append(name)
        append(lineSeparator)

        // Create Children (PropertyInfo)
        val levelsForChildren: MutableSet<Int> = HashSet(levels)
        if (!isLast) { levelsForChildren.add(level - 1) }
        val children = when (input) {
            null -> emptyList()
            is List<*> -> input.mapIndexed { index, child -> PropertyInfo(child, index.toString()) }
            is DomainNode -> {
                val properties = input.properties().map { PropertyInfo(it.get(input), it.name) }.toMutableList()
                moveRelationalOperatorsToBack(properties)
            }
            else -> emptyList()
        }

        // Add Children Strings
        children.forEachIndexed { index, child ->
            append(
                formatSupport(
                    child.item,
                    level + 1,
                    levelsForChildren,
                    index == children.size - 1,
                    child.param,
                    lineSeparator
                )
            )
        }
    }

    private fun moveRelationalOperatorsToBack(input: MutableList<PropertyInfo>): MutableList<PropertyInfo> {
        val sources = input.filter {
            it.item is PartiqlLogical.Bexpr ||
                it.item is PartiqlLogicalResolved.Bexpr ||
                it.item is PartiqlPhysical.Bexpr
        }
        input.removeAll(sources)
        input.addAll(sources)
        return input
    }

    /**
     * Returns the prefix of a level.
     */
    private fun getLead(level: Int, levels: Set<Int>, useElbow: Boolean): String {
        if (level == 0) return INDENT_ROOT
        return buildString {
            for (l in 0 until level - 1) {
                val indent = when (levels.contains(l)) {
                    true -> INDENT_I
                    false -> INDENTS
                }
                append(indent)
            }
            val elbow = when (useElbow) {
                true -> INDENT_ELBOW
                false -> INDENT_T
            }
            append(elbow)
        }
    }
}
