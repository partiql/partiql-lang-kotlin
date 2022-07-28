// We don't need warnings about deprecated ExprNode.
@file: Suppress("DEPRECATION")

package org.partiql.lang.ast

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.BuiltInScalarTypeId
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.pig.runtime.asPrimitive
import org.partiql.pig.runtime.toIonElement

/** Converts an [ExprNode] to a [PartiqlAst.statement]. */
fun ExprNode.toAstStatement(): PartiqlAst.Statement {
    val node = this
    return when (node) {
        is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg,
        is Typed, is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is DateLiteral, is TimeLiteral,
        is Seq, is NullIf, is Coalesce -> PartiqlAst.build { query(toAstExpr()) }

        is DataManipulation -> node.toAstDml()

        is CreateTable, is CreateIndex, is DropTable, is DropIndex -> toAstDdl()

        is Exec -> toAstExec()
    }
}

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
internal fun PartiQlMetaContainer.toIonElementMetaContainer(): IonElementMetaContainer =
    com.amazon.ionelement.api.metaContainerOf(map { it.tag to it })

private fun SymbolicName.toSymbolPrimitive(): SymbolPrimitive =
    SymbolPrimitive(this.name, this.metas.toIonElementMetaContainer())

private fun ExprNode.toAstDdl(): PartiqlAst.Statement {
    val thiz = this
    val metas = metas.toIonElementMetaContainer()

    return PartiqlAst.build {
        when (thiz) {
            is Literal, is LiteralMissing, is VariableReference, is Parameter, is NAry, is CallAgg, is Typed,
            is Path, is SimpleCase, is SearchedCase, is Select, is Struct, is Seq, is DateLiteral, is TimeLiteral,
            is NullIf, is Coalesce, is DataManipulation, is Exec -> error("Can't convert ${thiz.javaClass} to PartiqlAst.ddl")

            is CreateTable -> ddl(createTable(thiz.tableName), metas)
            is CreateIndex ->
                ddl(
                    createIndex(
                        identifier(thiz.tableId.id, thiz.tableId.case.toAstCaseSensitivity()),
                        thiz.keys.map { it.toAstExpr() }
                    ),
                    metas
                )
            is DropIndex ->
                ddl(
                    dropIndex(
                        // case-sensitivity of table names cannot be represented with ExprNode.
                        identifier(thiz.tableId.id, thiz.tableId.case.toAstCaseSensitivity()),
                        identifier(thiz.indexId.id, thiz.indexId.case.toAstCaseSensitivity())
                    ),
                    metas
                )
            is DropTable ->
                // case-sensitivity of table names cannot be represented with ExprNode.
                ddl(
                    dropTable(
                        identifier(thiz.tableId.id, thiz.tableId.case.toAstCaseSensitivity())
                    ),
                    metas
                )
        }
    }
}

