package org.partiql.eval.internal.plan

import org.partiql.eval.ExprValue
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal sealed class PExpr {
    data class Lit(val value: Datum) : PExpr()
    data class Var(val depth: Int, val offset: Int) : PExpr()
    data class TableRef(val catalogId: Int, val tableId: Int) : PExpr()
    data class TableDirect(val table: Table) : PExpr()
    data class Call(val fn: Fn, val args: List<PExpr>) : PExpr()
    data class DynamicCall(val name: String, val overloads: List<FnOverload>, val args: List<PExpr>) : PExpr()
    data class Cast(val operand: PExpr, val target: PType) : PExpr()
    data class Case(val branches: List<Branch>, val default: PExpr?) : PExpr()
    data class Branch(val condition: PExpr, val result: PExpr)
    data class NullIf(val v1: PExpr, val v2: PExpr) : PExpr()
    data class Coalesce(val args: List<PExpr>) : PExpr()
    data class Array(val values: List<PExpr>) : PExpr()
    data class Bag(val values: List<PExpr>) : PExpr()
    data class Struct(val fields: List<Field>) : PExpr()
    data class Field(val key: PExpr, val value: PExpr)
    data class Spread(val args: List<PExpr>) : PExpr()
    data class Select(val input: PRel, val constructor: PExpr, val ordered: Boolean) : PExpr()
    data class Pivot(val input: PRel, val key: PExpr, val value: PExpr) : PExpr()
    data class Subquery(val input: PRel, val constructor: PExpr, val scalar: Boolean) : PExpr()
    data class PathKey(val root: PExpr, val key: PExpr) : PExpr()
    data class PathIndex(val root: PExpr, val index: PExpr) : PExpr()
    data class PathSymbol(val root: PExpr, val symbol: String) : PExpr()
    data class Map(val keyType: PType, val valueType: PType, val entries: List<Field>) : PExpr()
    data class MapDynamic(val entries: List<Field>) : PExpr()
    data class Error(val type: PType) : PExpr()
    class Custom(val factory: () -> ExprValue) : PExpr()
}
