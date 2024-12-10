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
import org.partiql.ast.Literal
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectList
import org.partiql.ast.SelectStar
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprExtract
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprOr
import org.partiql.ast.expr.ExprOverlay
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprPosition
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprRowValue
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprSubstring
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.ExprValues
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.ExprVariant
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.Scope
import org.partiql.ast.expr.TrimSpec
import org.partiql.errors.TypeCheckException
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.builder.plan
import org.partiql.planner.internal.ir.rel
import org.partiql.planner.internal.ir.relBinding
import org.partiql.planner.internal.ir.relOpJoin
import org.partiql.planner.internal.ir.relOpScan
import org.partiql.planner.internal.ir.relOpUnpivot
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallUnresolved
import org.partiql.planner.internal.ir.rexOpCastUnresolved
import org.partiql.planner.internal.ir.rexOpCoalesce
import org.partiql.planner.internal.ir.rexOpCollection
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpNullif
import org.partiql.planner.internal.ir.rexOpPathIndex
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpSelect
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpSubquery
import org.partiql.planner.internal.ir.rexOpTupleUnion
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.internal.utils.DateTimeUtils
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumReader
import org.partiql.types.PType
import org.partiql.value.datetime.DateTimeValue
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.partiql.ast.SetQuantifier as AstSetQuantifier

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr, context: Env): Rex = ToRex.visitExprCoerce(expr, context)

    internal fun applyRel(expr: Expr, context: Env): Rex = expr.accept(ToRex, context)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstVisitor<Rex, Env>() {

        private val COLL_AGG_NAMES = setOf(
            "coll_any",
            "coll_avg",
            "coll_count",
            "coll_every",
            "coll_max",
            "coll_min",
            "coll_some",
            "coll_sum",
        )

        override fun defaultReturn(node: AstNode, context: Env): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        override fun visitExprRowValue(node: ExprRowValue, ctx: Env): Rex {
            val values = node.values.map { visitExprCoerce(it, ctx) }
            val op = rexOpCollection(values)
            return rex(LIST, op) // TODO: We only do this for legacy reasons. This should return a rexOpRow!
        }

        override fun visitExprLit(node: ExprLit, context: Env): Rex {
            val datum = node.lit.toDatum()
            val type = datum.type
            val cType = CompilerType(
                _delegate = type,
                isNullValue = node.lit.code() == Literal.NULL,
                isMissingValue = node.lit.code() == Literal.MISSING
            )
            val op = rexOpLit(datum)
            return rex(cType, op)
        }

        private fun Literal.toDatum(): Datum {
            val lit = this
            return when (lit.code()) {
                Literal.NULL -> Datum.nullValue()
                Literal.MISSING -> Datum.missing()
                Literal.STRING -> Datum.string(lit.stringValue())
                Literal.BOOL -> Datum.bool(lit.booleanValue())
                Literal.EXACT_NUM -> {
                    val dec = lit.bigDecimalValue().round(MathContext(38, RoundingMode.HALF_EVEN))
                    Datum.decimal(dec, dec.precision(), dec.scale())
                }
                Literal.INT_NUM -> {
                    val n = lit.numberValue()
                    // 1st, try parse as int
                    try {
                        val v = n.toInt(10)
                        return Datum.integer(v)
                    } catch (ex: NumberFormatException) {
                        // ignore
                    }

                    // 2nd, try parse as long
                    try {
                        val v = n.toLong(10)
                        return Datum.bigint(v)
                    } catch (ex: NumberFormatException) {
                        // ignore
                    }

                    // 3rd, try parse as BigInteger
                    try {
                        val v = BigInteger(n)
                        val vDecimal = BigDecimal(v)
                        return Datum.decimal(vDecimal, vDecimal.precision(), vDecimal.scale())
                    } catch (ex: NumberFormatException) {
                        throw ex
                    }
                }
                Literal.APPROX_NUM -> {
                    return Datum.doublePrecision(lit.numberValue().toDouble())
                }
                Literal.TYPED_STRING -> {
                    val type = this.dataType()
                    val typedString = this.stringValue()
                    when (type.code()) {
                        DataType.DATE -> {
                            val value = LocalDate.parse(typedString, DateTimeFormatter.ISO_LOCAL_DATE)
                            val date = DateTimeValue.date(value.year, value.monthValue, value.dayOfMonth)
                            return Datum.date(date)
                        }
                        DataType.TIME, DataType.TIME_WITH_TIME_ZONE -> {
                            val time = DateTimeUtils.parseTimeLiteral(typedString)
                            val precision = type.precision ?: 6
                            return Datum.time(time, precision)
                        }
                        DataType.TIMESTAMP, DataType.TIMESTAMP_WITH_TIME_ZONE -> {
                            val timestamp = DateTimeUtils.parseTimestamp(typedString)
                            val precision = type.precision ?: 6
                            val value = timestamp.toPrecision(precision)
                            return Datum.timestamp(value)
                        }
                        else -> error("Unsupported typed literal string: $this")
                    }
                }
                else -> error("Unsupported literal: $this")
            }
        }

        override fun visitExprVariant(node: ExprVariant, ctx: Env): Rex {
            if (node.encoding != "ion") {
                throw IllegalArgumentException("unsupported encoding ${node.encoding}")
            }
            // TODO: Does this result in a Datum of type variant?
            val v = DatumReader.ion(node.value.byteInputStream())
            val datum = v.next() ?: error("Expected a single value")
            v.next()?.let { throw TypeCheckException("Expected a single value") }
            val type = CompilerType(datum.type)
            return rex(type, rexOpLit(datum))
        }

        /**
         * !! IMPORTANT !!
         *
         * This is the top-level visit for handling subquery coercion. The default behavior is to coerce to a scalar.
         * In some situations, ie comparison to complex types we may make assertions on the desired type.
         *
         * It is recommended that every method (except for the exceptional cases) recurse the tree from visitExprCoerce.
         *
         *  - RHS of comparison when LHS is an array or collection expression; and visa-versa
         *  - It is the collection expression of a FROM clause or JOIN
         *  - It is the RHS of an IN predicate
         *  - It is an argument of an OUTER set operator.
         *
         * @param node
         * @param ctx
         * @return
         */
        internal fun visitExprCoerce(node: Expr, ctx: Env, coercion: Rex.Op.Subquery.Coercion = Rex.Op.Subquery.Coercion.SCALAR): Rex {
            val rex = node.accept(this, ctx)
            return when (isSqlSelect(node)) {
                true -> {
                    val select = rex.op as Rex.Op.Select
                    rex(
                        CompilerType(PType.dynamic()),
                        rexOpSubquery(
                            constructor = select.constructor,
                            rel = select.rel,
                            coercion = coercion
                        )
                    )
                }
                false -> rex
            }
        }

        override fun visitExprVarRef(node: ExprVarRef, context: Env): Rex {
            val type = (ANY)
            val identifier = AstToPlan.convert(node.identifierChain)
            val scope = when (node.scope.code()) {
                Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
                else -> error("Unexpected Scope type: ${node.scope}")
            }
            val op = rexOpVarUnresolved(identifier, scope)
            return rex(type, op)
        }

        private fun resolveUnaryOp(symbol: String, rhs: Expr, context: Env): Rex {
            val type = (ANY)
            // Args
            val arg = visitExprCoerce(rhs, context)
            val args = listOf(arg)
            // Fn
            val name = when (symbol) {
                // TODO move hard-coded operator resolution into SPI
                "+" -> "pos"
                "-" -> "neg"
                else -> error("unsupported unary op $symbol")
            }
            val id = Identifier.delimited(name)
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        private fun resolveBinaryOp(lhs: Expr, symbol: String, rhs: Expr, context: Env): Rex {
            val type = (ANY)
            val args = when (symbol) {
                "<", ">",
                "<=", ">=",
                "=", "<>", "!=" -> {
                    when {
                        // Example: [1, 2] < (SELECT a, b FROM t)
                        isLiteralArray(lhs) && isSqlSelect(rhs) -> {
                            val l = visitExprCoerce(lhs, context)
                            val r = visitExprCoerce(rhs, context, Rex.Op.Subquery.Coercion.ROW)
                            listOf(l, r)
                        }
                        // Example: (SELECT a, b FROM t) < [1, 2]
                        isSqlSelect(lhs) && isLiteralArray(rhs) -> {
                            val l = visitExprCoerce(lhs, context, Rex.Op.Subquery.Coercion.ROW)
                            val r = visitExprCoerce(rhs, context)
                            listOf(l, r)
                        }
                        // Example: 1 < 2
                        else -> {
                            val l = visitExprCoerce(lhs, context)
                            val r = visitExprCoerce(rhs, context)
                            listOf(l, r)
                        }
                    }
                }
                // Example: 1 + 2
                else -> {
                    val l = visitExprCoerce(lhs, context)
                    val r = visitExprCoerce(rhs, context)
                    listOf(l, r)
                }
            }
            // Wrap if a NOT, if necessary
            return when (symbol) {
                "<>", "!=" -> {
                    val op = negate(call("eq", *args.toTypedArray()))
                    rex(type, op)
                }
                else -> {
                    val name = when (symbol) {
                        // TODO eventually move hard-coded operator resolution into SPI
                        "<" -> "lt"
                        ">" -> "gt"
                        "<=" -> "lte"
                        ">=" -> "gte"
                        "=" -> "eq"
                        "||" -> "concat"
                        "+" -> "plus"
                        "-" -> "minus"
                        "*" -> "times"
                        "/" -> "divide"
                        "%" -> "modulo"
                        "&" -> "bitwise_and"
                        else -> error("unsupported binary op $symbol")
                    }
                    val id = Identifier.delimited(name)
                    val op = rexOpCallUnresolved(id, args)
                    rex(type, op)
                }
            }
        }

        override fun visitExprOperator(node: ExprOperator, ctx: Env): Rex {
            val lhs = node.lhs
            return if (lhs != null) {
                resolveBinaryOp(lhs, node.symbol, node.rhs, ctx)
            } else {
                resolveUnaryOp(node.symbol, node.rhs, ctx)
            }
        }

        override fun visitExprNot(node: ExprNot, ctx: Env): Rex {
            val type = (ANY)
            // Args
            val arg = visitExprCoerce(node.value, ctx)
            val args = listOf(arg)
            // Fn
            val id = Identifier.delimited("not")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        override fun visitExprAnd(node: ExprAnd, ctx: Env): Rex {
            val type = (ANY)
            val l = visitExprCoerce(node.lhs, ctx)
            val r = visitExprCoerce(node.rhs, ctx)
            val args = listOf(l, r)

            // Wrap if a NOT, if necessary
            val id = Identifier.delimited("and")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        override fun visitExprOr(node: ExprOr, ctx: Env): Rex {
            val type = (ANY)
            val l = visitExprCoerce(node.lhs, ctx)
            val r = visitExprCoerce(node.rhs, ctx)
            val args = listOf(l, r)

            // Wrap if a NOT, if necessary
            val id = Identifier.delimited("or")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        private fun isLiteralArray(node: Expr): Boolean = node is ExprArray

        private fun isSqlSelect(node: Expr): Boolean {
            return if (node is ExprQuerySet) {
                val body = node.body
                body is QueryBody.SFW && (body.select is SelectList || body.select is SelectStar)
            } else {
                false
            }
        }

        override fun visitExprPath(node: ExprPath, context: Env): Rex {
            // Args
            val root = visitExprCoerce(node.root, context)

            // Attempt to create qualified identifier
            val (newRoot, nextStep) = when (val op = root.op) {
                is Rex.Op.Var.Unresolved -> {
                    // convert consecutive symbol path steps to the root identifier
                    var i = 0
                    val parts = mutableListOf<Identifier.Part>()
                    parts.addAll(op.identifier.getParts())
                    var curStep = node.next
                    while (curStep != null) {
                        if (curStep !is PathStep.Field) {
                            break
                        }
                        parts.add(AstToPlan.part(curStep.field))
                        i += 1
                        curStep = curStep.next
                    }
                    val newRoot = rex(ANY, rexOpVarUnresolved(Identifier.of(parts), op.scope))
                    val newSteps = curStep
                    newRoot to newSteps
                }
                else -> {
                    root to node.next
                }
            }

            if (nextStep == null) {
                return newRoot
            }

            val fromList = mutableListOf<Rel>()

            var varRefIndex = 0 // tracking var ref index

            var curStep = nextStep
            var curPathNavi = newRoot
            while (curStep != null) {
                val path = when (curStep) {
                    is PathStep.Element -> {
                        val key = visitExprCoerce(curStep.element, context)
                        val op = when (val astKey = curStep.element) {
                            is ExprLit -> when (astKey.lit.code()) {
                                Literal.STRING -> rexOpPathKey(curPathNavi, key)
                                else -> rexOpPathIndex(curPathNavi, key)
                            }
                            is ExprCast -> when (astKey.asType.code() == DataType.STRING) {
                                true -> rexOpPathKey(curPathNavi, key)
                                false -> rexOpPathIndex(curPathNavi, key)
                            }
                            else -> rexOpPathIndex(curPathNavi, key)
                        }
                        op
                    }

                    is PathStep.Field -> {
                        when (curStep.field.isDelimited) {
                            true -> {
                                // case-sensitive path step becomes a key lookup
                                rexOpPathKey(curPathNavi, rexString(curStep.field.symbol))
                            }
                            false -> {
                                // case-insensitive path step becomes a symbol lookup
                                rexOpPathSymbol(curPathNavi, curStep.field.symbol)
                            }
                        }
                    }

                    // Unpivot and Wildcard steps trigger the rewrite
                    // According to spec Section 4.3
                    // ew1p1...wnpn
                    // rewrite to:
                    //  SELECT VALUE v_n.p_n
                    //  FROM
                    //       u_1 e as v_1
                    //       u_2 @v_1.p_1 as v_2
                    //       ...
                    //       u_n @v_(n-1).p_(n-1) as v_n
                    //  The From clause needs to be rewritten to
                    //                     Join <------------------- schema: [(k_1), v_1, (k_2), v_2, ..., (k_(n-1)) v_(n-1)]
                    //                  /       \
                    //               ...     un @v_(n-1).p_(n-1) <-- stack: [global, typeEnv: [outer: [global], schema: [(k_1), v_1, (k_2), v_2, ..., (k_(n-1)) v_(n-1)]]]
                    //                Join  <----------------------- schema: [(k_1), v_1, (k_2), v_2, (k_3), v_3]
                    //              /    \
                    //                   u_2 @v_1.p_1 as v2 <------- stack: [global, typeEnv: [outer: [global], schema: [(k_1), v_1, (k_2), v_2]]]
                    //          JOIN   <---------------------------- schema: [(k_1), v_1, (k_2), v_2]
                    //          /          \
                    //   u_1 e as v_1 < ----\----------------------- stack: [global]
                    //                    u_2 @v_1.p_1 as v2 <------ stack: [global, typeEnv: [outer: [global], schema: [(k_1), v_1]]]
                    //   while doing the traversal, instead of passing the stack,
                    //   each join will produce its own schema and pass the schema as a type Env.
                    // The (k_i) indicate the possible key binding produced by unpivot.
                    // We calculate the var ref on the fly.
                    is PathStep.AllFields -> {
                        // Unpivot produces two binding, in this context we want the value,
                        // which always going to be the second binding
                        val op = rexOpVarLocal(1, varRefIndex + 1)
                        varRefIndex += 2
                        val index = fromList.size
                        fromList.add(relFromUnpivot(curPathNavi, index))
                        op
                    }
                    is PathStep.AllElements -> {
                        // Scan produce only one binding
                        val op = rexOpVarLocal(1, varRefIndex)
                        varRefIndex += 1
                        val index = fromList.size
                        fromList.add(relFromDefault(curPathNavi, index))
                        op
                    }
                    else -> error("Unexpected PathStep type: $curStep")
                }
                curStep = curStep.next
                curPathNavi = rex(ANY, path)
            }
            if (fromList.size == 0) return curPathNavi
            val fromNode = fromList.reduce { acc, scan ->
                val schema = acc.type.schema + scan.type.schema
                val props = emptySet<Rel.Prop>()
                val type = relType(schema, props)
                rel(type, relOpJoin(acc, scan, rex(BOOL, rexOpLit(Datum.bool(true))), Rel.Op.Join.Type.INNER))
            }

            // compute the ref used by select construct
            // always going to be the last binding
            val selectRef = fromNode.type.schema.size - 1

            val constructor = when (val op = curPathNavi.op) {
                is Rex.Op.Path.Index -> rex(curPathNavi.type, rexOpPathIndex(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Path.Key -> rex(curPathNavi.type, rexOpPathKey(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Path.Symbol -> rex(curPathNavi.type, rexOpPathSymbol(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Var.Local -> rex(curPathNavi.type, rexOpVarLocal(0, selectRef))
                else -> throw IllegalStateException()
            }
            val op = rexOpSelect(constructor, fromNode)
            return rex(ANY, op)
        }

        override fun visitExprValues(node: ExprValues, ctx: Env): Rex {
            val rows = node.rows.map { visitExprCoerce(it, ctx) }
            return rex(BAG, rexOpCollection(rows))
        }

        /**
         * Construct Rel(Scan([path])).
         *
         * The constructed rel would produce one binding: _v$[index]
         */
        private fun relFromDefault(path: Rex, index: Int): Rel {
            val schema = listOf(
                relBinding(
                    name = "_v$index", // fresh variable
                    type = path.type
                )
            )
            val props = emptySet<Rel.Prop>()
            val relType = relType(schema, props)
            return rel(relType, relOpScan(path))
        }

        /**
         * Construct Rel(Unpivot([path])).
         *
         * The constructed rel would produce two bindings: _k$[index] and _v$[index]
         */
        private fun relFromUnpivot(path: Rex, index: Int): Rel {
            val schema = listOf(
                relBinding(
                    name = "_k$index", // fresh variable
                    type = STRING
                ),
                relBinding(
                    name = "_v$index", // fresh variable
                    type = path.type
                )
            )
            val props = emptySet<Rel.Prop>()
            val relType = relType(schema, props)
            return rel(relType, relOpUnpivot(path))
        }

        private fun rexString(str: String) = rex(STRING, rexOpLit(Datum.string(str)))

        override fun visitExprCall(node: ExprCall, context: Env): Rex {
            val type = (ANY)
            // Fn
            val id = AstToPlan.convert(node.function)
            if (id.hasQualifier()) {
                error("Qualified function calls are not currently supported.")
            }
            if (id.matches("TUPLEUNION")) {
                return visitExprCallTupleUnion(node, context)
            }
            if (id.matches("EXISTS", ignoreCase = true)) {
                return visitExprCallExists(node, context)
            }
            // Args
            val args = node.args.map { visitExprCoerce(it, context) }

            // Check if function is actually coll_<agg>
            if (isCollAgg(node)) {
                return callToCollAgg(id, node.setq, args)
            }

            if (node.setq != null) {
                error("Currently, only COLL_<AGG> may use set quantifiers.")
            }
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        /**
         * @return whether call is `COLL_<AGG>`.
         */
        private fun isCollAgg(node: ExprCall): Boolean {
            val fn = node.function
            val id = if (fn.next == null) {
                // is not a qualified identifier chain
                node.function.root
            } else {
                return false
            }
            return COLL_AGG_NAMES.contains(id.symbol.lowercase())
        }

        /**
         * Converts COLL_<AGG> to the relevant function calls. For example:
         * - `COLL_SUM(x)` becomes `coll_sum_all(x)`
         * - `COLL_SUM(ALL x)` becomes `coll_sum_all(x)`
         * - `COLL_SUM(DISTINCT x)` becomes `coll_sum_distinct(x)`
         *
         * It is assumed that the [id] has already been vetted by [isCollAgg].
         */
        private fun callToCollAgg(id: Identifier, setQuantifier: AstSetQuantifier?, args: List<Rex>): Rex {
            if (id.hasQualifier()) {
                error("Qualified function calls are not currently supported.")
            }
            if (args.size != 1) {
                error("Aggregate calls currently only support single arguments. Received ${args.size} arguments.")
            }
            val postfix = when (setQuantifier?.code()) {
                AstSetQuantifier.DISTINCT -> "_distinct"
                AstSetQuantifier.ALL -> "_all"
                null -> "_all"
                else -> error("Unexpected SetQuantifier type: $setQuantifier")
            }
            val newId = Identifier.regular(id.getIdentifier().getText() + postfix)
            val op = Rex.Op.Call.Unresolved(newId, listOf(args[0]))
            return Rex(ANY, op)
        }

        private fun visitExprCallTupleUnion(node: ExprCall, context: Env): Rex {
            val type = (STRUCT)
            val args = node.args.map { visitExprCoerce(it, context) }.toMutableList()
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        /**
         * Assume that the node's identifier refers to EXISTS.
         * TODO: This could be better suited as a dedicated node in the future.
         */
        private fun visitExprCallExists(node: ExprCall, context: Env): Rex {
            val type = (BOOL)
            if (node.args.size != 1) {
                error("EXISTS requires a single argument.")
            }
            val arg = visitExpr(node.args[0], context)
            val op = rexOpCallUnresolved(AstToPlan.convert(node.function), listOf(arg))
            return rex(type, op)
        }

        override fun visitExprCase(node: ExprCase, context: Env) = plan {
            val type = (ANY)
            val rex = when (node.expr) {
                null -> null
                else -> visitExprCoerce(node.expr!!, context) // match `rex
            }

            // Converts AST CASE (x) WHEN y THEN z --> Plan CASE WHEN x = y THEN z
            val id = Identifier.delimited("eq")
            val createBranch: (Rex, Rex) -> Rex.Op.Case.Branch = { condition: Rex, result: Rex ->
                val updatedCondition = when (rex) {
                    null -> condition
                    else -> rex(type, rexOpCallUnresolved(id, listOf(rex, condition)))
                }
                rexOpCaseBranch(updatedCondition, result)
            }

            val branches = node.branches.map {
                val branchCondition = visitExprCoerce(it.condition, context)
                val branchRex = visitExprCoerce(it.expr, context)
                createBranch(branchCondition, branchRex)
            }.toMutableList()

            val defaultRex = when (val default = node.defaultExpr) {
                null -> rex(type = ANY, op = rexOpLit(value = Datum.nullValue()))
                else -> visitExprCoerce(default, context)
            }
            val op = rexOpCase(branches = branches, default = defaultRex)
            rex(type, op)
        }

        override fun visitExprArray(node: ExprArray, ctx: Env): Rex {
            val values = node.values.map { visitExprCoerce(it, ctx) }
            val op = rexOpCollection(values)
            return rex(LIST, op)
        }

        override fun visitExprBag(node: ExprBag, ctx: Env): Rex {
            val values = node.values.map { visitExprCoerce(it, ctx) }
            val op = rexOpCollection(values)
            return rex(BAG, op)
        }

        override fun visitExprStruct(node: ExprStruct, context: Env): Rex {
            val type = (STRUCT)
            val fields = node.fields.map {
                val k = visitExprCoerce(it.name, context)
                val v = visitExprCoerce(it.value, context)
                rexOpStructField(k, v)
            }
            val op = rexOpStruct(fields)
            return rex(type, op)
        }

        // SPECIAL FORMS

        /**
         * <arg0> NOT? LIKE <arg1> ( ESCAPE <arg2>)?
         */
        override fun visitExprLike(node: ExprLike, ctx: Env): Rex {
            val type = BOOL
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = visitExprCoerce(node.pattern, ctx)
            val arg2 = node.escape?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            var call = when (arg2) {
                null -> call("like", arg0, arg1)
                else -> call("like_escape", arg0, arg1, arg2)
            }
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            return rex(type, call)
        }

        /**
         * <arg0> NOT? BETWEEN <arg1> AND <arg2>
         */
        override fun visitExprBetween(node: ExprBetween, ctx: Env): Rex = plan {
            val type = BOOL
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = visitExprCoerce(node.from, ctx)
            val arg2 = visitExprCoerce(node.to, ctx)
            // Call
            var call = call("between", arg0, arg1, arg2)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            rex(type, call)
        }

        /**
         * <arg0> NOT? IN <arg1>
         *
         * SQL Spec 1999 section 8.4
         * RVC IN IPV is equivalent to RVC = ANY IPV -> Quantified Comparison Predicate
         * Which means:
         * Let the expression be T in C, where C is [a1, ..., an]
         * T in C is true iff T = a_x is true for any a_x in [a1, ...., an]
         * T in C is false iff T = a_x is false for every a_x in [a1, ....., an ] or cardinality of the collection is 0.
         * Otherwise, T in C is unknown.
         *
         */
        override fun visitExprInCollection(node: ExprInCollection, ctx: Env): Rex {
            val type = BOOL
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = node.rhs.accept(this, ctx) // !! don't insert scalar subquery coercions

            // Call
            var call = call("in_collection", arg0, arg1)
            // NOT?
            if (node.not == true) {
                call = negate(call)
            }
            return rex(type, call)
        }

        /**
         * <arg0> IS <NOT>? <type>
         */
        override fun visitExprIsType(node: ExprIsType, ctx: Env): Rex {
            val type = BOOL
            // arg
            val arg0 = visitExprCoerce(node.value, ctx)
            val targetType = node.type
            var call = when (targetType.code()) {
                // <absent types>
                DataType.NULL -> call("is_null", arg0)
                DataType.MISSING -> call("is_missing", arg0)
                // <character string types>
                // TODO CHAR_VARYING, CHARACTER_LARGE_OBJECT, CHAR_LARGE_OBJECT
                DataType.CHARACTER, DataType.CHAR -> call("is_char", targetType.length.toRex(), arg0)
                DataType.CHARACTER_VARYING, DataType.VARCHAR -> call("is_varchar", targetType.length.toRex(), arg0)
                DataType.CLOB -> call("is_clob", arg0)
                DataType.STRING -> call("is_string", targetType.length.toRex(), arg0)
                DataType.SYMBOL -> call("is_symbol", arg0)
                // <binary large object string type>
                // TODO BINARY_LARGE_OBJECT
                DataType.BLOB -> call("is_blob", arg0)
                // <bit string type>
                DataType.BIT -> call("is_bit", arg0) // TODO define in parser
                DataType.BIT_VARYING -> call("is_bitVarying", arg0) // TODO define in parser
                // <numeric types> - <exact numeric types>
                DataType.NUMERIC -> call("is_numeric", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                DataType.DEC, DataType.DECIMAL -> call("is_decimal", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                DataType.BIGINT, DataType.INT8, DataType.INTEGER8 -> call("is_int64", arg0)
                DataType.INT4, DataType.INTEGER4, DataType.INTEGER -> call("is_int32", arg0)
                DataType.INT -> call("is_int", arg0)
                DataType.INT2, DataType.SMALLINT -> call("is_int16", arg0)
                DataType.TINYINT -> call("is_int8", arg0) // TODO define in parser
                // <numeric type> - <approximate numeric type>
                DataType.FLOAT -> call("is_float32", arg0)
                DataType.REAL -> call("is_real", arg0)
                DataType.DOUBLE_PRECISION -> call("is_float64", arg0)
                // <boolean type>
                DataType.BOOLEAN, DataType.BOOL -> call("is_bool", arg0)
                // <datetime type>
                DataType.DATE -> call("is_date", arg0)
                // TODO: DO we want to seperate with time zone vs without time zone into two different type in the plan?
                //  leave the parameterized type out for now until the above is answered
                DataType.TIME -> call("is_time", arg0)
                DataType.TIME_WITH_TIME_ZONE -> call("is_timeWithTz", arg0)
                DataType.TIMESTAMP -> call("is_timestamp", arg0)
                DataType.TIMESTAMP_WITH_TIME_ZONE -> call("is_timestampWithTz", arg0)
                // <interval type>
                DataType.INTERVAL -> call("is_interval", arg0) // TODO define in parser
                // <container type>
                DataType.STRUCT, DataType.TUPLE -> call("is_struct", arg0)
                // <collection type>
                DataType.LIST -> call("is_list", arg0)
                DataType.BAG -> call("is_bag", arg0)
                DataType.SEXP -> call("is_sexp", arg0)
                // <user defined type>
                DataType.USER_DEFINED -> call("is_custom", arg0)
                else -> error("Unexpected DataType type: $targetType")
            }

            if (node.not == true) {
                call = negate(call)
            }

            return rex(type, call)
        }

        override fun visitExprCoalesce(node: ExprCoalesce, ctx: Env): Rex {
            val type = ANY
            val args = node.args.map { arg ->
                visitExprCoerce(arg, ctx)
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        override fun visitExprNullIf(node: ExprNullIf, ctx: Env): Rex {
            val type = ANY
            val v1 = visitExprCoerce(node.v1, ctx)
            val v2 = visitExprCoerce(node.v2, ctx)
            val op = rexOpNullif(v1, v2)
            return rex(type, op)
        }

        /**
         * SUBSTRING(<arg0> (FROM <arg1> (FOR <arg2>)?)? )
         */
        override fun visitExprSubstring(node: ExprSubstring, ctx: Env): Rex {
            val type = ANY
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.start?.let { visitExprCoerce(it, ctx) } ?: rex(INT, rexOpLit(Datum.bigint(1L)))
            val arg2 = node.length?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            val call = when (arg2) {
                null -> call("substring", arg0, arg1)
                else -> call("substring", arg0, arg1, arg2)
            }
            return rex(type, call)
        }

        /**
         * POSITION(<arg0> IN <arg1>)
         */
        override fun visitExprPosition(node: ExprPosition, ctx: Env): Rex {
            val type = ANY
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call
            val call = call("position", arg0, arg1)
            return rex(type, call)
        }

        /**
         * TRIM([LEADING|TRAILING|BOTH]? (<arg1> FROM)? <arg0>)
         */
        override fun visitExprTrim(node: ExprTrim, ctx: Env): Rex {
            val type = STRING
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.chars?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            val call = when (node.trimSpec?.code()) {
                TrimSpec.LEADING -> when (arg1) {
                    null -> call("trim_leading", arg0)
                    else -> call("trim_leading_chars", arg0, arg1)
                }
                TrimSpec.TRAILING -> when (arg1) {
                    null -> call("trim_trailing", arg0)
                    else -> call("trim_trailing_chars", arg0, arg1)
                }
                // TODO: We may want to add a trim_both for trim(BOTH FROM arg)
                else -> when (arg1) {
                    null -> call("trim", arg0)
                    else -> call("trim_chars", arg0, arg1)
                }
            }
            return rex(type, call)
        }

        /**
         * SQL Spec 1999: Section 6.18 <string value function>
         *
         * <character overlay function> ::=
         *    OVERLAY <left paren> <character value expression>
         *    PLACING <character value expression>
         *    FROM <start position>
         *    [ FOR <string length> ] <right paren>
         *
         * The <character overlay function> is equivalent to:
         *
         *   SUBSTRING ( CV FROM 1 FOR SP - 1 ) || RS || SUBSTRING ( CV FROM SP + SL )
         *
         * Where CV is the first <character value expression>,
         * SP is the <start position>
         * RS is the second <character value expression>,
         * SL is the <string length> if specified, otherwise it is char_length(RS).
         */
        override fun visitExprOverlay(node: ExprOverlay, ctx: Env): Rex {
            val cv = visitExprCoerce(node.value, ctx)
            val sp = visitExprCoerce(node.from, ctx)
            val rs = visitExprCoerce(node.placing, ctx)
            val sl = node.forLength?.let { visitExprCoerce(it, ctx) } ?: rex(ANY, call("char_length", rs))
            val p1 = rex(
                ANY,
                call(
                    "substring",
                    cv,
                    rex(INT4, rexOpLit(Datum.integer(1))),
                    rex(ANY, call("minus", sp, rex(INT4, rexOpLit(Datum.integer(1)))))
                )
            )
            val p2 = rex(ANY, call("concat", p1, rs))
            return rex(
                ANY,
                call(
                    "concat",
                    p2,
                    rex(ANY, call("substring", cv, rex(ANY, call("plus", sp, sl))))
                )
            )
        }

        override fun visitExprExtract(node: ExprExtract, ctx: Env): Rex {
            val call = call("extract_${node.field.name().lowercase()}", visitExprCoerce(node.source, ctx))
            return rex(ANY, call)
        }

        override fun visitExprCast(node: ExprCast, ctx: Env): Rex {
            val type = visitType(node.asType)
            val arg = visitExprCoerce(node.value, ctx)
            return rex(ANY, rexOpCastUnresolved(type, arg))
        }

        private fun visitType(type: DataType): CompilerType {
            return when (type.code()) {
                // <absent types>
                DataType.NULL -> error("Casting to NULL is not supported.")
                DataType.MISSING -> error("Casting to MISSING is not supported.")
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

        override fun visitExprSessionAttribute(node: ExprSessionAttribute, ctx: Env): Rex {
            val type = ANY
            val fn = node.sessionAttribute.name().lowercase()
            val call = call(fn)
            return rex(type, call)
        }

        override fun visitExprQuerySet(node: ExprQuerySet, context: Env): Rex = RelConverter.apply(node, context)

        // Helpers

        private fun negate(call: Rex.Op): Rex.Op.Call {
            val id = Identifier.delimited("not")
            val arg = rex(BOOL, call)
            return rexOpCallUnresolved(id, listOf(arg))
        }

        /**
         * Create a [Rex.Op.Call.Static] node which has a hidden unresolved Function.
         * The purpose of having such hidden function is to prevent usage of generated function name in query text.
         */
        private fun call(name: String, vararg args: Rex): Rex.Op.Call {
            val id = Identifier.regular(name)
            return rexOpCallUnresolved(id, args.toList())
        }

        private fun Int?.toRex(): Rex {
            return when (this) {
                null -> rex(INT4, rexOpLit(Datum.nullValue(PType.integer())))
                else -> rex(INT4, rexOpLit(Datum.integer(this)))
            }
        }

        private val ANY: CompilerType = CompilerType(PType.dynamic())
        private val BOOL: CompilerType = CompilerType(PType.bool())
        private val STRING: CompilerType = CompilerType(PType.string())
        private val STRUCT: CompilerType = CompilerType(PType.struct())
        private val BAG: CompilerType = CompilerType(PType.bag())
        private val LIST: CompilerType = CompilerType(PType.array())
        private val INT: CompilerType = CompilerType(PType.numeric(38, 0))
        private val INT4: CompilerType = CompilerType(PType.integer())
        private val TIMESTAMP: CompilerType = CompilerType(PType.timestamp(6))
    }
}
