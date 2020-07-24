package org.partiql.lang.ast

import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.pig.runtime.SymbolPrimitive

/** Converts an [ExprNode] to a [PartiqlAst.statement]. */
fun ExprNode.toAstStatement(): PartiqlAst.Statement {
    val node = this
    return when(node) {
        is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg,
        is Typed, is Path, is SimpleCase, is SearchedCase, is Select, is Struct,
        is Seq -> PartiqlAst.build { query(toAstExpr()) }

        is DataManipulation -> node.toAstDml()

        is CreateTable, is CreateIndex, is DropTable, is DropIndex -> toAstDdl()

    }
}

private fun PartiQlMetaContainer.toElectrolyteMetaContainer(): ElectrolyteMetaContainer =
    com.amazon.ionelement.api.metaContainerOf(map { it.tag to it })

private fun ExprNode.toAstDdl(): PartiqlAst.Statement {
    val thiz = this
    val metas = metas.toElectrolyteMetaContainer()

    return PartiqlAst.build {
        when(thiz) {
            is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg, is Typed,
            is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is Seq,
            is DataManipulation -> error("Can't convert ${thiz.javaClass} to PartiqlAst.ddl")

            is CreateTable -> ddl(createTable(thiz.tableName), metas)
            is CreateIndex -> ddl(createIndex(identifier(thiz.tableName, caseSensitive()), thiz.keys.map { it.toAstExpr() }), metas)
            is DropIndex ->
                ddl(
                    dropIndex(
                        // case-sensitivity of table names cannot be represented with ExprNode.
                        identifier(thiz.tableName, caseSensitive()),
                        identifier(thiz.identifier.id, thiz.identifier.case.toAstCaseSensitivity())),
                    metas)
            is DropTable ->
                // case-sensitivity of table names cannot be represented with ExprNode.
                ddl(dropTable(identifier(thiz.tableName, caseSensitive())), metas)
        }
    }
}

fun ExprNode.toAstExpr(): PartiqlAst.Expr {
    val node = this
    val metas = this.metas.toElectrolyteMetaContainer()

    return PartiqlAst.build {
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
                    NAryOp.BETWEEN -> between(args[0], args[1], args[2], metas)
                    NAryOp.NOT -> not(args[0], metas)
                    NAryOp.IN -> inCollection(args, metas)
                    NAryOp.AND -> and(args, metas)
                    NAryOp.OR -> or(args, metas)
                    NAryOp.STRING_CONCAT -> concat(args, metas)
                    NAryOp.CALL -> {
                        val idArg = args.first() as? PartiqlAst.Expr.Id
                            ?: error("First argument of call should be a VariableReference")
                        // the above error message says "VariableReference" and not PartiqlAst.expr.id because it would
                        // have been converted from a VariableReference when [args] was being built.

                        // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                        call(idArg.name.text, args.drop(1), metas)
                    }
                    NAryOp.INTERSECT -> intersect(distinct(), args, metas)
                    NAryOp.INTERSECT_ALL -> intersect(all(), args, metas)
                    NAryOp.EXCEPT -> except(distinct(), args, metas)
                    NAryOp.EXCEPT_ALL -> except(all(), args, metas)
                    NAryOp.UNION -> union(distinct(), args, metas)
                    NAryOp.UNION_ALL -> union(all(), args, metas)
                }
            }
            is CallAgg -> {
                val symbol1 = (node.funcExpr as? VariableReference)?.id
                    ?: error("Expected CallAgg.funcExpr to be a VariableReference")
                // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                callAgg(node.setQuantifier.toAstSetQuantifier(), symbol1, node.arg.toAstExpr(), metas)
            }
            is Typed ->
                when(node.op) {
                    TypedOp.CAST -> cast(node.expr.toAstExpr(), node.type.toAstType(), metas)
                    TypedOp.IS -> isType(node.expr.toAstExpr(), node.type.toAstType(), metas)
                }
            is Path -> path(node.root.toAstExpr(), node.components.map { it.toAstPathStep() }, metas)
            is SimpleCase ->
                simpleCase(
                    node.valueExpr.toAstExpr(),
                    exprPairList(node.whenClauses.map { exprPair(it.valueExpr.toAstExpr(), it.thenExpr.toAstExpr()) }),
                    node.elseExpr?.toAstExpr(),
                    metas)
            is SearchedCase ->
                searchedCase(
                    exprPairList(node.whenClauses.map { exprPair(it.condition.toAstExpr(), it.thenExpr.toAstExpr()) }),
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
                            SetQuantifier.DISTINCT -> distinct()
                        }
                    },
                    project = node.projection.toAstSelectProject(),
                    from = node.from.toAstFromSource(),
                    where = node.where?.toAstExpr(),
                    group = node.groupBy?.toAstGroupSpec(),
                    having = node.having?.toAstExpr(),
                    limit = node.limit?.toAstExpr(),
                    metas = metas)
            is Struct -> struct(node.fields.map { exprPair(it.name.toAstExpr(), it.expr.toAstExpr()) })
            is Seq ->
                when(node.type) {
                    SeqType.LIST -> list(node.values.map { it.toAstExpr() })
                    SeqType.SEXP -> sexp(node.values.map { it.toAstExpr() })
                    SeqType.BAG -> bag(node.values.map { it.toAstExpr() })
                }

            // These are handled by `toAstDml()`
            is DataManipulation, is CreateTable, is CreateIndex, is DropTable, is DropIndex ->
                error("Can't transform ${node.javaClass} to a PartiqlAst.expr }")
        }
    }
}

