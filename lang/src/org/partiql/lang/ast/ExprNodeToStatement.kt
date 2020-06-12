package org.partiql.lang.ast

import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.domains.partiql_ast
import org.partiql.lang.domains.partiql_ast.group_by
import org.partiql.lang.domains.partiql_ast.grouping_strategy
import org.partiql.lang.domains.partiql_ast.set_quantifier
import org.partiql.pig.runtime.asPrimitive

/** Converts an [ExprNode] to a [partiql_ast.statement]. */
fun ExprNode.toAstStatement(): partiql_ast.statement {
    val node = this
    return when(node) {
        is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg,
        is Typed, is Path, is SimpleCase, is SearchedCase, is Select, is Struct,
        is Seq -> partiql_ast.build { query(toAstExpr()) }

        is DataManipulation -> node.toAstDml()

        is CreateTable, is CreateIndex, is DropTable, is DropIndex -> toAstDdl()

    }
}

private fun PartiQlMetaContainer.toElectrolyteMetaContainer(): ElectrolyteMetaContainer =
    com.amazon.ionelement.api.metaContainerOf(map { it.tag to it })

private fun ExprNode.toAstDdl(): partiql_ast.statement {
    val thiz = this
    val metas = metas.toElectrolyteMetaContainer()

    return partiql_ast.build {
        when(thiz) {
            is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg, is Typed,
            is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is Seq,
            is DataManipulation -> error("Can't convert ${thiz.javaClass} to partiql_ast.ddl")

            is CreateTable -> ddl(create_table(thiz.tableName), metas)
            is CreateIndex -> ddl(create_index(identifier(thiz.tableName, case_sensitive()), thiz.keys.map { it.toAstExpr() }), metas)
            is DropIndex ->
                ddl(
                    drop_index(
                        // case-sensitivity of table names cannot be represented with ExprNode.
                        identifier(thiz.tableName, case_sensitive()),
                        identifier(thiz.identifier.id, thiz.identifier.case.toAstCaseSensitivity())),
                    metas)
            is DropTable ->
                // case-sensitivity of table names cannot be represented with ExprNode.
                ddl(drop_table(identifier(thiz.tableName, case_sensitive())), metas)
        }
    }
}

