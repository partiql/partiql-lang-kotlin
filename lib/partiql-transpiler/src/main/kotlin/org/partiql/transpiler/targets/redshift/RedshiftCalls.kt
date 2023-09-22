package org.partiql.transpiler.targets.redshift

import org.partiql.ast.Ast
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.builder.AstFactory
import org.partiql.transpiler.ProblemCallback
import org.partiql.transpiler.error
import org.partiql.transpiler.info
import org.partiql.transpiler.sql.SqlArgs
import org.partiql.transpiler.sql.SqlCallFn
import org.partiql.transpiler.sql.SqlCalls
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class)
public class RedshiftCalls(private val onProblem: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = id("dateadd")
        onProblem.info("PartiQL `date_add` was replaced by Redshift `dateadd`")
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr = Ast.create {
        val id = id("datediff")
        onProblem.info("PartiQL `date_diff` was replaced by Redshift `datediff`")
        val arg0 = exprLit(symbolValue(part.name.lowercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    private fun utcnow(args: SqlArgs): Expr = Ast.create {
        val id = id("sysdate")
        onProblem.info("PartiQL `utcnow()` was replaced by Redshift `SYSDATE`")
        exprVar(id, Expr.Var.Scope.DEFAULT)
    }

    override fun rewriteCast(type: PartiQLValueType, args: SqlArgs): Expr = Ast.create {
        when (type) {
            PartiQLValueType.ANY -> {
                onProblem.error("PartiQL `ANY` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INT8 -> {
                onProblem.error("PartiQL `INT8` type (1-byte integer) not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INT -> {
                onProblem.error("PartiQL `INT` type (arbitrary precision integer) not supported in Redshift")
                // this needs a extra safety renaming because int refers to int4 in redshift.
                exprCast(args[0].expr, typeCustom("Arbitrary Precision Integer"))
            }
            PartiQLValueType.MISSING -> {
                onProblem.error("PartiQL `MISSING` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.SYMBOL -> {
                onProblem.error("PartiQL `SYMBOL` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INTERVAL -> {
                onProblem.error("PartiQL `INTERVAL` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.BLOB -> {
                onProblem.error("PartiQL `BLOB` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.CLOB -> {
                onProblem.error("PartiQL `CLOB` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.BAG -> {
                onProblem.error("PartiQL `BAG` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.LIST -> {
                onProblem.error("PartiQL `LIST` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.SEXP -> {
                onProblem.error("PartiQL `SEXP` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.STRUCT -> {
                onProblem.error("PartiQL `STRUCT` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            // using the customer type to rename type
            PartiQLValueType.FLOAT32 -> exprCast(args[0].expr, typeCustom("FLOAT4"))
            PartiQLValueType.FLOAT64 -> exprCast(args[0].expr, typeCustom("FLOAT8"))
            PartiQLValueType.BINARY -> exprCast(args[0].expr, typeCustom("VARBYTE"))
            PartiQLValueType.BYTE -> TODO("Mapping to VARBYTE(1), do this after supporting parameterized type")
            else -> super.rewriteCast(type, args)
        }
    }

    private fun AstFactory.id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
