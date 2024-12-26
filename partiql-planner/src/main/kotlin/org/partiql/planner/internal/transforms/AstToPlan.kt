/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.DataType
import org.partiql.ast.Query
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.errors.TypeCheckException
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.catalog.Identifier
import org.partiql.types.PType
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.IdentifierChain as AstIdentifierChain
import org.partiql.ast.Statement as AstStatement
import org.partiql.planner.internal.ir.Statement as PlanStatement

/**
 * Simple translation from AST to an unresolved algebraic IR.
 */
internal object AstToPlan {

    // statement.toPlan()
    @JvmStatic
    fun apply(statement: AstStatement, env: Env): PlanStatement = statement.accept(ToPlanStatement, env)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToPlanStatement : AstVisitor<PlanStatement, Env>() {

        override fun defaultReturn(node: AstNode, env: Env) = throw IllegalArgumentException("Unsupported statement")

        override fun visitCreateTable(node: CreateTable, env: Env): PlanStatement {
            return DdlConverter.apply(node, env)
        }

        override fun visitQuery(node: Query, env: Env): PlanStatement {
            val rex = when (val expr = node.expr) {
                is ExprQuerySet -> RelConverter.apply(expr, env)
                else -> RexConverter.apply(expr, env)
            }
            return statementQuery(rex)
        }
    }

    // --- Helpers --------------------

    fun convert(identifier: AstIdentifierChain): Identifier {
        val parts = mutableListOf<Identifier.Part>()
        parts.add(part(identifier.root))
        var curStep = identifier.next
        while (curStep != null) {
            parts.add(part(curStep.root))
            curStep = curStep.next
        }
        return Identifier.of(parts)
    }

    fun convert(identifier: AstIdentifier): Identifier {
        return Identifier.of(part(identifier))
    }

    fun part(identifier: AstIdentifier): Identifier.Part = when (identifier.isDelimited) {
        true -> Identifier.Part.delimited(identifier.symbol)
        false -> Identifier.Part.regular(identifier.symbol)
    }

    fun visitType(type: DataType): CompilerType {
        return when (type.code()) {
            // <character string types>
            // TODO CHAR_VARYING, CHARACTER_LARGE_OBJECT, CHAR_LARGE_OBJECT
            DataType.CHARACTER, DataType.CHAR -> {
                val length = type.length ?: 1
                assertGtZeroAndCreate(PType.CHAR, "length", length, PType::character)
            }
            DataType.CHARACTER_VARYING, DataType.VARCHAR -> {
                val length = type.length ?: 1
                assertGtZeroAndCreate(PType.VARCHAR, "length", length, PType::varchar)
            }
            DataType.CLOB -> assertGtZeroAndCreate(PType.CLOB, "length", type.length ?: Int.MAX_VALUE, PType::clob)
            DataType.STRING -> PType.string()
            // <binary large object string type>
            // TODO BINARY_LARGE_OBJECT
            DataType.BLOB -> assertGtZeroAndCreate(PType.BLOB, "length", type.length ?: Int.MAX_VALUE, PType::blob)
            // <bit string type>
            DataType.BIT -> error("BIT is not supported yet.")
            DataType.BIT_VARYING -> error("BIT VARYING is not supported yet.")
            // <numeric types> - <exact numeric types>
            DataType.NUMERIC -> {
                val p = type.precision
                val s = type.scale
                when {
                    p == null && s == null -> PType.decimal(38, 0)
                    p != null && s != null -> {
                        assertParamCompToZero(PType.NUMERIC, "precision", p, false)
                        assertParamCompToZero(PType.NUMERIC, "scale", s, true)
                        if (s > p) {
                            throw TypeCheckException("Numeric scale cannot be greater than precision.")
                        }
                        PType.decimal(type.precision!!, type.scale!!)
                    }
                    p != null && s == null -> {
                        assertParamCompToZero(PType.NUMERIC, "precision", p, false)
                        PType.decimal(p, 0)
                    }
                    else -> error("Precision can never be null while scale is specified.")
                }
            }
            DataType.DEC, DataType.DECIMAL -> {
                val p = type.precision
                val s = type.scale
                when {
                    p == null && s == null -> PType.decimal(38, 0)
                    p != null && s != null -> {
                        assertParamCompToZero(PType.DECIMAL, "precision", p, false)
                        assertParamCompToZero(PType.DECIMAL, "scale", s, true)
                        if (s > p) {
                            throw TypeCheckException("Decimal scale cannot be greater than precision.")
                        }
                        PType.decimal(p, s)
                    }
                    p != null && s == null -> {
                        assertParamCompToZero(PType.DECIMAL, "precision", p, false)
                        PType.decimal(p, 0)
                    }
                    else -> error("Precision can never be null while scale is specified.")
                }
            }
            DataType.BIGINT, DataType.INT8, DataType.INTEGER8 -> PType.bigint()
            DataType.INT4, DataType.INTEGER4, DataType.INTEGER, DataType.INT -> PType.integer()
            DataType.INT2, DataType.SMALLINT -> PType.smallint()
            DataType.TINYINT -> PType.tinyint() // TODO define in parser
            // <numeric type> - <approximate numeric type>
            DataType.FLOAT -> PType.real()
            DataType.REAL -> PType.real()
            DataType.DOUBLE_PRECISION -> PType.doublePrecision()
            // <boolean type>
            DataType.BOOL -> PType.bool()
            // <datetime type>
            DataType.DATE -> PType.date()
            DataType.TIME -> assertGtEqZeroAndCreate(PType.TIME, "precision", type.precision ?: 0, PType::time)
            DataType.TIME_WITH_TIME_ZONE -> assertGtEqZeroAndCreate(PType.TIMEZ, "precision", type.precision ?: 0, PType::timez)
            DataType.TIMESTAMP -> assertGtEqZeroAndCreate(PType.TIMESTAMP, "precision", type.precision ?: 6, PType::timestamp)
            DataType.TIMESTAMP_WITH_TIME_ZONE -> assertGtEqZeroAndCreate(PType.TIMESTAMPZ, "precision", type.precision ?: 6, PType::timestampz)
            // <interval type>
            DataType.INTERVAL -> error("INTERVAL is not supported yet.")
            // <container type>
            DataType.STRUCT -> PType.struct()
            DataType.TUPLE -> PType.struct()
            // <collection type>
            DataType.LIST -> PType.array()
            DataType.BAG -> PType.bag()
            // <user defined type>
            DataType.USER_DEFINED -> TODO("Custom type not supported ")
            else -> error("Unsupported DataType type: $type")
        }.toCType()
    }

    private fun assertGtZeroAndCreate(type: Int, param: String, value: Int, create: (Int) -> PType): PType {
        assertParamCompToZero(type, param, value, false)
        return create.invoke(value)
    }

    private fun assertGtEqZeroAndCreate(type: Int, param: String, value: Int, create: (Int) -> PType): PType {
        assertParamCompToZero(type, param, value, true)
        return create.invoke(value)
    }

    /**
     * @param allowZero when FALSE, this asserts that [value] > 0. If TRUE, this asserts that [value] >= 0.
     */
    private fun assertParamCompToZero(type: Int, param: String, value: Int, allowZero: Boolean) {
        val (result, compString) = when (allowZero) {
            true -> (value >= 0) to "greater than"
            false -> (value > 0) to "greater than or equal to"
        }
        if (!result) {
            throw TypeCheckException("$type $param must be an integer value $compString 0.")
        }
    }
}