private fun ExprNode.toAstExec(): PartiqlAst.Statement {
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
            is VariableReference -> id(
                node.id,
                node.case.toAstCaseSensitivity(),
                node.scopeQualifier.toAstScopeQualifier(),
                metas
            )
            is Parameter -> parameter(node.position.toLong(), metas)
            is NAry -> {
                val args = node.args.map { it.toAstExpr() }
                when (node.op) {
                    NAryOp.ADD -> when (args.size) {
                        0 -> throw IllegalArgumentException("Operator 'Add' must have at least one argument")
                        1 -> pos(args.first(), metas)
                        else -> plus(args, metas)
                    }
                    NAryOp.SUB -> when (args.size) {
                        0 -> throw IllegalArgumentException("Operator 'Sub' must have at least one argument")
                        1 -> neg(args.first(), metas)
                        else -> minus(args, metas)
                    }
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
                    NAryOp.UNION -> union(distinct(), args, metas)
                    NAryOp.UNION_ALL -> union(all(), args, metas)
                    NAryOp.INTERSECT -> intersect(distinct(), args, metas)
                    NAryOp.INTERSECT_ALL -> intersect(all(), args, metas)
                    NAryOp.EXCEPT -> except(distinct(), args, metas)
                    NAryOp.EXCEPT_ALL -> except(all(), args, metas)
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
                when (node.op) {
                    TypedOp.CAST -> cast(node.expr.toAstExpr(), node.type.toAstType(), metas)
                    TypedOp.CAN_CAST -> canCast(node.expr.toAstExpr(), node.type.toAstType(), metas)
                    TypedOp.CAN_LOSSLESS_CAST -> canLosslessCast(node.expr.toAstExpr(), node.type.toAstType(), metas)
                    TypedOp.IS -> isType(node.expr.toAstExpr(), node.type.toAstType(), metas)
                }
            is Path -> path(node.root.toAstExpr(), node.components.map { it.toAstPathStep() }, metas)
            is SimpleCase ->
                simpleCase(
                    node.valueExpr.toAstExpr(),
                    exprPairList(node.whenClauses.map { exprPair(it.valueExpr.toAstExpr(), it.thenExpr.toAstExpr()) }),
                    node.elseExpr?.toAstExpr(),
                    metas
                )
            is SearchedCase ->
                searchedCase(
                    exprPairList(node.whenClauses.map { exprPair(it.condition.toAstExpr(), it.thenExpr.toAstExpr()) }),
                    node.elseExpr?.toAstExpr(),
                    metas
                )
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
                    offset = node.offset?.toAstExpr(),
                    metas = metas
                )
            is Struct -> struct(node.fields.map { exprPair(it.name.toAstExpr(), it.expr.toAstExpr()) }, metas)
            is Seq ->
                when (node.type) {
                    SeqType.LIST -> list(node.values.map { it.toAstExpr() }, metas)
                    SeqType.SEXP -> sexp(node.values.map { it.toAstExpr() }, metas)
                    SeqType.BAG -> bag(node.values.map { it.toAstExpr() }, metas)
                }

            // These are handled by `toAstDml()`, `toAstDdl()`, and `toAstExec()`
            is DataManipulation, is CreateTable, is CreateIndex, is DropTable, is DropIndex, is Exec ->
                error("Can't transform ${node.javaClass} to a PartiqlAst.expr }")
            is NullIf -> nullIf(node.expr1.toAstExpr(), node.expr2.toAstExpr(), metas)
            is Coalesce -> coalesce(node.args.map { it.toAstExpr() }, metas)
            // DateTime types
            is DateLiteral -> date(node.year.toLong(), node.month.toLong(), node.day.toLong(), metas)
            is TimeLiteral ->
                litTime(
                    timeValue(
                        node.hour.toLong(),
                        node.minute.toLong(),
                        node.second.toLong(),
                        node.nano.toLong(),
                        node.precision.toLong(),
                        node.with_time_zone,
                        node.tz_minutes?.toLong()
                    ),
                    metas
                )
        }
    }
}

private fun OrderBy.toAstOrderBySpec(): PartiqlAst.OrderBy {
    val thiz = this
    return PartiqlAst.build {
        orderBy(
            thiz.sortSpecItems.map {
                sortSpec(
                    it.expr.toAstExpr(),
                    it.orderingSpec?.toAstOrderSpec(),
                    it.nullsSpec?.toAstNullsSpec()
                )
            }
        )
    }
}

private fun OrderingSpec.toAstOrderSpec(): PartiqlAst.OrderingSpec =
    PartiqlAst.build {
        when (this@toAstOrderSpec) {
            OrderingSpec.ASC -> asc()
            OrderingSpec.DESC -> desc()
        }
    }

private fun NullsSpec.toAstNullsSpec(): PartiqlAst.NullsSpec =
    PartiqlAst.build {
        when (this@toAstNullsSpec) {
            NullsSpec.FIRST -> nullsFirst()
            NullsSpec.LAST -> nullsLast()
        }
    }

