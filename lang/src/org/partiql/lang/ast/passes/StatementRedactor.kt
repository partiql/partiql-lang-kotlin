package org.partiql.lang.ast.passes

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.StringElement
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.SqlParser

/**
 * This is a function alias for determining which UDF input arguments need to be redacted.
 *
 * There are two components needed for implementation:
 *     1. Which arguments are needed for [SafeFieldName] validation
 *     2. Which arguments are needed for redaction should be returned
 *
 * For example, for a given function in which argument number is static, func(a, b, c, d),
 * we can validate whether `a` and `b` are a [SafeFieldName], if yes, `c` and `d` will be redacted.
 */
typealias UserDefinedFunctionRedactionLambda = (List<PartiqlAst.Expr>) -> List<PartiqlAst.Expr>

private val ion = IonSystemBuilder.standard().build()
private val parser = SqlParser(ion)
private const val maskPattern = "***(Redacted)"

const val INVALID_NUM_ARGS = "Invalid number of args in node"
const val INPUT_AST_STATEMENT_MISMATCH = "Unable to redact the statement. Please check that the input ast is the parsed result of the input statement"

/**
 * Returns true if the given [node] type is to be skipped for redaction or its text is one of the [safeFieldNames].
 */
fun skipRedaction(node: PartiqlAst.Expr, safeFieldNames: Set<String>): Boolean {
    if (safeFieldNames.isEmpty()) {
        return false
    }

    return when (node) {
        is PartiqlAst.Expr.Id -> safeFieldNames.contains(node.name.text)
        is PartiqlAst.Expr.Lit -> {
            when (node.value) {
                is StringElement -> safeFieldNames.contains(node.value.stringValue)
                else -> false
            }
        }
        is PartiqlAst.Expr.Path -> {
            node.steps.any {
                when (it) {
                    is PartiqlAst.PathStep.PathExpr -> skipRedaction(it.index, safeFieldNames)
                    else -> false
                }
            }
        }
        else -> true // Skip redaction for other nodes
    }
}

/**
 * From the input PartiQL [statement], returns a statement in which [PartiqlAst.Expr.Lit]s not assigned with
 * [providedSafeFieldNames] are redacted to "***(Redacted)".
 *
 * [providedSafeFieldNames] is an optional set of fields whose values are not to be redacted. If no set is provided,
 * all literals will be redacted.
 *     For example, given a [providedSafeFieldNames] of Set('hashkey')
 *     The query of `SELECT * FROM tb WHERE hashkey = 'a' AND attr = 12345` will be redacted to:
 *                  `SELECT * FROM tb WHERE hashkey = 'a' AND attr = ***(Redacted)`
 * [userDefinedFunctionRedactionConfig] is an optional mapping of UDF names to functions determining which call
 * arguments are to be redacted. For an example, please check StatementRedactorTest.kt for more details.
 */
fun redact(statement: String,
           providedSafeFieldNames: Set<String> = emptySet(),
           userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda> = emptyMap()): String {
    return redact(statement, parser.parseExprNode(statement), providedSafeFieldNames, userDefinedFunctionRedactionConfig)
}

/**
 * From the input PartiQL [statement], returns a statement in which [PartiqlAst.Expr.Lit]s not assigned with
 * [providedSafeFieldNames] are redacted to "***(Redacted)". Assumes that the parsed PartiQL [statement] is the same
 * as the input [ast].
 *
 * [providedSafeFieldNames] is an optional set of fields whose values are not to be redacted. If no set is provided,
 * all literals will be redacted.
 *     For example, given a [providedSafeFieldNames] of Set('hashkey')
 *     The query of `SELECT * FROM tb WHERE hashkey = 'a' AND attr = 12345` will be redacted to:
 *                  `SELECT * FROM tb WHERE hashkey = 'a' AND attr = ***(Redacted)`
 * [userDefinedFunctionRedactionConfig] is an optional mapping of UDF names to functions determining which call
 * arguments are to be redacted. For an example, please check StatementRedactorTest.kt for more details.
 */
fun redact(statement: String,
           ast: ExprNode,
           providedSafeFieldNames: Set<String> = emptySet(),
           userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda> = emptyMap()): String {

    val partiqlAst = ast.toAstStatement()
    val statementRedactionVisitor = StatementRedactionVisitor(statement, providedSafeFieldNames, userDefinedFunctionRedactionConfig)
    statementRedactionVisitor.walkStatement(partiqlAst)
    return statementRedactionVisitor.getRedactedStatement()
}

/**
 * Redact [PartiqlAst.Expr.Lit]s not assigned with [safeFieldNames] to "***(Redacted)". Function calls that have an
 * entry in [userDefinedFunctionRedactionConfig] will have their arguments redacted based on the redaction lambda.
 */
