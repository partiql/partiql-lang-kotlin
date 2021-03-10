package org.partiql.lang.ast.passes

import com.amazon.ion.IonString
import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.ast.*
import org.partiql.lang.ast.NAryOp.*
import org.partiql.lang.syntax.SqlParser

/**
 * This is a lambda function typed alias for caller side udf validation implementation
 *
 * There are two major components needed for implementation:
 *     1. Which arguments are needed for [SafeFieldName] validation
 *     2. Which arguments are needed for redaction should be returned
 *
 * For example, for a given function which argument number is static, func(a, b, c, d)
 * We can validate whether arg[0] and arg[1] are [SafeFieldName], if yes, arg[2] and arg[3] will be redacted
 */
typealias UserDefinedFunctionRedactionLambda = (List<ExprNode>) -> List<ExprNode>

private var safeFieldNames = emptySet<String>()

private val parser = SqlParser(IonSystemBuilder.standard().build())

const val INVALID_NUM_ARGS = "Invalid number of args in node"
const val INPUT_AST_STATEMENT_MISMATCH = "Input ast should be the parsed result of the input statement"

/**
 * Return true if the input node has value in [safeFieldNames] or skip the redaction intentionally
 */
fun validateSafeFieldNames(node: ExprNode): Boolean {
    if (safeFieldNames.isEmpty()) {
        return false
    }

    return when (node) {
        is NAry -> true // Skip redaction for NAry node
        is VariableReference -> safeFieldNames.contains(node.id)
        is Literal -> {
            if (node.ionValue is IonString) {
                val name = node.ionValue.stringValue()
                safeFieldNames.contains(name)
            } else { false }
        }
        is Path -> {
            var hasKeyAttribute = false
            node.components.map {
                when (it) {
                    is PathComponentExpr -> {
                        if (it.expr is Literal && it.expr.ionValue is IonString) {
                            val name = it.expr.ionValue.stringValue()
                            hasKeyAttribute = hasKeyAttribute || safeFieldNames.contains(name)
                        }
                    }
                    else -> { /* intentionally blank */ }
                }
            }
            hasKeyAttribute
        }
        else -> {
            throw IllegalArgumentException("Unexpected ExprNode type in StatementRedactor.validateSafeFieldNames: ${node.javaClass}")
        }
    }
}

/**
 * Redact sensitive data [Literal] not assigned with [safeFieldNames] to "***(Redacted)" based on input [statement]
 * and ast from default parser
 *
 * [safeFieldNames] is optional for fields not redacted
 *     For example: `SELECT * FROM tb WHERE hashkey = 'a' AND attr = 16453643`
 *     gets rewritten as `SELECT * FROM tb WHERE hashkey = 'a' AND attr = ***(Redacted)`
 *     because hashkey is in [safeFieldNames]
 * [userDefinedFunctionRedactionConfig] is optional for UDF function redaction, please check StatementRedactorTest.kt for more details
 *
 * Return redacted statement
 */
fun redact(statement: String,
           providedSafeFieldNames: Set<String> = emptySet(),
           userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda> = emptyMap()): String {
    return redact(statement, parser.parseExprNode(statement), providedSafeFieldNames, userDefinedFunctionRedactionConfig)
}

/**
 * Redact sensitive data [Literal] not assigned with [safeFieldNames] to "***(Redacted)" based on input [ast]
 * and [ast] parsed from the same input [statement]
 *
 * [safeFieldNames] is optional for fields not redacted
 *     For example: `SELECT * FROM tb WHERE hashkey = 'a' AND attr = 16453643`
 *     gets rewritten as `SELECT * FROM tb WHERE hashkey = 'a' AND attr = ***(Redacted)`
 *     because hashkey is in [safeFieldNames]
 * [userDefinedFunctionRedactionConfig] is optional for UDF function redaction, please check StatementRedactorTest.kt for more details
 *
 * Return correct redacted statement only if the parsed results of input [statement] is the same as input [ast]
 */