private fun GroupBy.toAstGroupSpec(): PartiqlAst.GroupBy =
    PartiqlAst.build {
        groupBy_(
            this@toAstGroupSpec.grouping.toAstGroupStrategy(),
            groupKeyList(
                this@toAstGroupSpec.groupByItems.map {
                    val keyMetas = it.asName?.metas?.toIonElementMetaContainer() ?: emptyMetaContainer()
                    groupKey_(it.expr.toAstExpr(), it.asName?.name?.asPrimitive(keyMetas))
                }
            ),
            this@toAstGroupSpec.groupName?.name?.asPrimitive(this@toAstGroupSpec.groupName.metas.toIonElementMetaContainer())
        )
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
        when (thiz) {
            is SelectProjectionValue -> projectValue(thiz.expr.toAstExpr(), thiz.metas.toIonElementMetaContainer())
            is SelectProjectionList -> {
                if (thiz.items.any { it is SelectListItemStar }) {
                    if (thiz.items.size > 1) error("More than one select item when SELECT * was present.")
                    val metas = (thiz.items[0] as SelectListItemStar).metas.toIonElementMetaContainer()
                    projectStar(metas)
                } else {
                    projectList(
                        thiz.items.map {
                            when (it) {
                                is SelectListItemExpr -> projectExpr_(
                                    it.expr.toAstExpr(),
                                    it.asName?.toPrimitive(),
                                    it.expr.metas.toIonElementMetaContainer()
                                )
                                is SelectListItemProjectAll -> projectAll(
                                    it.expr.toAstExpr(),
                                    it.expr.metas.toIonElementMetaContainer()
                                )
                                is SelectListItemStar -> error("this should happen due to `when` branch above.")
                            }
                        },
                        metas = thiz.metas.toIonElementMetaContainer()
                    )
                }
            }
            is SelectProjectionPivot -> projectPivot(
                thiz.nameExpr.toAstExpr(),
                thiz.valueExpr.toAstExpr(),
                thiz.metas.toIonElementMetaContainer()
            )
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
                thiz.expr.metas.toIonElementMetaContainer()
            )
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
                    metas = metas
                )
            }
            is FromSourceUnpivot -> unpivot_(
                thiz.expr.toAstExpr(),
                thiz.variables.asName?.toPrimitive(),
                thiz.variables.atName?.toPrimitive(),
                thiz.variables.byName?.toPrimitive(),
                thiz.metas.toIonElementMetaContainer()
            )
        }
    }
}

private fun LetSource.toAstLetSource(): PartiqlAst.Let {
    val thiz = this
    return PartiqlAst.build {
        let(
            thiz.bindings.map {
                letBinding_(it.expr.toAstExpr(), it.name.toSymbolPrimitive())
            }
        )
    }
}

private fun PathComponent.toAstPathStep(): PartiqlAst.PathStep {
    val thiz = this
    val metas = thiz.metas.toIonElementMetaContainer()
    return PartiqlAst.build {
        when (thiz) {
            is PathComponentExpr -> pathExpr(thiz.expr.toAstExpr(), thiz.case.toAstCaseSensitivity(), metas)
            is PathComponentUnpivot -> pathUnpivot(metas)
            is PathComponentWildcard -> pathWildcard(metas)
        }
    }
}

private fun OnConflict.toAstOnConflict(): PartiqlAst.OnConflict {
    val thiz = this
    return PartiqlAst.build {
        when (thiz.conflictAction) {
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
            thiz.metas.toIonElementMetaContainer()
        )
    }
}

private fun DmlOpList.toAstDmlOps(dml: DataManipulation): PartiqlAst.DmlOpList =
    PartiqlAst.build {
        dmlOpList(
            this@toAstDmlOps.ops.map {
                it.toAstDmlOp(dml)
            },
            metas = dml.metas.toIonElementMetaContainer()
        )
    }