private class StatementRedactionVisitor(
    private val statement: String,
    private val safeFieldNames: Set<String>,
    private val userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda>
) : PartiqlAst.Visitor() {
    private val sourceLocationMetaForRedaction = arrayListOf<SourceLocationMeta>()

    /**
     * Returns the redacted [statement].
     */
    fun getRedactedStatement(): String {
        val lines = statement.lines()
        val totalCharactersInPreviousLines = IntArray(lines.size)
        for (lineNum in 1 until lines.size) {
            totalCharactersInPreviousLines[lineNum] = totalCharactersInPreviousLines[lineNum - 1] + lines[lineNum - 1].length + 1
        }

        val redactedStatement = StringBuilder(statement)
        var offset = 0
        sourceLocationMetaForRedaction.sortWith(compareBy<SourceLocationMeta> { it.lineNum }.thenBy { it.charOffset })

        sourceLocationMetaForRedaction.map {
            val length = it.length.toInt()
            val lineNum = it.lineNum.toInt()
            if (lineNum < 1 || lineNum > totalCharactersInPreviousLines.size) {
                throw IllegalArgumentException("$INPUT_AST_STATEMENT_MISMATCH, line number: $lineNum")
            }
            val start = totalCharactersInPreviousLines[lineNum - 1] + it.charOffset.toInt() - 1 + offset
            if (start < 0 || length < 0 || start >= redactedStatement.length || start > redactedStatement.length - length) {
                throw IllegalArgumentException(INPUT_AST_STATEMENT_MISMATCH)
            }
            redactedStatement.replace(start, start + length, maskPattern)
            offset = offset + maskPattern.length - length
        }
        return redactedStatement.toString()
    }

    override fun visitExprSelect(node: PartiqlAst.Expr.Select) {
        node.where?.let { redactExpr(it) }
    }

    override fun visitStatementDml(node: PartiqlAst.Statement.Dml) {
        node.where?.let { redactExpr(it) }
    }

    override fun visitAssignment(node: PartiqlAst.Assignment) {
        if (!skipRedaction(node.target, safeFieldNames)) {
            redactExpr(node.value)
        }
    }

    override fun visitDmlOpInsertValue(node: PartiqlAst.DmlOp.InsertValue) {
        when (node.value) {
            is PartiqlAst.Expr.Struct -> redactStructInInsertValueOp(node.value)
            else -> redactExpr(node.value)
        }
    }

    private fun redactExpr(node: PartiqlAst.Expr) {
        if (node.isNAry()) {
            redactNAry(node)
        }

        else when (node) {
            is PartiqlAst.Expr.Lit -> redactLiteral(node)
            is PartiqlAst.Expr.List -> redactSeq(node)
            is PartiqlAst.Expr.Sexp -> redactSeq(node)
            is PartiqlAst.Expr.Bag -> redactSeq(node)
            is PartiqlAst.Expr.Struct -> redactStruct(node)
            is PartiqlAst.Expr.IsType -> redactTypes(node)
            else -> { /* other nodes are not currently redacted */ }
        }
    }

    private fun redactLogicalOp(args: List<PartiqlAst.Expr>) { args.forEach { redactExpr(it) } }

    private fun redactComparisonOp(args: List<PartiqlAst.Expr>) {
        if (args.size != 2) {
            throw IllegalArgumentException(INVALID_NUM_ARGS)
        }
        if (!skipRedaction(args[0], safeFieldNames)) {
            redactExpr(args[1])
        }
    }

    private fun plusMinusRedaction(args: List<PartiqlAst.Expr>) {
        when (args.size) {
            1 -> redactExpr(args[0])
            2 -> {
                redactExpr(args[0])
                redactExpr(args[1])
            }
            else -> throw IllegalArgumentException(INVALID_NUM_ARGS)
        }
    }

    private fun arithmeticOpRedaction(args: List<PartiqlAst.Expr>) {
        if (args.size != 2) {
            throw IllegalArgumentException(INVALID_NUM_ARGS)
        }
        redactExpr(args[0])
        redactExpr(args[1])
    }

    private fun redactNAry(node: PartiqlAst.Expr) {
        when (node) {
            // Logical Ops
            is PartiqlAst.Expr.And -> redactLogicalOp(node.operands)
            is PartiqlAst.Expr.Or -> redactLogicalOp(node.operands)
            is PartiqlAst.Expr.Not -> redactExpr(node.expr)
            // Comparison Ops
            is PartiqlAst.Expr.Eq -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.Ne -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.Gt -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.Gte -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.Lt -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.Lte -> redactComparisonOp(node.operands)
            is PartiqlAst.Expr.InCollection -> redactComparisonOp(node.operands)
            // Arithmetic Ops
            is PartiqlAst.Expr.Plus -> plusMinusRedaction(node.operands)
            is PartiqlAst.Expr.Minus -> plusMinusRedaction(node.operands)
            is PartiqlAst.Expr.Times -> arithmeticOpRedaction(node.operands)
            is PartiqlAst.Expr.Divide -> arithmeticOpRedaction(node.operands)
            is PartiqlAst.Expr.Modulo -> arithmeticOpRedaction(node.operands)
            is PartiqlAst.Expr.Concat -> arithmeticOpRedaction(node.operands)
            // BETWEEN
            is PartiqlAst.Expr.Between -> {
                if (!skipRedaction(node.value, safeFieldNames)) {
                    redactExpr(node.from)
                    redactExpr(node.to)
                }
            }
            // CALL
            is PartiqlAst.Expr.Call -> redactCall(node)
            else -> { /* intentionally blank */ }
        }
    }

    private fun redactLiteral(literal: PartiqlAst.Expr.Lit) {
        val sourceLocation = literal.metas.sourceLocation ?: error("Cannot redact due to missing source location")
        sourceLocationMetaForRedaction.add(sourceLocation)
    }

    // once `bag`, `list`, and `sexp` modeled as described here: https://github.com/partiql/partiql-lang-kotlin/issues/239,
    // delete duplicated code
    private fun redactSeq(seq: PartiqlAst.Expr.List) = seq.values.map { redactExpr(it) }
    private fun redactSeq(seq: PartiqlAst.Expr.Bag) = seq.values.map { redactExpr(it) }
    private fun redactSeq(seq: PartiqlAst.Expr.Sexp) = seq.values.map { redactExpr(it) }

    private fun redactStruct(struct: PartiqlAst.Expr.Struct) {
        struct.fields.map {
            if (it.first is PartiqlAst.Expr.Lit) {
                redactLiteral(it.first)
            }
            redactExpr(it.second)
        }
    }

    private fun redactCall(node: PartiqlAst.Expr.Call) {
        val funcName = node.funcName.text
        when (val redactionLambda = userDefinedFunctionRedactionConfig[funcName]) {
            null -> node.args.map { redactExpr(it) }
            else -> {
                redactionLambda(node.args).map { redactExpr(it) }
            }
        }
    }

    private fun redactTypes(typed: PartiqlAst.Expr.IsType) {
        if (typed.value is PartiqlAst.Expr.Id && !skipRedaction(typed.value, safeFieldNames)) {
            val sourceLocation = typed.type.metas.sourceLocation ?: error("Cannot redact due to missing source location")
            sourceLocationMetaForRedaction.add(sourceLocation)
        }
    }

    /**
     * For [PartiqlAst.DmlOp.InsertValue], only the outermost level of struct files could have a key attribute.
     * For example, in the struct { 'hk': 'a', 'rk': 1, 'attr': { 'hk': 'a' }},
     * only 'hk' in 'attr': { 'hk': 'a' } will be redacted
     */
    private fun redactStructInInsertValueOp(struct: PartiqlAst.Expr.Struct) {
        struct.fields.map {
            when (it.first) {
                is PartiqlAst.Expr.Lit ->
                    if (!skipRedaction(it.first, safeFieldNames)) {
                        redactExpr(it.second)
                    }
                else { /* intentionally blank */ }
            }
        }
    }

    // once NAry node modeled better in PIG (https://github.com/partiql/partiql-lang-kotlin/issues/241), this code can be
    // refactored
    // TODO: other NAry ops that not modeled (LIKE, INTERSECT, INTERSECT_ALL, EXCEPT, EXCEPT_ALL, UNION, UNION_ALL)
    private fun PartiqlAst.Expr.isNAry(): Boolean {
        return this is PartiqlAst.Expr.And
            || this is PartiqlAst.Expr.Or
            || this is PartiqlAst.Expr.Not
            || this is PartiqlAst.Expr.Eq
            || this is PartiqlAst.Expr.Ne
            || this is PartiqlAst.Expr.Gt
            || this is PartiqlAst.Expr.Gte
            || this is PartiqlAst.Expr.Lt
            || this is PartiqlAst.Expr.Lte
            || this is PartiqlAst.Expr.InCollection
            || this is PartiqlAst.Expr.Plus
            || this is PartiqlAst.Expr.Minus
            || this is PartiqlAst.Expr.Times
            || this is PartiqlAst.Expr.Divide
            || this is PartiqlAst.Expr.Modulo
            || this is PartiqlAst.Expr.Concat
            || this is PartiqlAst.Expr.Between
            || this is PartiqlAst.Expr.Call

    }
}