private fun GroupBy.toAstGroupSpec(): PartiqlAst.GroupBy =
    PartiqlAst.build {
        groupBy_(
            this@toAstGroupSpec.grouping.toAstGroupStrategy(),
            groupKeyList(this@toAstGroupSpec.groupByItems.map { groupKey_(it.expr.toAstExpr(), it.asName?.toSymbolPrimitive()) }),
            this@toAstGroupSpec.groupName?.toSymbolPrimitive())
    }

private fun SymbolicName.toSymbolPrimitive() =
    SymbolPrimitive(this.name, this.metas.toElectrolyteMetaContainer())

private fun GroupingStrategy.toAstGroupStrategy(): PartiqlAst.GroupingStrategy =
    PartiqlAst.build {
        when (this@toAstGroupStrategy) {
            GroupingStrategy.FULL -> groupFull()
            GroupingStrategy.PARTIAL -> groupPartial()
        }
    }

private fun CaseSensitivity.toAstCaseSensitivity(): PartiqlAst.CaseSensitivity {
    val thiz = this
    return PartiqlAst.build {
        when (thiz) {
            CaseSensitivity.SENSITIVE -> caseSensitive()
            CaseSensitivity.INSENSITIVE -> caseInsensitive()
        }
    }
}

private fun ScopeQualifier.toAstScopeQualifier(): PartiqlAst.ScopeQualifier {
    val thiz = this
    return PartiqlAst.build {
        when (thiz) {
            ScopeQualifier.LEXICAL -> localsFirst()
            ScopeQualifier.UNQUALIFIED -> unqualified()
        }
    }
}

private fun SetQuantifier.toAstSetQuantifier(): PartiqlAst.SetQuantifier {
    val thiz = this
    return PartiqlAst.build {
        when (thiz) {
            SetQuantifier.ALL -> all()
            SetQuantifier.DISTINCT -> distinct()
        }
    }
}

private fun SelectProjection.toAstSelectProject(): PartiqlAst.Projection {
    val thiz = this
    return PartiqlAst.build {
        when(thiz) {
            is SelectProjectionValue -> projectValue(thiz.expr.toAstExpr())
            is SelectProjectionList -> {
                if(thiz.items.any { it is SelectListItemStar }) {
                    if(thiz.items.size > 1) error("More than one select item when SELECT * was present.")
                    projectStar()
                }
                else
                    projectList(
                        thiz.items.map {
                            when(it) {
                                is SelectListItemExpr -> projectExpr(it.expr.toAstExpr(), it.asName?.name)
                                is SelectListItemProjectAll -> projectAll(it.expr.toAstExpr())
                                is SelectListItemStar -> error("this should happen due to `when` branch above.")
                            }
                        })
            }
            is SelectProjectionPivot -> projectPivot(thiz.nameExpr.toAstExpr(), thiz.valueExpr.toAstExpr())
        }
    }
}