fun redact(statement: String,
           ast: ExprNode,
           providedSafeFieldNames: Set<String> = emptySet(),
           userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda> = emptyMap()): String {
    safeFieldNames = providedSafeFieldNames

    val statementRedactionRewriter = StatementRedactionRewriter(statement, ast, userDefinedFunctionRedactionConfig)
    statementRedactionRewriter.rewriteExprNode()
    return statementRedactionRewriter.getRedactedStatement()
}

/**
 * Redact sensitive data [Literal] not assigned with [safeFieldNames] to "***(Redacted)"
 *
 * TODO: when migrating to PIG, please use PartiqlAst.Visitor and not PartiqlAst.VisitorTransform!"
 */
private class StatementRedactionRewriter(
        private val statement: String,
        node: ExprNode,
        private val userDefinedFunctionRedactionConfig: Map<String, UserDefinedFunctionRedactionLambda>
) : AstRewriterBase() {
    private val root = node
    private val sourceLocationMetaForRedaction = arrayListOf<SourceLocationMeta>()
    private val maskPattern = "***(Redacted)"

    fun rewriteExprNode(): ExprNode {
        return super.rewriteExprNode(root)
    }

    override fun rewriteSelectWhere(node: ExprNode): ExprNode {
        redactOnField(node)
        return super.rewriteSelectWhere(node)
    }

    override fun rewriteDataManipulationWhere(node: ExprNode): ExprNode {
        redactOnField(node)
        return super.rewriteDataManipulationWhere(node)
    }

    override fun rewriteAssignment(node: Assignment): Assignment {
        if (!validateSafeFieldNames(node.lvalue)) {
            redactOnField(node.rvalue)
        }
        return super.rewriteAssignment(node)
    }

    override fun rewriteDataManipulationOperationInsertValueOp(node: InsertValueOp): DataManipulationOperation {
        when (node.value) {
            is Struct -> redactOnStructInInsertValueOp(node.value)
            else -> redactOnField(node.value)
        }
        return super.rewriteDataManipulationOperationInsertValueOp(node)
    }

    private fun redactOnField(node: ExprNode) {
        when (node) {
            is NAry -> redactOnNAry(node)
            is Literal -> redactOnLiteral(node)
            is Seq -> redactOnSeq(node)
            is Struct -> redactOnStruct(node)
            is Typed -> redactOnTyped(node)
            else -> { /* intentionally blank */ }
        }
    }

    private fun redactOnNAry(node: NAry) {
        val rewrittenArgs = node.args

        when (node.op) {
            AND, OR -> {
                rewrittenArgs.forEach { redactOnField(it) }
            }
            EQ, NE, GT, GTE, LT, LTE, IN -> {
                if (rewrittenArgs.size != 2) {
                    throw IllegalArgumentException(INVALID_NUM_ARGS)
                }
                if (!validateSafeFieldNames(rewrittenArgs[0])) {
                    redactOnField(rewrittenArgs[1])
                }
            }
            ADD, SUB -> {
                when (rewrittenArgs.size) {
                    1 -> {
                        redactOnField(rewrittenArgs[0])
                    }
                    2 -> {
                        redactOnField(rewrittenArgs[0])
                        redactOnField(rewrittenArgs[1])
                    }
                    else -> {
                        throw IllegalArgumentException(INVALID_NUM_ARGS)
                    }
                }
            }
            MUL, DIV, MOD -> {
                if (rewrittenArgs.size != 2) {
                    throw IllegalArgumentException(INVALID_NUM_ARGS)
                }
                redactOnField(rewrittenArgs[0])
                redactOnField(rewrittenArgs[1])
            }
            BETWEEN -> {
                if (rewrittenArgs.size != 3) {
                    throw IllegalArgumentException(INVALID_NUM_ARGS)
                }
                if (!validateSafeFieldNames(rewrittenArgs[0])) {
                    redactOnField(rewrittenArgs[1])
                    redactOnField(rewrittenArgs[2])
                }
            }
            NOT -> {
                if (rewrittenArgs.size != 1) {
                    throw IllegalArgumentException(INVALID_NUM_ARGS)
                }
                val arg = rewrittenArgs[0]
                when (arg) {
                    is NAry -> redactOnNAry(arg)
                    is Typed -> redactOnTyped(arg)
                    else -> { /* intentionally blank */ }
                }
            }
            CALL -> {
                redactOnCall(rewrittenArgs)
            }
            else -> { /* intentionally blank */ }
        }
    }

    private fun redactOnLiteral(literal: Literal) {
        val sourceLocation = literal.metas.sourceLocation ?: throw NoSuchElementException("No SourceLocation meta data in Literal object $literal")
        sourceLocationMetaForRedaction.add(sourceLocation)
    }

    private fun redactOnSeq(seq: Seq) {
        seq.values.map {
            redactOnField(it)
        }
    }

    private fun redactOnStruct(struct: Struct) {
        struct.fields.map {
            if (it.name is Literal) {
                redactOnLiteral(it.name)
            }
            redactOnField(it.expr)
        }
    }

    private fun redactOnCall(args: List<ExprNode>) {
        if (args.isEmpty()) {
            return
        }
        if (args[0] !is VariableReference) {
            return
        }

        val funcName = (args[0] as VariableReference).id

        if (userDefinedFunctionRedactionConfig.isEmpty() || !userDefinedFunctionRedactionConfig.containsKey(funcName)) {
            args.drop(0).map { redactOnField(it) }
            return
        }

        /* Call is redacted based on function name */
        val getArgsToRedact = userDefinedFunctionRedactionConfig[funcName]
        val argsToRedact = getArgsToRedact?.invoke(args)
        argsToRedact?.map { redactOnField(it) }
    }

    private fun redactOnTyped(typed: Typed) {
        when(typed.op) {
            TypedOp.IS -> {
                if (typed.expr is VariableReference && !validateSafeFieldNames(typed.expr)) {
                    val sourceLocation = typed.type.metas.sourceLocation ?: throw NoSuchElementException("No SourceLocation meta data in DataType object $typed")
                    sourceLocationMetaForRedaction.add(sourceLocation)
                }
            }
            else -> { /* intentionally blank */ }
        }
    }

    /**
     * For InsertValueOp, only first level of struct files could have key attribute
     * For example, in struct { 'hk': 'a', 'rk': 1, 'attr': { 'hk': 'a' }}
     * 'hk' in 'attr': { 'hk': 'a' } will be redacted
     */
    private fun redactOnStructInInsertValueOp(struct: Struct) {
        struct.fields.map {
            if (it.name is Literal) {
                if (!validateSafeFieldNames(it.name)) {
                    redactOnField(it.expr)
                }
            } else { /* intentionally blank */ }
        }
    }

    /**
     * Redact the statement based on [redactedSourceLocationMeta]
     * Convert the lineNum in SourceLocationMeta to the actual index in the single line
     */
    fun getRedactedStatement(): String {
        val lines = statement.lines()
        val totalCharactersInPreviousLines = IntArray(lines.size)
        for (lineNum in 1 until lines.size) {
            totalCharactersInPreviousLines[lineNum] = totalCharactersInPreviousLines[lineNum - 1] + lines[lineNum - 1].length + 1
        }

        val sb = StringBuilder(statement)
        var offset = 0;
        sourceLocationMetaForRedaction.sortWith(compareBy<SourceLocationMeta> { it.lineNum }.thenBy { it.charOffset })

        sourceLocationMetaForRedaction.map {
            val length = it.length.toInt()
            val start = totalCharactersInPreviousLines[it.lineNum.toInt() - 1] + it.charOffset.toInt() - 1 + offset
            if (start >= sb.length || start > sb.length - length) {
                throw IllegalArgumentException(INPUT_AST_STATEMENT_MISMATCH)
            }
            sb.replace(start, start + length, maskPattern)
            offset = offset + maskPattern.length - length
        }

        return sb.toString()
    }
}
