package org.partiql.lang.ast

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.asPrimitive

/** Converts an [ExprNode] to a [PartiqlAst.statement]. */
fun ExprNode.toAstStatement(): PartiqlAst.Statement {
    val node = this
    return when(node) {
        is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg,
        is Typed, is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is DateTimeType,
        is Seq -> PartiqlAst.build { query(toAstExpr()) }

        is DataManipulation -> node.toAstDml()

        is CreateTable, is CreateIndex, is DropTable, is DropIndex -> toAstDdl()

        is Exec -> toAstExec()
    }
}

internal fun PartiQlMetaContainer.toIonElementMetaContainer(): IonElementMetaContainer =
    com.amazon.ionelement.api.metaContainerOf(map { it.tag to it })

private fun SymbolicName.toSymbolPrimitive() : SymbolPrimitive =
    SymbolPrimitive(this.name, this.metas.toIonElementMetaContainer())

private fun ExprNode.toAstDdl(): PartiqlAst.Statement {
    val thiz = this
    val metas = metas.toIonElementMetaContainer()

    return PartiqlAst.build {
        when(thiz) {
            is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg, is Typed,
            is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is Seq, is DateTimeType,
            is DataManipulation, is Exec -> error("Can't convert ${thiz.javaClass} to PartiqlAst.ddl")

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

private fun ExprNode.toAstExec() : PartiqlAst.Statement {
    val node = this
    val metas = metas.toIonElementMetaContainer()

    return PartiqlAst.build {
        when (node) {
            is Exec -> exec_(node.procedureName.toSymbolPrimitive(), node.args.map { it.toAstExpr() }, metas)
            else -> error("Can't convert ${node.javaClass} to PartiqlAst.Statement.Exec")
        }
    }
}

fun ExprNode.toAstExpr(): PartiqlAst.Expr {
    checkThreadInterrupted()
    val node = this
    val metas = this.metas.toIonElementMetaContainer()

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
                val symbol1 = (node.funcExpr as? VariableReference)
                    ?: error("Expected CallAgg.funcExpr to be a VariableReference")
                val symbol1Primitive = symbol1.id.asPrimitive(symbol1.metas.toIonElementMetaContainer())
                // TODO:  we are losing case-sensitivity of the function name here.  Do we care?
                callAgg_(node.setQuantifier.toAstSetQuantifier(), symbol1Primitive, node.arg.toAstExpr(), metas)
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
                    fromLet = node.fromLet?.toAstLetSource(),
                    where = node.where?.toAstExpr(),
                    order = node.orderBy?.toAstOrderBySpec(),
                    group = node.groupBy?.toAstGroupSpec(),
                    having = node.having?.toAstExpr(),
                    limit = node.limit?.toAstExpr(),
                    metas = metas)
            is Struct -> struct(node.fields.map { exprPair(it.name.toAstExpr(), it.expr.toAstExpr()) }, metas)
            is Seq ->
                when(node.type) {
                    SeqType.LIST -> list(node.values.map { it.toAstExpr() }, metas)
                    SeqType.SEXP -> sexp(node.values.map { it.toAstExpr() }, metas)
                    SeqType.BAG -> bag(node.values.map { it.toAstExpr() }, metas)
                }

            // These are handled by `toAstDml()`, `toAstDdl()`, and `toAstExec()`
            is DataManipulation, is CreateTable, is CreateIndex, is DropTable, is DropIndex, is Exec ->
                error("Can't transform ${node.javaClass} to a PartiqlAst.expr }")
            // DateTime types
            is DateTimeType -> {
                when (node) {
                    is DateTimeType.Date -> date(node.year.toLong(), node.month.toLong(), node.day.toLong(), metas)
                    is DateTimeType.Time -> litTime(
                        timeValue(
                            node.hour.toLong(),
                            node.minute.toLong(),
                            node.second.toLong(),
                            node.nano.toLong(),
                            node.precision.toLong(),
                            node.tz_minutes?.toLong()
                        )
                    )
                }
            }
        }
    }
}