private fun FromSource.toAstFromSource(): PartiqlAst.FromSource {
    val thiz = this
    val metas = thiz.metas().toElectrolyteMetaContainer()
    return PartiqlAst.build {
        when (thiz) {
            is FromSourceExpr -> scan_(
                expr = thiz.expr.toAstExpr(),
                asAlias = thiz.variables.asName?.toSymbolPrimitive(),
                atAlias = thiz.variables.atName?.toSymbolPrimitive(),
                byAlias = thiz.variables.byName?.toSymbolPrimitive(),
                metas = metas)
            is FromSourceJoin -> {
                val jt = when (thiz.joinOp) {
                    JoinOp.INNER -> inner()
                    JoinOp.LEFT -> left()
                    JoinOp.RIGHT -> right()
                    JoinOp.OUTER -> full()
                }
                join(
                    type = jt,
                    left = thiz.leftRef.toAstFromSource(),
                    right = thiz.rightRef.toAstFromSource(),
                    predicate = if (thiz.metas.hasMeta(IsImplictJoinMeta.TAG)) null else thiz.condition.toAstExpr(),
                    metas = metas)
            }
            is FromSourceUnpivot -> unpivot_(
                expr = thiz.expr.toAstExpr(),
                asAlias = thiz.variables.asName?.toSymbolPrimitive(),
                atAlias = thiz.variables.atName?.toSymbolPrimitive(),
                byAlias = thiz.variables.byName?.toSymbolPrimitive(),
                metas = metas)
        }
    }
}

private fun PathComponent.toAstPathStep(): PartiqlAst.PathStep {
    val thiz = this
    return PartiqlAst.build {
        when (thiz) {
            is PathComponentExpr -> pathExpr(thiz.expr.toAstExpr(), thiz.case.toAstCaseSensitivity())
            is PathComponentUnpivot -> pathUnpivot(thiz.metas.toElectrolyteMetaContainer())
            is PathComponentWildcard -> pathWildcard(thiz.metas.toElectrolyteMetaContainer())
        }
    }
}

private fun DataManipulation.toAstDml(): PartiqlAst.Statement {
    val thiz = this
    return PartiqlAst.build {
        val dmlOp = thiz.dmlOperation
        val dmlOp2 = when (dmlOp) {
            is InsertOp ->
                insert(
                    dmlOp.lvalue.toAstExpr(),
                    dmlOp.values.toAstExpr())
            is InsertValueOp ->
                insertValue(
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


private fun DataType.toAstType(): PartiqlAst.Type {
    val thiz = this
    val arg1 = thiz.args.getOrNull(0)?.toLong()
    val arg2 = thiz.args.getOrNull(1)?.toLong()
    return PartiqlAst.build {
        when(thiz.sqlDataType) {
            SqlDataType.MISSING -> missingType()
            SqlDataType.NULL -> nullType()
            SqlDataType.BOOLEAN -> booleanType()
            SqlDataType.SMALLINT -> smallintType()
            SqlDataType.INTEGER -> integerType()
            SqlDataType.FLOAT -> floatType(arg1)
            SqlDataType.REAL -> realType()
            SqlDataType.DOUBLE_PRECISION -> doublePrecisionType()
            SqlDataType.DECIMAL -> decimalType(arg1, arg2)
            SqlDataType.NUMERIC -> numericType(arg1, arg2)
            SqlDataType.TIMESTAMP -> timestampType()
            SqlDataType.CHARACTER -> characterType(arg1)
            SqlDataType.CHARACTER_VARYING -> characterVaryingType(arg1)
            SqlDataType.STRING -> stringType()
            SqlDataType.SYMBOL -> symbolType()
            SqlDataType.CLOB -> clobType()
            SqlDataType.BLOB -> blobType()
            SqlDataType.STRUCT -> structType()
            SqlDataType.TUPLE -> tupleType()
            SqlDataType.LIST -> listType()
            SqlDataType.SEXP -> sexpType()
            SqlDataType.BAG -> bagType()
        }
    }
}