private fun DataManipulationOperation.toAstDmlOp(dml: DataManipulation): PartiqlAst.DmlOp =
    PartiqlAst.build {
        when (val thiz = this@toAstDmlOp) {
            is InsertOp ->
                insert(
                    thiz.lvalue.toAstExpr(),
                    thiz.values.toAstExpr()
                )
            is InsertValueOp ->
                insertValue(
                    thiz.lvalue.toAstExpr(),
                    thiz.value.toAstExpr(),
                    thiz.position?.toAstExpr(),
                    thiz.onConflict?.toAstOnConflict(),
                    dml.metas.toIonElementMetaContainer()
                )
            is AssignmentOp ->
                set(
                    assignment(
                        thiz.assignment.lvalue.toAstExpr(),
                        thiz.assignment.rvalue.toAstExpr()
                    )
                )
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

fun DataType.toAstType(): PartiqlAst.Type {
    val thiz = this
    val metas = thiz.metas.toIonElementMetaContainer()
    val arg1 = thiz.args.getOrNull(0)?.toLong()
    val arg2 = thiz.args.getOrNull(1)?.toLong()

    return PartiqlAst.build {
        when (thiz.sqlDataType) {
            SqlDataType.MISSING -> missingType(metas)
            SqlDataType.NULL -> nullType(metas)
            SqlDataType.BOOLEAN -> scalarType(BuiltInScalarTypeId.BOOLEAN, metas = metas)
            SqlDataType.SMALLINT -> scalarType(BuiltInScalarTypeId.SMALLINT, metas = metas)
            SqlDataType.INTEGER4 -> scalarType(BuiltInScalarTypeId.INTEGER4, metas = metas)
            SqlDataType.INTEGER8 -> scalarType(BuiltInScalarTypeId.INTEGER8, metas = metas)
            SqlDataType.INTEGER -> scalarType(BuiltInScalarTypeId.INTEGER, metas = metas)
            SqlDataType.FLOAT -> scalarType(BuiltInScalarTypeId.FLOAT, listOfNotNull(arg1), metas = metas)
            SqlDataType.REAL -> scalarType(BuiltInScalarTypeId.REAL, metas = metas)
            SqlDataType.DOUBLE_PRECISION -> scalarType(BuiltInScalarTypeId.DOUBLE_PRECISION, metas = metas)
            SqlDataType.DECIMAL -> scalarType(BuiltInScalarTypeId.DECIMAL, listOfNotNull(arg1, arg2), metas)
            SqlDataType.NUMERIC -> scalarType(BuiltInScalarTypeId.NUMERIC, listOfNotNull(arg1, arg2), metas)
            SqlDataType.TIMESTAMP -> scalarType(BuiltInScalarTypeId.TIMESTAMP, metas = metas)
            SqlDataType.CHARACTER -> scalarType(BuiltInScalarTypeId.CHARACTER, listOfNotNull(arg1), metas)
            SqlDataType.CHARACTER_VARYING -> scalarType(BuiltInScalarTypeId.CHARACTER_VARYING, listOfNotNull(arg1), metas)
            SqlDataType.STRING -> scalarType(BuiltInScalarTypeId.STRING, metas = metas)
            SqlDataType.SYMBOL -> scalarType(BuiltInScalarTypeId.SYMBOL, metas = metas)
            SqlDataType.CLOB -> scalarType(BuiltInScalarTypeId.CLOB, metas = metas)
            SqlDataType.BLOB -> scalarType(BuiltInScalarTypeId.BLOB, metas = metas)
            SqlDataType.STRUCT -> structType(metas)
            SqlDataType.TUPLE -> tupleType(metas)
            SqlDataType.LIST -> listType(metas)
            SqlDataType.SEXP -> sexpType(metas)
            SqlDataType.BAG -> bagType(metas)
            SqlDataType.DATE -> scalarType(BuiltInScalarTypeId.DATE, metas = metas)
            SqlDataType.TIME -> scalarType(BuiltInScalarTypeId.TIME, listOfNotNull(arg1), metas)
            SqlDataType.TIME_WITH_TIME_ZONE -> scalarType(BuiltInScalarTypeId.TIME_WITH_TIME_ZONE, listOfNotNull(arg1), metas)
            SqlDataType.ANY -> anyType(metas)
            is SqlDataType.CustomDataType -> customType(thiz.sqlDataType.name.toLowerCase(), metas)
        }
    }
}

private fun SymbolicName.toPrimitive(): SymbolPrimitive =
    SymbolPrimitive(this.name, this.metas.toIonElementMetaContainer())
