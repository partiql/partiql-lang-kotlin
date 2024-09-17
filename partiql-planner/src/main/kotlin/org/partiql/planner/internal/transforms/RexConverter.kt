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
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
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
import org.partiql.spi.catalog.Identifier
import org.partiql.types.PType
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.ast.Identifier as AstIdentifier

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr, context: Env): Rex = ToRex.visitExprCoerce(expr, context)

    internal fun applyRel(expr: Expr, context: Env): Rex = expr.accept(ToRex, context)

    @OptIn(PartiQLValueExperimental::class)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToRex : AstBaseVisitor<Rex, Env>() {

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

        override fun visitExprLit(node: Expr.Lit, context: Env): Rex {
            val type = CompilerType(
                _delegate = node.value.type.toPType(),
                isNullValue = node.value.isNull,
                isMissingValue = node.value is MissingValue
            )
            val op = rexOpLit(node.value)
            return rex(type, op)
        }

        override fun visitExprIon(node: Expr.Ion, ctx: Env): Rex {
            val value =
                PartiQLValueIonReaderBuilder
                    .standard().build(node.value).read()
            val type = CompilerType(value.type.toPType())
            return rex(type, rexOpLit(value))
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
            val rex = super.visitExpr(node, ctx)
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

        override fun visitExprVar(node: Expr.Var, context: Env): Rex {
            val type = (ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
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

        override fun visitExprOperator(node: Expr.Operator, ctx: Env): Rex {
            val lhs = node.lhs
            return if (lhs != null) {
                resolveBinaryOp(lhs, node.symbol, node.rhs, ctx)
            } else {
                resolveUnaryOp(node.symbol, node.rhs, ctx)
            }
        }

        override fun visitExprNot(node: Expr.Not, ctx: Env): Rex {
            val type = (ANY)
            // Args
            val arg = visitExprCoerce(node.value, ctx)
            val args = listOf(arg)
            // Fn
            val id = Identifier.delimited("not")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        override fun visitExprAnd(node: Expr.And, ctx: Env): Rex {
            val type = (ANY)
            val l = visitExprCoerce(node.lhs, ctx)
            val r = visitExprCoerce(node.rhs, ctx)
            val args = listOf(l, r)

            // Wrap if a NOT, if necessary
            val id = Identifier.delimited("and")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        override fun visitExprOr(node: Expr.Or, ctx: Env): Rex {
            val type = (ANY)
            val l = visitExprCoerce(node.lhs, ctx)
            val r = visitExprCoerce(node.rhs, ctx)
            val args = listOf(l, r)

            // Wrap if a NOT, if necessary
            val id = Identifier.delimited("or")
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        private fun isLiteralArray(node: Expr): Boolean = node is Expr.Collection && (node.type == Expr.Collection.Type.ARRAY || node.type == Expr.Collection.Type.LIST)

        private fun isSqlSelect(node: Expr): Boolean {
            return if (node is Expr.QuerySet) {
                val body = node.body
                body is QueryBody.SFW && (body.select is Select.Project || body.select is Select.Star)
            } else {
                false
            }
        }

        override fun visitExprPath(node: Expr.Path, context: Env): Rex {
            // Args
            val root = visitExprCoerce(node.root, context)

            // Attempt to create qualified identifier
            val (newRoot, newSteps) = when (val op = root.op) {
                is Rex.Op.Var.Unresolved -> {
                    // convert consecutive symbol path steps to the root identifier
                    var i = 0
                    val parts = mutableListOf<Identifier.Part>()
                    parts.addAll(op.identifier.getParts())
                    for (step in node.steps) {
                        if (step !is Expr.Path.Step.Symbol) {
                            break
                        }
                        parts.add(AstToPlan.part(step.symbol))
                        i += 1
                    }
                    val newRoot = rex(ANY, rexOpVarUnresolved(Identifier.of(parts), op.scope))
                    val newSteps = node.steps.subList(i, node.steps.size)
                    newRoot to newSteps
                }
                else -> root to node.steps
            }

            if (newSteps.isEmpty()) {
                return newRoot
            }

            val fromList = mutableListOf<Rel>()

            var varRefIndex = 0 // tracking var ref index

            val pathNavi = newSteps.fold(newRoot) { current, step ->
                val path = when (step) {
                    is Expr.Path.Step.Index -> {
                        val key = visitExprCoerce(step.key, context)
                        val op = when (val astKey = step.key) {
                            is Expr.Lit -> when (astKey.value) {
                                is StringValue -> rexOpPathKey(current, key)
                                else -> rexOpPathIndex(current, key)
                            }

                            is Expr.Cast -> when (astKey.asType is Type.String) {
                                true -> rexOpPathKey(current, key)
                                false -> rexOpPathIndex(current, key)
                            }

                            else -> rexOpPathIndex(current, key)
                        }
                        op
                    }

                    is Expr.Path.Step.Symbol -> {
                        when (step.symbol.caseSensitivity) {
                            AstIdentifier.CaseSensitivity.SENSITIVE -> {
                                // case-sensitive path step becomes a key lookup
                                rexOpPathKey(current, rexString(step.symbol.symbol))
                            }
                            AstIdentifier.CaseSensitivity.INSENSITIVE -> {
                                // case-insensitive path step becomes a symbol lookup
                                rexOpPathSymbol(current, step.symbol.symbol)
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
                    is Expr.Path.Step.Unpivot -> {
                        // Unpivot produces two binding, in this context we want the value,
                        // which always going to be the second binding
                        val op = rexOpVarLocal(1, varRefIndex + 1)
                        varRefIndex += 2
                        val index = fromList.size
                        fromList.add(relFromUnpivot(current, index))
                        op
                    }
                    is Expr.Path.Step.Wildcard -> {
                        // Scan produce only one binding
                        val op = rexOpVarLocal(1, varRefIndex)
                        varRefIndex += 1
                        val index = fromList.size
                        fromList.add(relFromDefault(current, index))
                        op
                    }
                }
                rex(ANY, path)
            }

            if (fromList.size == 0) return pathNavi
            val fromNode = fromList.reduce { acc, scan ->
                val schema = acc.type.schema + scan.type.schema
                val props = emptySet<Rel.Prop>()
                val type = relType(schema, props)
                rel(type, relOpJoin(acc, scan, rex(BOOL, rexOpLit(boolValue(true))), Rel.Op.Join.Type.INNER))
            }

            // compute the ref used by select construct
            // always going to be the last binding
            val selectRef = fromNode.type.schema.size - 1

            val constructor = when (val op = pathNavi.op) {
                is Rex.Op.Path.Index -> rex(pathNavi.type, rexOpPathIndex(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Path.Key -> rex(pathNavi.type, rexOpPathKey(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Path.Symbol -> rex(pathNavi.type, rexOpPathSymbol(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key))
                is Rex.Op.Var.Local -> rex(pathNavi.type, rexOpVarLocal(0, selectRef))
                else -> throw IllegalStateException()
            }
            val op = rexOpSelect(constructor, fromNode)
            return rex(ANY, op)
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

        private fun rexString(str: String) = rex(STRING, rexOpLit(stringValue(str)))

        override fun visitExprCall(node: Expr.Call, context: Env): Rex {
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
        private fun isCollAgg(node: Expr.Call): Boolean {
            val id = node.function as? org.partiql.ast.Identifier.Symbol ?: return false
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
        private fun callToCollAgg(id: Identifier, setQuantifier: SetQuantifier?, args: List<Rex>): Rex {
            if (id.hasQualifier()) {
                error("Qualified function calls are not currently supported.")
            }
            if (args.size != 1) {
                error("Aggregate calls currently only support single arguments. Received ${args.size} arguments.")
            }
            val postfix = when (setQuantifier) {
                SetQuantifier.DISTINCT -> "_distinct"
                SetQuantifier.ALL -> "_all"
                null -> "_all"
            }
            val newId = Identifier.regular(id.getIdentifier().getText() + postfix)
            val op = Rex.Op.Call.Unresolved(newId, listOf(args[0]))
            return Rex(ANY, op)
        }

        private fun visitExprCallTupleUnion(node: Expr.Call, context: Env): Rex {
            val type = (STRUCT)
            val args = node.args.map { visitExprCoerce(it, context) }.toMutableList()
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        /**
         * Assume that the node's identifier refers to EXISTS.
         * TODO: This could be better suited as a dedicated node in the future.
         */
        private fun visitExprCallExists(node: Expr.Call, context: Env): Rex {
            val type = (BOOL)
            if (node.args.size != 1) {
                error("EXISTS requires a single argument.")
            }
            val arg = visitExpr(node.args[0], context)
            val op = rexOpCallUnresolved(AstToPlan.convert(node.function), listOf(arg))
            return rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, context: Env) = plan {
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

            val defaultRex = when (val default = node.default) {
                null -> rex(type = ANY, op = rexOpLit(value = nullValue()))
                else -> visitExprCoerce(default, context)
            }
            val op = rexOpCase(branches = branches, default = defaultRex)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, context: Env): Rex {
            val type = when (node.type) {
                Expr.Collection.Type.BAG -> BAG
                Expr.Collection.Type.ARRAY -> LIST
                Expr.Collection.Type.VALUES -> LIST
                Expr.Collection.Type.LIST -> LIST
                Expr.Collection.Type.SEXP -> SEXP
            }
            val values = node.values.map { visitExprCoerce(it, context) }
            val op = rexOpCollection(values)
            return rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, context: Env): Rex {
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
        override fun visitExprLike(node: Expr.Like, ctx: Env): Rex {
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
        override fun visitExprBetween(node: Expr.Between, ctx: Env): Rex = plan {
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
        override fun visitExprInCollection(node: Expr.InCollection, ctx: Env): Rex {
            val type = BOOL
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExpr(node.rhs, ctx) // !! don't insert scalar subquery coercions

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
        override fun visitExprIsType(node: Expr.IsType, ctx: Env): Rex {
            val type = BOOL
            // arg
            val arg0 = visitExprCoerce(node.value, ctx)

            var call = when (val targetType = node.type) {
                is Type.NullType -> call("is_null", arg0)
                is Type.Missing -> call("is_missing", arg0)
                is Type.Bool -> call("is_bool", arg0)
                is Type.Tinyint -> call("is_int8", arg0)
                is Type.Smallint, is Type.Int2 -> call("is_int16", arg0)
                is Type.Int4 -> call("is_int32", arg0)
                is Type.Bigint, is Type.Int8 -> call("is_int64", arg0)
                is Type.Int -> call("is_int", arg0)
                is Type.Real -> call("is_real", arg0)
                is Type.Float32 -> call("is_float32", arg0)
                is Type.Float64 -> call("is_float64", arg0)
                is Type.Decimal -> call("is_decimal", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                is Type.Numeric -> call("is_numeric", targetType.precision.toRex(), targetType.scale.toRex(), arg0)
                is Type.Char -> call("is_char", targetType.length.toRex(), arg0)
                is Type.Varchar -> call("is_varchar", targetType.length.toRex(), arg0)
                is Type.String -> call("is_string", targetType.length.toRex(), arg0)
                is Type.Symbol -> call("is_symbol", arg0)
                is Type.Bit -> call("is_bit", arg0)
                is Type.BitVarying -> call("is_bitVarying", arg0)
                is Type.ByteString -> call("is_byteString", arg0)
                is Type.Blob -> call("is_blob", arg0)
                is Type.Clob -> call("is_clob", arg0)
                is Type.Date -> call("is_date", arg0)
                is Type.Time -> call("is_time", arg0)
                // TODO: DO we want to seperate with time zone vs without time zone into two different type in the plan?
                //  leave the parameterized type out for now until the above is answered
                is Type.TimeWithTz -> call("is_timeWithTz", arg0)
                is Type.Timestamp -> call("is_timestamp", arg0)
                is Type.TimestampWithTz -> call("is_timestampWithTz", arg0)
                is Type.Interval -> call("is_interval", arg0)
                is Type.Bag -> call("is_bag", arg0)
                is Type.List -> call("is_list", arg0)
                is Type.Sexp -> call("is_sexp", arg0)
                is Type.Tuple -> call("is_struct", arg0)
                is Type.Struct -> call("is_struct", arg0)
                is Type.Any -> call("is_any", arg0)
                is Type.Custom -> call("is_custom", arg0)
            }

            if (node.not == true) {
                call = negate(call)
            }

            return rex(type, call)
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Env): Rex {
            val type = ANY
            val args = node.args.map { arg ->
                visitExprCoerce(arg, ctx)
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        override fun visitExprNullIf(node: Expr.NullIf, ctx: Env): Rex {
            val type = ANY
            val value = visitExprCoerce(node.value, ctx)
            val nullifier = visitExprCoerce(node.nullifier, ctx)
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * SUBSTRING(<arg0> (FROM <arg1> (FOR <arg2>)?)? )
         */
        override fun visitExprSubstring(node: Expr.Substring, ctx: Env): Rex {
            val type = ANY
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.start?.let { visitExprCoerce(it, ctx) } ?: rex(INT, rexOpLit(int64Value(1)))
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
        override fun visitExprPosition(node: Expr.Position, ctx: Env): Rex {
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
        override fun visitExprTrim(node: Expr.Trim, ctx: Env): Rex {
            val type = STRING
            // Args
            val arg0 = visitExprCoerce(node.value, ctx)
            val arg1 = node.chars?.let { visitExprCoerce(it, ctx) }
            // Call Variants
            val call = when (node.spec) {
                Expr.Trim.Spec.LEADING -> when (arg1) {
                    null -> call("trim_leading", arg0)
                    else -> call("trim_leading_chars", arg0, arg1)
                }
                Expr.Trim.Spec.TRAILING -> when (arg1) {
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
        override fun visitExprOverlay(node: Expr.Overlay, ctx: Env): Rex {
            val cv = visitExprCoerce(node.value, ctx)
            val sp = visitExprCoerce(node.start, ctx)
            val rs = visitExprCoerce(node.overlay, ctx)
            val sl = node.length?.let { visitExprCoerce(it, ctx) } ?: rex(ANY, call("char_length", rs))
            val p1 = rex(
                ANY,
                call(
                    "substring",
                    cv,
                    rex(INT4, rexOpLit(int32Value(1))),
                    rex(ANY, call("minus", sp, rex(INT4, rexOpLit(int32Value(1)))))
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

        override fun visitExprExtract(node: Expr.Extract, ctx: Env): Rex {
            val call = call("extract_${node.field.name.lowercase()}", visitExprCoerce(node.source, ctx))
            return rex(ANY, call)
        }

        override fun visitExprCast(node: Expr.Cast, ctx: Env): Rex {
            val type = visitType(node.asType)
            val arg = visitExprCoerce(node.value, ctx)
            return rex(ANY, rexOpCastUnresolved(type, arg))
        }

        private fun visitType(type: Type): CompilerType {
            return when (type) {
                is Type.NullType -> error("Casting to NULL is not supported.")
                is Type.Missing -> error("Casting to MISSING is not supported.")
                is Type.Bool -> PType.bool()
                is Type.Tinyint -> PType.tinyint()
                is Type.Smallint, is Type.Int2 -> PType.smallint()
                is Type.Int4 -> PType.integer()
                is Type.Bigint, is Type.Int8 -> PType.bigint()
                is Type.Int -> PType.numeric()
                is Type.Real -> PType.real()
                is Type.Float32 -> PType.real()
                is Type.Float64 -> PType.doublePrecision()
                is Type.Decimal -> when {
                    type.precision == null && type.scale == null -> PType.decimal()
                    type.precision != null && type.scale != null -> PType.decimal(type.precision!!, type.scale!!)
                    type.precision != null && type.scale == null -> PType.decimal(type.precision!!, 0)
                    else -> error("Precision can never be null while scale is specified.")
                }

                is Type.Numeric -> when {
                    type.precision == null && type.scale == null -> PType.decimal()
                    type.precision != null && type.scale != null -> PType.decimal(type.precision!!, type.scale!!)
                    type.precision != null && type.scale == null -> PType.decimal(type.precision!!, 0)
                    else -> error("Precision can never be null while scale is specified.")
                }

                is Type.Char -> PType.character(type.length ?: 255) // TODO: What is default?
                is Type.Varchar -> error("VARCHAR is not supported yet.")
                is Type.String -> PType.string()
                is Type.Symbol -> PType.symbol()
                is Type.Bit -> error("BIT is not supported yet.")
                is Type.BitVarying -> error("BIT VARYING is not supported yet.")
                is Type.ByteString -> error("BINARY is not supported yet.")
                is Type.Blob -> PType.blob(type.length ?: Int.MAX_VALUE)
                is Type.Clob -> PType.clob(type.length ?: Int.MAX_VALUE)
                is Type.Date -> PType.date()
                is Type.Time -> PType.time(type.precision ?: 6)
                is Type.TimeWithTz -> PType.timez(type.precision ?: 6)
                is Type.Timestamp -> PType.timestamp(type.precision ?: 6)
                is Type.TimestampWithTz -> PType.timestampz(type.precision ?: 6)
                is Type.Interval -> error("INTERVAL is not supported yet.")
                is Type.Bag -> PType.bag()
                is Type.Sexp -> PType.sexp()
                is Type.Any -> PType.dynamic()
                is Type.Custom -> TODO("Custom type not supported ")
                is Type.List -> PType.array()
                is Type.Tuple -> PType.struct()
                is Type.Struct -> PType.struct()
            }.toCType()
        }

        override fun visitExprDateAdd(node: Expr.DateAdd, ctx: Env): Rex {
            val type = TIMESTAMP
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_ADD(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_ADD(TIMEZONE_MINUTE, ...)")
                else -> call("date_add_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprDateDiff(node: Expr.DateDiff, ctx: Env): Rex {
            val type = TIMESTAMP
            // Args
            val arg0 = visitExprCoerce(node.lhs, ctx)
            val arg1 = visitExprCoerce(node.rhs, ctx)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_DIFF(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_DIFF(TIMEZONE_MINUTE, ...)")
                else -> call("date_diff_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: Env): Rex {
            val type = ANY
            val fn = node.attribute.name.lowercase()
            val call = call(fn)
            return rex(type, call)
        }

        override fun visitExprQuerySet(node: Expr.QuerySet, context: Env): Rex = RelConverter.apply(node, context)

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

        private fun Int?.toRex() = rex(INT4, rexOpLit(int32Value(this)))

        private val ANY: CompilerType = CompilerType(PType.dynamic())
        private val BOOL: CompilerType = CompilerType(PType.bool())
        private val STRING: CompilerType = CompilerType(PType.string())
        private val STRUCT: CompilerType = CompilerType(PType.struct())
        private val BAG: CompilerType = CompilerType(PType.bag())
        private val LIST: CompilerType = CompilerType(PType.array())
        private val SEXP: CompilerType = CompilerType(PType.sexp())
        private val INT: CompilerType = CompilerType(PType.numeric())
        private val INT4: CompilerType = CompilerType(PType.integer())
        private val TIMESTAMP: CompilerType = CompilerType(PType.timestamp(6))
    }
}
