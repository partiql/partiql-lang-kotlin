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
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.builder.plan
import org.partiql.planner.internal.ir.identifierQualified
import org.partiql.planner.internal.ir.identifierSymbol
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
import org.partiql.planner.internal.typer.toStaticType
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.io.PartiQLValueIonReaderBuilder
import org.partiql.value.nullValue
import org.partiql.value.stringValue

/**
 * Converts an AST expression node to a Plan Rex node; ignoring any typing.
 */
internal object RexConverter {

    internal fun apply(expr: Expr): Rex = ToRex.convert(expr)

    internal fun applyRel(expr: Expr): Rex = expr.accept(ToRex, Unit)

    @OptIn(PartiQLValueExperimental::class)
    private object ToRex : AstBaseVisitor<Rex, Unit>() {

        override fun defaultReturn(node: AstNode, ctx: Unit): Rex =
            throw IllegalArgumentException("unsupported rex $node")

        /**
         * !! IMPORTANT !!
         *
         * This is the top-level visit for handling subquery coercion. The default behavior is to coerce to a scalar.
         * In some situations, ie comparison to complex types we may make assertions on the desired type.
         *
         * It is recommended that every method (except for the exceptional cases) recurse the tree from convert.
         *
         *  - RHS of comparison when LHS is an array or collection expression; and visa-versa
         *  - It is the collection expression of a FROM clause or JOIN
         *  - It is the RHS of an IN predicate
         *  - It is an argument of an OUTER set operator.
         */
        fun convert(node: Expr, coercion: Rex.Op.Subquery.Coercion = Rex.Op.Subquery.Coercion.SCALAR): Rex {
            val rex = super.visitExpr(node, Unit)
            return when (isSqlSelect(node)) {
                true -> {
                    val select = rex.op as Rex.Op.Select
                    rex(
                        StaticType.ANY,
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

        override fun visitExprLit(node: Expr.Lit, ctx: Unit): Rex {
            val type = node.value.type.toStaticType()
            val op = rexOpLit(node.value)
            return rex(type, op)
        }

        override fun visitExprIon(node: Expr.Ion, ctx: Unit): Rex {
            val value =
                PartiQLValueIonReaderBuilder
                    .standard().build(node.value).read()
            val type = value.type.toStaticType()
            return rex(type, rexOpLit(value))
        }

        override fun visitExprVar(node: Expr.Var, ctx: Unit): Rex {
            val type = (StaticType.ANY)
            val identifier = AstToPlan.convert(node.identifier)
            val scope = when (node.scope) {
                Expr.Var.Scope.DEFAULT -> Rex.Op.Var.Scope.DEFAULT
                Expr.Var.Scope.LOCAL -> Rex.Op.Var.Scope.LOCAL
            }
            val op = rexOpVarUnresolved(identifier, scope)
            return rex(type, op)
        }

        override fun visitExprUnary(node: Expr.Unary, ctx: Unit): Rex {
            val type = (StaticType.ANY)
            // Args
            val arg = convert(node.expr)
            val args = listOf(arg)
            // Fn
            val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.INSENSITIVE)
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        override fun visitExprBinary(node: Expr.Binary, ctx: Unit): Rex {
            val type = (StaticType.ANY)
            val args = when (node.op) {
                Expr.Binary.Op.LT, Expr.Binary.Op.GT,
                Expr.Binary.Op.LTE, Expr.Binary.Op.GTE,
                Expr.Binary.Op.EQ, Expr.Binary.Op.NE,
                -> {
                    when {
                        // Example: [1, 2] < (SELECT a, b FROM t)
                        isLiteralArray(node.lhs) && isSqlSelect(node.rhs) -> {
                            val lhs = convert(node.lhs)
                            val rhs = convert(node.rhs, Rex.Op.Subquery.Coercion.ROW)
                            listOf(lhs, rhs)
                        }
                        // Example: (SELECT a, b FROM t) < [1, 2]
                        isSqlSelect(node.lhs) && isLiteralArray(node.rhs) -> {
                            val lhs = convert(node.lhs, Rex.Op.Subquery.Coercion.ROW)
                            val rhs = convert(node.rhs)
                            listOf(lhs, rhs)
                        }
                        // Example: 1 < 2
                        else -> {
                            val lhs = convert(node.lhs)
                            val rhs = convert(node.rhs)
                            listOf(lhs, rhs)
                        }
                    }
                }
                // Example: 1 + 2
                else -> {
                    val lhs = convert(node.lhs)
                    val rhs = convert(node.rhs)
                    listOf(lhs, rhs)
                }
            }
            // Wrap if a NOT if necessary
            return when (node.op) {
                Expr.Binary.Op.NE -> {
                    val op = negate(call("eq", *args.toTypedArray()))
                    rex(type, op)
                }
                else -> {
                    val id = identifierSymbol(node.op.name.lowercase(), Identifier.CaseSensitivity.INSENSITIVE)
                    val op = rexOpCallUnresolved(id, args)
                    rex(type, op)
                }
            }
        }

        private fun isLiteralArray(node: Expr): Boolean =
            node is Expr.Collection && (node.type == Expr.Collection.Type.ARRAY || node.type == Expr.Collection.Type.LIST)

        private fun isSqlSelect(node: Expr): Boolean = node is Expr.SFW &&
            (
                node.select is Select.Project || node.select is Select.Star
                )

        private fun mergeIdentifiers(root: Identifier, steps: List<Identifier>): Identifier {
            if (steps.isEmpty()) {
                return root
            }
            val (newRoot, firstSteps) = when (root) {
                is Identifier.Symbol -> root to emptyList()
                is Identifier.Qualified -> root.root to root.steps
            }
            val followingSteps = steps.flatMap { step ->
                when (step) {
                    is Identifier.Symbol -> listOf(step)
                    is Identifier.Qualified -> listOf(step.root) + step.steps
                }
            }
            return identifierQualified(newRoot, firstSteps + followingSteps)
        }

        override fun visitExprPath(node: Expr.Path, ctx: Unit): Rex {
            // Args
            val root = convert(node.root)

            // Attempt to create qualified identifier
            val (newRoot, newSteps) = when (val op = root.op) {
                is Rex.Op.Var.Unresolved -> {
                    val identifierSteps = mutableListOf<Identifier>()
                    run {
                        node.steps.forEach { step ->
                            if (step !is Expr.Path.Step.Symbol) {
                                return@run
                            }
                            identifierSteps.add(AstToPlan.convert(step.symbol))
                        }
                    }
                    when (identifierSteps.size) {
                        0 -> root to node.steps
                        else -> {
                            val newRoot = rex(
                                StaticType.ANY,
                                rexOpVarUnresolved(mergeIdentifiers(op.identifier, identifierSteps), op.scope)
                            )
                            val newSteps = node.steps.subList(identifierSteps.size, node.steps.size)
                            newRoot to newSteps
                        }
                    }
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
                        val key = convert(step.key)
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
                        val identifier = AstToPlan.convert(step.symbol)
                        val op = when (identifier.caseSensitivity) {
                            Identifier.CaseSensitivity.SENSITIVE -> rexOpPathKey(
                                current,
                                rexString(identifier.symbol)
                            )

                            Identifier.CaseSensitivity.INSENSITIVE -> rexOpPathSymbol(current, identifier.symbol)
                        }
                        op
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
                        // Unpivot produces two binding, in this ctx we want the value,
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
                rex(StaticType.ANY, path)
            }

            if (fromList.size == 0) return pathNavi
            val fromNode = fromList.reduce { acc, scan ->
                val schema = acc.type.schema + scan.type.schema
                val props = emptySet<Rel.Prop>()
                val type = relType(schema, props)
                rel(type, relOpJoin(acc, scan, rex(StaticType.BOOL, rexOpLit(boolValue(true))), Rel.Op.Join.Type.INNER))
            }

            // compute the ref used by select construct
            // always going to be the last binding
            val selectRef = fromNode.type.schema.size - 1

            val constructor = when (val op = pathNavi.op) {
                is Rex.Op.Path.Index -> rex(
                    pathNavi.type,
                    rexOpPathIndex(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key)
                )
                is Rex.Op.Path.Key -> rex(
                    pathNavi.type,
                    rexOpPathKey(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key)
                )
                is Rex.Op.Path.Symbol -> rex(
                    pathNavi.type,
                    rexOpPathSymbol(rex(op.root.type, rexOpVarLocal(0, selectRef)), op.key)
                )
                is Rex.Op.Var.Local -> rex(pathNavi.type, rexOpVarLocal(0, selectRef))
                else -> throw IllegalStateException()
            }
            val op = rexOpSelect(constructor, fromNode)
            return rex(StaticType.ANY, op)
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
                    type = StaticType.STRING
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

        private fun rexString(str: String) = rex(StaticType.STRING, rexOpLit(stringValue(str)))

        override fun visitExprCall(node: Expr.Call, ctx: Unit): Rex {
            val type = (StaticType.ANY)
            // Fn
            val id = AstToPlan.convert(node.function)
            if (id is Identifier.Symbol && id.symbol.equals("TUPLEUNION", ignoreCase = true)) {
                return visitExprCallTupleUnion(node, ctx)
            }
            // Args
            val args = node.args.map { convert(it) }
            // Rex
            val op = rexOpCallUnresolved(id, args)
            return rex(type, op)
        }

        private fun visitExprCallTupleUnion(node: Expr.Call, ctx: Unit): Rex {
            val type = (StaticType.STRUCT)
            val args = node.args.map { convert(it) }.toMutableList()
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        override fun visitExprCase(node: Expr.Case, ctx: Unit) = plan {
            val type = (StaticType.ANY)
            val rex = when (node.expr) {
                null -> null
                else -> convert(node.expr!!) // match `rex
            }

            // Converts AST CASE (x) WHEN y THEN z --> Plan CASE WHEN x = y THEN z
            val id = identifierSymbol(Expr.Binary.Op.EQ.name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            val createBranch: (Rex, Rex) -> Rex.Op.Case.Branch = { condition: Rex, result: Rex ->
                val updatedCondition = when (rex) {
                    null -> condition
                    else -> rex(type, rexOpCallUnresolved(id, listOf(rex, condition)))
                }
                rexOpCaseBranch(updatedCondition, result)
            }

            val branches = node.branches.map {
                val branchCondition = convert(it.condition)
                val branchRex = convert(it.expr)
                createBranch(branchCondition, branchRex)
            }.toMutableList()

            val defaultRex = when (val default = node.default) {
                null -> rex(type = StaticType.ANY, op = rexOpLit(value = nullValue()))
                else -> convert(default)
            }
            val op = rexOpCase(branches = branches, default = defaultRex)
            rex(type, op)
        }

        override fun visitExprCollection(node: Expr.Collection, ctx: Unit): Rex {
            val type = when (node.type) {
                Expr.Collection.Type.BAG -> StaticType.BAG
                Expr.Collection.Type.ARRAY -> StaticType.LIST
                Expr.Collection.Type.VALUES -> StaticType.LIST
                Expr.Collection.Type.LIST -> StaticType.LIST
                Expr.Collection.Type.SEXP -> StaticType.SEXP
            }
            val values = node.values.map { convert(it) }
            val op = rexOpCollection(values)
            return rex(type, op)
        }

        override fun visitExprStruct(node: Expr.Struct, ctx: Unit): Rex {
            val type = (StaticType.STRUCT)
            val fields = node.fields.map {
                val k = convert(it.name)
                val v = convert(it.value)
                rexOpStructField(k, v)
            }
            val op = rexOpStruct(fields)
            return rex(type, op)
        }

        // SPECIAL FORMS

        /**
         * <arg0> NOT? LIKE <arg1> ( ESCAPE <arg2>)?
         */
        override fun visitExprLike(node: Expr.Like, ctx: Unit): Rex {
            val type = StaticType.BOOL
            // Args
            val arg0 = convert(node.value)
            val arg1 = convert(node.pattern)
            val arg2 = node.escape?.let { convert(it) }
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
        override fun visitExprBetween(node: Expr.Between, ctx: Unit): Rex = plan {
            val type = StaticType.BOOL
            // Args
            val arg0 = convert(node.value)
            val arg1 = convert(node.from)
            val arg2 = convert(node.to)
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
        override fun visitExprInCollection(node: Expr.InCollection, ctx: Unit): Rex {
            val type = StaticType.BOOL
            // Args
            val arg0 = convert(node.lhs)
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
        override fun visitExprIsType(node: Expr.IsType, ctx: Unit): Rex {
            val type = StaticType.BOOL
            // arg
            val arg0 = convert(node.value)

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
                is Type.Sexp -> call("is_sexp", arg0)
                is Type.Any -> call("is_any", arg0)
                is Type.Custom -> call("is_custom", arg0)
                is Type.List -> call("is_list", arg0)
                is Type.Tuple -> call("is_tuple", arg0)
                // Note that for is function, the parser will reject parameterized list/struct
                is Type.Array -> call("is_list", arg0)
                is Type.Struct -> call("is_struct", arg0)
            }

            if (node.not == true) {
                call = negate(call)
            }

            return rex(type, call)
        }

        override fun visitExprCoalesce(node: Expr.Coalesce, ctx: Unit): Rex {
            val type = StaticType.ANY
            val args = node.args.map { arg ->
                convert(arg)
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        override fun visitExprNullIf(node: Expr.NullIf, ctx: Unit): Rex {
            val type = StaticType.ANY
            val value = convert(node.value)
            val nullifier = convert(node.nullifier)
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * SUBSTRING(<arg0> (FROM <arg1> (FOR <arg2>)?)? )
         */
        override fun visitExprSubstring(node: Expr.Substring, ctx: Unit): Rex {
            val type = StaticType.ANY
            // Args
            val arg0 = convert(node.value)
            val arg1 = node.start?.let { convert(it) } ?: rex(StaticType.INT, rexOpLit(int64Value(1)))
            val arg2 = node.length?.let { convert(it) }
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
        override fun visitExprPosition(node: Expr.Position, ctx: Unit): Rex {
            val type = StaticType.ANY
            // Args
            val arg0 = convert(node.lhs)
            val arg1 = convert(node.rhs)
            // Call
            val call = call("position", arg0, arg1)
            return rex(type, call)
        }

        /**
         * TRIM([LEADING|TRAILING|BOTH]? (<arg1> FROM)? <arg0>)
         */
        override fun visitExprTrim(node: Expr.Trim, ctx: Unit): Rex {
            val type = StaticType.TEXT
            // Args
            val arg0 = convert(node.value)
            val arg1 = node.chars?.let { convert(it) }
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
        override fun visitExprOverlay(node: Expr.Overlay, ctx: Unit): Rex {
            val cv = convert(node.value)
            val sp = convert(node.start)
            val rs = convert(node.overlay)
            val sl = node.length?.let { convert(it) } ?: rex(StaticType.ANY, call("char_length", rs))
            val p1 = rex(
                StaticType.ANY,
                call(
                    "substring",
                    cv,
                    rex(StaticType.INT4, rexOpLit(int32Value(1))),
                    rex(StaticType.ANY, call("minus", sp, rex(StaticType.INT4, rexOpLit(int32Value(1)))))
                )
            )
            val p2 = rex(StaticType.ANY, call("concat", p1, rs))
            return rex(
                StaticType.ANY,
                call(
                    "concat",
                    p2,
                    rex(StaticType.ANY, call("substring", cv, rex(StaticType.ANY, call("plus", sp, sl))))
                )
            )
        }

        override fun visitExprExtract(node: Expr.Extract, ctx: Unit): Rex {
            val call = call("extract_${node.field.name.lowercase()}", convert(node.source))
            return rex(StaticType.ANY, call)
        }

        // TODO: Ignoring type parameter now
        override fun visitExprCast(node: Expr.Cast, ctx: Unit): Rex {
            val type = node.asType
            val arg = convert(node.value)
            val target = when (type) {
                is Type.NullType -> error("Cannot cast any value to NULL")
                is Type.Missing -> error("Cannot cast any value to MISSING")
                is Type.Bool -> PartiQLValueType.BOOL
                is Type.Tinyint -> PartiQLValueType.INT8
                is Type.Smallint, is Type.Int2 -> PartiQLValueType.INT16
                is Type.Int4 -> PartiQLValueType.INT32
                is Type.Bigint, is Type.Int8 -> PartiQLValueType.INT64
                is Type.Int -> PartiQLValueType.INT
                is Type.Real -> PartiQLValueType.FLOAT64
                is Type.Float32 -> PartiQLValueType.FLOAT32
                is Type.Float64 -> PartiQLValueType.FLOAT64
                is Type.Decimal -> if (type.scale != null) PartiQLValueType.DECIMAL else PartiQLValueType.DECIMAL_ARBITRARY
                is Type.Numeric -> if (type.scale != null) PartiQLValueType.DECIMAL else PartiQLValueType.DECIMAL_ARBITRARY
                is Type.Char -> PartiQLValueType.CHAR
                is Type.Varchar -> PartiQLValueType.STRING
                is Type.String -> PartiQLValueType.STRING
                is Type.Symbol -> PartiQLValueType.SYMBOL
                is Type.Bit -> PartiQLValueType.BINARY
                is Type.BitVarying -> PartiQLValueType.BINARY
                is Type.ByteString -> PartiQLValueType.BINARY
                is Type.Blob -> PartiQLValueType.BLOB
                is Type.Clob -> PartiQLValueType.CLOB
                is Type.Date -> PartiQLValueType.DATE
                is Type.Time -> PartiQLValueType.TIME
                is Type.TimeWithTz -> PartiQLValueType.TIME
                is Type.Timestamp -> PartiQLValueType.TIMESTAMP
                is Type.TimestampWithTz -> PartiQLValueType.TIMESTAMP
                is Type.Interval -> PartiQLValueType.INTERVAL
                is Type.Bag -> PartiQLValueType.BAG
                is Type.Sexp -> PartiQLValueType.SEXP
                is Type.Any -> PartiQLValueType.ANY
                is Type.Custom -> TODO("Custom type not supported ")
                is Type.List -> PartiQLValueType.LIST
                is Type.Tuple -> PartiQLValueType.STRUCT
                is Type.Array -> PartiQLValueType.LIST
                is Type.Struct -> PartiQLValueType.STRUCT
            }
            return rex(StaticType.ANY, rexOpCastUnresolved(target, arg))
        }

        override fun visitExprCanCast(node: Expr.CanCast, ctx: Unit): Rex {
            TODO("PartiQL Special Form CAN_CAST")
        }

        override fun visitExprCanLosslessCast(node: Expr.CanLosslessCast, ctx: Unit): Rex {
            TODO("PartiQL Special Form CAN_LOSSLESS_CAST")
        }

        override fun visitExprDateAdd(node: Expr.DateAdd, ctx: Unit): Rex {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = convert(node.lhs)
            val arg1 = convert(node.rhs)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_ADD(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_ADD(TIMEZONE_MINUTE, ...)")
                else -> call("date_add_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprDateDiff(node: Expr.DateDiff, ctx: Unit): Rex {
            val type = StaticType.TIMESTAMP
            // Args
            val arg0 = convert(node.lhs)
            val arg1 = convert(node.rhs)
            // Call Variants
            val call = when (node.field) {
                DatetimeField.TIMEZONE_HOUR -> error("Invalid call DATE_DIFF(TIMEZONE_HOUR, ...)")
                DatetimeField.TIMEZONE_MINUTE -> error("Invalid call DATE_DIFF(TIMEZONE_MINUTE, ...)")
                else -> call("date_diff_${node.field.name.lowercase()}", arg0, arg1)
            }
            return rex(type, call)
        }

        override fun visitExprSessionAttribute(node: Expr.SessionAttribute, ctx: Unit): Rex {
            val type = StaticType.ANY
            val fn = node.attribute.name.lowercase()
            val call = call(fn)
            return rex(type, call)
        }

        override fun visitExprSFW(node: Expr.SFW, ctx: Unit): Rex = RelConverter.apply(node)

        override fun visitExprBagOp(node: Expr.BagOp, ctx: Unit): Rex {
            val lhs = Rel(
                type = Rel.Type(listOf(Rel.Binding("_0", StaticType.ANY)), props = emptySet()),
                op = Rel.Op.Scan(visitExpr(node.lhs, ctx))
            )
            val rhs = Rel(
                type = Rel.Type(listOf(Rel.Binding("_1", StaticType.ANY)), props = emptySet()),
                op = Rel.Op.Scan(visitExpr(node.rhs, ctx))
            )
            val quantifier = when (node.type.setq) {
                SetQuantifier.ALL -> Rel.Op.Set.Quantifier.ALL
                null, SetQuantifier.DISTINCT -> Rel.Op.Set.Quantifier.DISTINCT
            }
            val isOuter = node.outer == true
            val op = when (node.type.type) {
                SetOp.Type.UNION -> Rel.Op.Set.Union(quantifier, lhs, rhs, isOuter)
                SetOp.Type.EXCEPT -> Rel.Op.Set.Except(quantifier, lhs, rhs, isOuter)
                SetOp.Type.INTERSECT -> Rel.Op.Set.Intersect(quantifier, lhs, rhs, isOuter)
            }
            val rel = Rel(
                type = Rel.Type(listOf(Rel.Binding("_0", StaticType.ANY)), props = emptySet()),
                op = op
            )
            return Rex(
                type = StaticType.ANY,
                op = Rex.Op.Select(
                    constructor = Rex(
                        StaticType.ANY,
                        Rex.Op.Var.Unresolved(
                            Identifier.Symbol("_0", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.LOCAL
                        )
                    ),
                    rel = rel
                )
            )
        }

        // Helpers

        private fun negate(call: Rex.Op.Call): Rex.Op.Call {
            val name = Expr.Unary.Op.NOT.name
            val id = identifierSymbol(name.lowercase(), Identifier.CaseSensitivity.SENSITIVE)
            // wrap
            val arg = rex(StaticType.BOOL, call)
            // rewrite call
            return rexOpCallUnresolved(id, listOf(arg))
        }

        /**
         * Create a [Rex.Op.Call.Static] node which has a hidden unresolved Function.
         * The purpose of having such hidden function is to prevent usage of generated function name in query text.
         */
        private fun call(name: String, vararg args: Rex): Rex.Op.Call {
            val id = identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE)
            return rexOpCallUnresolved(id, args.toList())
        }

        private fun Int?.toRex() = rex(StaticType.INT4, rexOpLit(int32Value(this)))
    }
}