fun ExprNode.toAstExpr(): partiql_ast.expr {
    val node = this
    val metas = this.metas.toElectrolyteMetaContainer()

    return partiql_ast.build {
        when (node) {
            is Literal -> lit(node.ionValue.toIonElement(), metas)
            is LiteralMissing -> missing(metas)
            is VariableReference -> id(node.id, node.case.toAstCaseSensitivity(), node.scopeQualifier.toAstScopeQualifier(), metas)
            is Parameter -> parameter(node.position.toLong(), metas)
            is NAry -> {
                val args = node.args.map { it.toAstExpr() }
                when(node.op) {
                    NAryOp.ADD -> plus(args, metas)
                    NAryOp.SUB -> minus(args, metas)
                    NAryOp.MUL -> times(args, metas)
                    NAryOp.DIV -> divide(args, metas)
                    NAryOp.MOD -> modulo(args, metas)
                    NAryOp.EQ -> eq(args, metas)
                    NAryOp.LT -> lt(args, metas)
                    NAryOp.LTE -> lte(args, metas)
                    NAryOp.GT -> gt(args, metas)
                    NAryOp.GTE -> gte(args, metas)
                    NAryOp.NE -> ne(args, metas)
                    NAryOp.LIKE -> like(args[0], args[1], if (args.size >= 3) args[2] else null, metas)
                    NAryOp.BETWEEN -> between(args[0], args[1], args[2])
                    NAryOp.NOT -> not(args[0], metas)
                    NAryOp.IN -> in_collection(args, metas)
                    NAryOp.AND -> and(args, metas)
                    NAryOp.OR -> or(args, metas)
                    NAryOp.STRING_CONCAT -> concat(args, metas)
                    NAryOp.CALL -> {
                        val idArg = args.first() as? partiql_ast.expr.id
                            ?: error("First argument of call should be a VariableReference")
                        // the above error message says "VariableReference" and not partiql_ast.expr.id because it would
                        // have been converted from a VariableReference when [args] was being built.

                        // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                        call(idArg.symbol0.text, args.drop(1), metas)
                    }
                    NAryOp.INTERSECT -> intersect(set_quantifier.distinct(), args, metas)
                    NAryOp.INTERSECT_ALL -> intersect(set_quantifier.all(), args, metas)
                    NAryOp.EXCEPT -> except(set_quantifier.distinct(), args, metas)
                    NAryOp.EXCEPT_ALL -> except(set_quantifier.all(), args, metas)
                    NAryOp.UNION -> union(set_quantifier.distinct(), args, metas)
                    NAryOp.UNION_ALL -> union(set_quantifier.all(), args, metas)
                }
            }
            is CallAgg -> {
                val symbol1 = (node.funcExpr as? VariableReference)?.id
                    ?: error("Expected CallAgg.funcExpr to be a VariableReference")
                // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                call_agg(node.setQuantifier.toAstSetQuantifier(), symbol1, node.arg.toAstExpr(), metas)
            }
            is Typed ->
                when(node.op) {
                    TypedOp.CAST -> cast(node.expr.toAstExpr(), node.type.toAstType(), metas)
                    TypedOp.IS -> is_type(node.expr.toAstExpr(), node.type.toAstType(), metas)
                }
            is Path -> path(node.root.toAstExpr(), node.components.map { it.toAstPathStep() }, metas)
            is SimpleCase ->
                simple_case(
                    node.valueExpr.toAstExpr(),
                    expr_pair_list(node.whenClauses.map { expr_pair(it.valueExpr.toAstExpr(), it.thenExpr.toAstExpr()) }),
                    node.elseExpr?.toAstExpr(),
                    metas)
            is SearchedCase ->
                searched_case(
                    expr_pair_list(node.whenClauses.map { expr_pair(it.condition.toAstExpr(), it.thenExpr.toAstExpr()) }),
                    node.elseExpr?.toAstExpr(),
                    metas)
            is Select ->
                select(
                    // Only set setq if its distinct since setting it causes it to be added to the s-expressions
                    // created by the `toIonElement()` function and that breaks downstream QLDB rules which are
                    // not plumbed to account for this field at this time.  Luckily, QLDB does not support the
                    // DISTINCT modifier on SFW queries anyway.
                    setq = node.setQuantifier.let {
                        when (it) {
                            SetQuantifier.ALL -> null
                            SetQuantifier.DISTINCT -> set_quantifier.distinct()
                        }
                    },
                    project = node.projection.toAstSelectProject(),
                    from = node.from.toAstFromSource(),
                    where = node.where?.toAstExpr(),
                    group = node.groupBy?.toAstGroupSpec(),
                    having = node.having?.toAstExpr(),
                    limit = node.limit?.toAstExpr(),
                    metas = metas)
            is Struct -> struct(node.fields.map { expr_pair(it.name.toAstExpr(), it.expr.toAstExpr()) })
            is Seq ->
                when(node.type) {
                    SeqType.LIST -> list(node.values.map { it.toAstExpr() })
                    SeqType.SEXP -> sexp(node.values.map { it.toAstExpr() })
                    SeqType.BAG -> bag(node.values.map { it.toAstExpr() })
                }

            // These are handled by `toAstDml()`
            is DataManipulation, is CreateTable, is CreateIndex, is DropTable, is DropIndex ->
                error("Can't transform ${node.javaClass} to a partiql_ast.expr }")
        }
    }
}

private fun GroupBy.toAstGroupSpec(): group_by =
    group_by(
        this.grouping.toAstGroupStrategy(),
        partiql_ast.group_key_list(this.groupByItems.map { partiql_ast.group_key(it.expr.toAstExpr(), it.asName?.name?.asPrimitive()) }),
        this.groupName?.name?.asPrimitive())


private fun GroupingStrategy.toAstGroupStrategy(): grouping_strategy =
    when(this) {
        GroupingStrategy.FULL -> grouping_strategy.group_full()
        GroupingStrategy.PARTIAL -> grouping_strategy.group_partial()
    }

private fun CaseSensitivity.toAstCaseSensitivity(): partiql_ast.case_sensitivity {
    val thiz = this
    return partiql_ast.build {
        when (thiz) {
            CaseSensitivity.SENSITIVE -> case_sensitive()
            CaseSensitivity.INSENSITIVE -> case_insensitive()
        }
    }
}

private fun ScopeQualifier.toAstScopeQualifier(): partiql_ast.scope_qualifier {
    val thiz = this
    return partiql_ast.build {
        when (thiz) {
            ScopeQualifier.LEXICAL -> locals_first()
            ScopeQualifier.UNQUALIFIED -> unqualified()
        }
    }
}

private fun SetQuantifier.toAstSetQuantifier(): partiql_ast.set_quantifier {
    val thiz = this
    return partiql_ast.build {
        when (thiz) {
            SetQuantifier.ALL -> all()
            SetQuantifier.DISTINCT -> distinct()
        }
    }
}

private fun SelectProjection.toAstSelectProject(): partiql_ast.projection {
    val thiz = this
    return partiql_ast.build {
        when(thiz) {
            is SelectProjectionValue -> project_value(thiz.expr.toAstExpr())
            is SelectProjectionList -> {
                if(thiz.items.any { it is SelectListItemStar }) {
                    if(thiz.items.size > 1) error("More than one select item when SELECT * was present.")
                    project_star()
                }
                else
                    project_list(
                        thiz.items.map {
                            when(it) {
                                is SelectListItemExpr -> project_expr(it.expr.toAstExpr(), it.asName?.name)
                                is SelectListItemProjectAll -> project_all(it.expr.toAstExpr())
                                is SelectListItemStar -> error("this should happen due to `when` branch above.")
                            }
                        })
            }
            is SelectProjectionPivot -> project_pivot(thiz.nameExpr.toAstExpr(), thiz.valueExpr.toAstExpr())
        }
    }
}