private fun OrderBy.toAstOrderBySpec(): PartiqlAst.OrderBy {
    val thiz = this
    return PartiqlAst.build {
        orderBy(
            thiz.sortSpecItems.map { sortSpec(it.expr.toAstExpr(), it.orderingSpec.toAstOrderSpec()) }
        )
    }
}

private fun OrderingSpec?.toAstOrderSpec(): PartiqlAst.OrderingSpec =
    PartiqlAst.build {
        when (this@toAstOrderSpec) {
            OrderingSpec.DESC -> desc()
            else -> asc()
        }
    }

private fun GroupBy.toAstGroupSpec(): PartiqlAst.GroupBy =
    PartiqlAst.build {
        groupBy_(
            this@toAstGroupSpec.grouping.toAstGroupStrategy(),
            groupKeyList(this@toAstGroupSpec.groupByItems.map {
                val keyMetas = it.asName?.metas?.toIonElementMetaContainer() ?: emptyMetaContainer()
                groupKey_(it.expr.toAstExpr(), it.asName?.name?.asPrimitive(keyMetas) )
            }),
            this@toAstGroupSpec.groupName?.name?.asPrimitive(this@toAstGroupSpec.groupName.metas.toIonElementMetaContainer()))
    }


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
                    val metas = (thiz.items[0] as SelectListItemStar).metas.toIonElementMetaContainer()
                    projectStar(metas)
                }
                else
                    projectList(
                        thiz.items.map {
                            when(it) {
                                is SelectListItemExpr -> projectExpr_(it.expr.toAstExpr(), it.asName?.toPrimitive())
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
    val metas = thiz.metas().toIonElementMetaContainer()
    return PartiqlAst.build {
        when (thiz) {
            is FromSourceExpr -> scan_(
                thiz.expr.toAstExpr(),
                thiz.variables.asName?.toPrimitive(),
                thiz.variables.atName?.toPrimitive(),
                thiz.variables.byName?.toPrimitive(),
                thiz.expr.metas.toIonElementMetaContainer())
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
            is FromSourceUnpivot -> unpivot_(
                thiz.expr.toAstExpr(),
                thiz.variables.asName?.toPrimitive(),
                thiz.variables.atName?.toPrimitive(),
                thiz.variables.byName?.toPrimitive(),
                thiz.metas.toIonElementMetaContainer())
        }
    }
}

private fun LetSource.toAstLetSource(): PartiqlAst.Let {
    val thiz = this
    return PartiqlAst.build {
        let(
            thiz.bindings.map {
                letBinding(it.expr.toAstExpr(), it.name.name)
            }
        )
    }
}

private fun PathComponent.toAstPathStep(): PartiqlAst.PathStep {
    val thiz = this
    return PartiqlAst.build {
        when (thiz) {
            is PathComponentExpr -> pathExpr(thiz.expr.toAstExpr(), thiz.case.toAstCaseSensitivity())
            is PathComponentUnpivot -> pathUnpivot(thiz.metas.toIonElementMetaContainer())
            is PathComponentWildcard -> pathWildcard(thiz.metas.toIonElementMetaContainer())
        }
    }
}

private fun OnConflict.toAstOnConflict(): PartiqlAst.OnConflict {
    val thiz = this
    return PartiqlAst.build {
        when(thiz.conflictAction) {
            ConflictAction.DO_NOTHING -> onConflict(thiz.condition.toAstExpr(), doNothing())
        }
    }
}

private fun DataManipulation.toAstDml(): PartiqlAst.Statement {
    val thiz = this
    return PartiqlAst.build {
        val dmlOps = thiz.dmlOperations
        val dmlOps2 = dmlOps.toAstDmlOps(thiz)

        dml(
            dmlOps2,
            thiz.from?.toAstFromSource(),
            thiz.where?.toAstExpr(),
            thiz.returning?.toAstReturningExpr(),
            thiz.metas.toIonElementMetaContainer())
    }
}

private fun DmlOpList.toAstDmlOps(dml: DataManipulation): PartiqlAst.DmlOpList =
    PartiqlAst.build {
        dmlOpList(
            this@toAstDmlOps.ops.map {
                it.toAstDmlOp(dml)
            },
            metas = dml.metas.toIonElementMetaContainer())
    }
private fun DataManipulationOperation.toAstDmlOp(dml: DataManipulation): PartiqlAst.DmlOp =
    PartiqlAst.build {
        when (val thiz = this@toAstDmlOp) {
            is InsertOp ->
                insert(
                    thiz.lvalue.toAstExpr(),
                    thiz.values.toAstExpr())
            is InsertValueOp ->
                insertValue(
                    thiz.lvalue.toAstExpr(),
                    thiz.value.toAstExpr(),
                    thiz.position?.toAstExpr(),
                    thiz.onConflict?.toAstOnConflict(),
                    dml.metas.toIonElementMetaContainer())
            is AssignmentOp ->
                set(
                    assignment(
                        thiz.assignment.lvalue.toAstExpr(),
                        thiz.assignment.rvalue.toAstExpr()))
            is RemoveOp -> remove(thiz.lvalue.toAstExpr())
            DeleteOp -> delete()
        }
    }

private fun ReturningExpr.toAstReturningExpr(): PartiqlAst.ReturningExpr {
    val thiz = this
    return PartiqlAst.build {
        returningExpr(
            thiz.returningElems.map {
                returningElem(it.returningMapping.toReturningMapping(), it.columnComponent.toColumnComponent())
            }
        )
    }
}

private fun ColumnComponent.toColumnComponent(): PartiqlAst.ColumnComponent {
    return PartiqlAst.build {
        when (val thiz = this@toColumnComponent) {
            is ReturningWildcard -> returningWildcard()
            is ReturningColumn -> returningColumn(thiz.column.toAstExpr())
        }
    }
}

private fun ReturningMapping.toReturningMapping(): PartiqlAst.ReturningMapping {
    return PartiqlAst.build {
        when (this@toReturningMapping) {
            ReturningMapping.MODIFIED_OLD -> modifiedOld()
            ReturningMapping.MODIFIED_NEW -> modifiedNew()
            ReturningMapping.ALL_OLD -> allOld()
            ReturningMapping.ALL_NEW -> allNew()
        }
    }
}

private fun DataType.toAstType(): PartiqlAst.Type {
    val thiz = this
    val metas = thiz.metas.toIonElementMetaContainer()
    val arg1 = thiz.args.getOrNull(0)?.toLong()
    val arg2 = thiz.args.getOrNull(1)?.toLong()
    return PartiqlAst.build {
        when(thiz.sqlDataType) {
            SqlDataType.MISSING -> missingType(metas)
            SqlDataType.NULL -> nullType(metas)
            SqlDataType.BOOLEAN -> booleanType(metas)
            SqlDataType.SMALLINT -> smallintType(metas)
            SqlDataType.INTEGER -> integerType(metas)
            SqlDataType.FLOAT -> floatType(arg1, metas)
            SqlDataType.REAL -> realType(metas)
            SqlDataType.DOUBLE_PRECISION -> doublePrecisionType(metas)
            SqlDataType.DECIMAL -> decimalType(arg1, arg2, metas)
            SqlDataType.NUMERIC -> numericType(arg1, arg2, metas)
            SqlDataType.TIMESTAMP -> timestampType(metas)
            SqlDataType.CHARACTER -> characterType(arg1, metas)
            SqlDataType.CHARACTER_VARYING -> characterVaryingType(arg1, metas)
            SqlDataType.STRING -> stringType(metas)
            SqlDataType.SYMBOL -> symbolType(metas)
            SqlDataType.CLOB -> clobType(metas)
            SqlDataType.BLOB -> blobType(metas)
            SqlDataType.STRUCT -> structType(metas)
            SqlDataType.TUPLE -> tupleType(metas)
            SqlDataType.LIST -> listType(metas)
            SqlDataType.SEXP -> sexpType(metas)
            SqlDataType.BAG -> bagType(metas)
            SqlDataType.DATE -> dateType(metas)
            SqlDataType.TIME -> timeType(arg1, metas)
            SqlDataType.TIME_WITH_TIME_ZONE -> timeWithTimeZoneType(arg1, metas)
        }
    }
}


private fun SymbolicName.toPrimitive(): SymbolPrimitive =
    SymbolPrimitive(this.name, this.metas.toIonElementMetaContainer())