private fun FromSource.toAstFromSource(): partiql_ast.from_source {
    val thiz = this
    val metas = thiz.metas().toElectrolyteMetaContainer()
    return partiql_ast.build {
        when (thiz) {
            is FromSourceExpr -> scan(
                thiz.expr.toAstExpr(),
                thiz.variables.asName?.name,
                thiz.variables.atName?.name,
                thiz.variables.byName?.name)
            is FromSourceJoin -> {
                val jt = when (thiz.joinOp) {
                    JoinOp.INNER -> inner()
                    JoinOp.LEFT -> left()
                    JoinOp.RIGHT -> right()
                    JoinOp.OUTER -> full()
                }
                join(
                    jt,
                    thiz.leftRef.toAstFromSource(),
                    thiz.rightRef.toAstFromSource(),
                    if (thiz.metas.hasMeta(IsImplictJoinMeta.TAG)) null else thiz.condition.toAstExpr(),
                    metas = metas)
            }
            is FromSourceUnpivot -> unpivot(
                thiz.expr.toAstExpr(),
                thiz.variables.asName?.name,
                thiz.variables.atName?.name,
                thiz.variables.byName?.name)
        }
    }
}

private fun PathComponent.toAstPathStep(): partiql_ast.path_step {
    val thiz = this
    return partiql_ast.build {
        when (thiz) {
            is PathComponentExpr -> path_expr(thiz.expr.toAstExpr(), thiz.case.toAstCaseSensitivity())
            is PathComponentUnpivot -> path_unpivot(thiz.metas.toElectrolyteMetaContainer())
            is PathComponentWildcard -> path_wildcard(thiz.metas.toElectrolyteMetaContainer())
        }
    }
}

private fun DataManipulation.toAstDml(): partiql_ast.statement {
    val thiz = this
    return partiql_ast.build {
        val dmlOp = thiz.dmlOperation
        val dmlOp2 = when (dmlOp) {
            is InsertOp ->
                insert(
                    dmlOp.lvalue.toAstExpr(),
                    dmlOp.values.toAstExpr())
            is InsertValueOp ->
                insert_value(
                    dmlOp.lvalue.toAstExpr(),
                    dmlOp.value.toAstExpr(),
                    dmlOp.position?.toAstExpr(),
                    thiz.metas.toElectrolyteMetaContainer())
            is AssignmentOp ->
                set(
                    dmlOp.assignments.map {
                        assignment(
                            it.lvalue.toAstExpr(),
                            it.rvalue.toAstExpr())
                    })
            is RemoveOp -> remove(dmlOp.lvalue.toAstExpr())
            DeleteOp -> delete()
        }

        dml(
            dmlOp2,
            thiz.from?.toAstFromSource(),
            thiz.where?.toAstExpr(),
            thiz.metas.toElectrolyteMetaContainer())
    }
}


private fun DataType.toAstType(): partiql_ast.type {
    val thiz = this
    val arg1 = thiz.args.getOrNull(0)?.toLong()
    val arg2 = thiz.args.getOrNull(1)?.toLong()
    return partiql_ast.build {
        when(thiz.sqlDataType) {
            SqlDataType.MISSING -> missing_type()
            SqlDataType.NULL -> null_type()
            SqlDataType.BOOLEAN -> boolean_type()
            SqlDataType.SMALLINT -> smallint_type()
            SqlDataType.INTEGER -> integer_type()
            SqlDataType.FLOAT -> float_type(arg1)
            SqlDataType.REAL -> real_type()
            SqlDataType.DOUBLE_PRECISION -> double_precision_type()
            SqlDataType.DECIMAL -> decimal_type(arg1, arg2)
            SqlDataType.NUMERIC -> numeric_type(arg1, arg2)
            SqlDataType.TIMESTAMP -> timestamp_type()
            SqlDataType.CHARACTER -> character_type(arg1)
            SqlDataType.CHARACTER_VARYING -> character_varying_type(arg1)
            SqlDataType.STRING -> string_type()
            SqlDataType.SYMBOL -> symbol_type()
            SqlDataType.CLOB -> clob_type()
            SqlDataType.BLOB -> blob_type()
            SqlDataType.STRUCT -> struct_type()
            SqlDataType.TUPLE -> tuple_type()
            SqlDataType.LIST -> list_type()
            SqlDataType.SEXP -> sexp_type()
            SqlDataType.BAG -> bag_type()
        }
    }
}
