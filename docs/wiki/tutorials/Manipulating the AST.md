# Description

The purpose of this document is to give readers more information on how to analyze/modify/traverse the
abstract syntax tree (AST) returned by PartiQL's parser.

The PartiQL Lang Kotlin source code uses the [PartiQL IR Generator (PIG)](https://github.com/partiql/partiql-ir-generator)
to generate the AST data structure and associated Visitors and Visitor Transforms. The Visitor allows you to traverse the
tree, while the Visitor Transforms allow you to modify the tree as it is traversed.

# Example Use-Cases

Some readers may ask what use-cases may require traversing the AST, and this section proposes the following scenarios:

## Checking for Data Manipulation Language (DML)

Suppose your application does not support DML in user-specified queries, and you have a Command Line Interface (CLI)
that supports syntax highlighting. Your use-case might be to highlight any areas of the query that contain DML
statements with red -- perhaps to show your users where their query is wrong.

This is a perfect example of how a PartiqlAst.Visitor can be leveraged to traverse the AST, check for DML statements,
and throw exceptions with location information of the found statement.

```kotlin
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.sourceLocationMeta
import org.partiql.lang.syntax.PartiQLParserBuilder

fun main() {
    val parser = PartiQLParserBuilder.standard().build()
    val query = "INSERT INTO customers VALUE { 'id': 1, 'name': 'John' }"
    val ast = parser.parseAstStatement(query)

    try {
        DmlEnforcer.walkStatement(ast)
    } catch (ex: Throwable) {
        when (ex) {
            is DmlEnforcer.DmlException -> highlightErrorLocation(ex.line, ex.char, ex.length)
            is RuntimeException -> highlightErrorQuery()
            else -> highlightErrorQuery()
        }
    }
    highlightSuccessQuery()
}

object DmlEnforcer : PartiqlAst.Visitor() {
    override fun visitStatementDml(node: PartiqlAst.Statement.Dml) = throwDmlException(node)

    override fun walkStatementDml(node: PartiqlAst.Statement.Dml) = throwDmlException(node)

    private fun throwDmlException(node: PartiqlAst.Statement.Dml): Nothing = when (val source = node.metas[SourceLocationMeta.TAG] as? SourceLocationMeta) {
        null -> throw RuntimeException("DML not allowed.")
        else -> throw DmlException(source.lineNum, source.charOffset, source.length)
    }

    class DmlException(val line: Long, val char: Long, val length: Long) : RuntimeException(
        "DML not allowed. DML found at line $line, character $char, and length $length."
    )
}

private fun highlightErrorLocation(line: Long, char: Long, length: Long) = TODO("Whatever your application desires.")
private fun highlightErrorQuery() = TODO("Whatever your application desires.")
private fun highlightSuccessQuery() = TODO("Whatever your application desires.")
```

## Specifying Operand Types

In a similar scenario, your use-case may be to only allow table references on the right-hand side (RHS) of the IN
predicate. Or, perhaps, your use-case is to specifically exclude literal lists as the RHS operand. Either way, we can
use the PartiqlAst.Visitor abstract class to accomplish this.

```kotlin
import org.partiql.lang.domains.PartiqlAst

fun main() {
    val parser = PartiQLParserBuilder.standard().build()
    val query = "(1 IN some_table) AND (1 IN [0, 1, 2])"
    val ast = parser.parseAstStatement(query)

    InPredicateTableReferenceChecker.walkStatement(ast)
    InPredicateListChecker.walkStatement(ast)
}

object InPredicateTableReferenceChecker : PartiqlAst.Visitor() {
    override fun visitExprInCollection(node: PartiqlAst.Expr.InCollection) {
        val rhs = node.operands[1]
        if (rhs !is PartiqlAst.Expr.Id || isTableReference(rhs.name).not()) {
            val errorMessage = """
                Only table references (identifiers) are allowed on the RHS of an IN predicate.
            """
            throw RuntimeException(errorMessage)
        }
    }
}

object InPredicateListChecker : PartiqlAst.Visitor() {
    override fun visitExprInCollection(node: PartiqlAst.Expr.InCollection) {
        val rhs = node.operands[1]
        if (rhs is PartiqlAst.Expr.List) {
            val errorMessage = """
                Literal lists are not allowed on the RHS of an IN predicate.
                Allowed Usage Examples:
                    - 1 IN some_table -- example of literal on LHS
                    - a IN some_table -- example of variable on LHS
                NOT Allowed Usage Examples:
                    - 1 IN [0, 1, 2] -- example of literal on LHS
                    - a IN (0, 1, 2) -- example of variable on LHS
            """
            throw RuntimeException(errorMessage)
        }
    }
}
```

## Replacing Function Calls

Perhaps, your use-case is to provide users with "auto-correction" of builtin functions. Let's say it's quite common for
your users to misspell some builtin function name, and, therefore, you want to avoid the situation of prompting the user
to fix their mistake.

```kotlin
import org.partiql.lang.domains.PartiqlAst

fun main() {
    val parser = PartiQLParserBuilder.standard().build()
    val query = "difficult_spell_name(1, 2)"
    val ast = parser.parseAstStatement(query)

    val fixedAst = FunctionSpellCheckFixer.transformStatement(ast)
}

object FunctionSpellCheckFixer : PartiqlAst.VisitorTransform() {

    private const val actualFunctionName = "difficult_to_spell_function_name"

    override fun transformExprCall(node: PartiqlAst.Expr.Call): PartiqlAst.Expr {
        val transformedNode = super.transformExprCall(node) as PartiqlAst.Expr.Call
        return when (isSlightlyMisspelled(node.funcName)) {
            true -> transformedNode.copy(funcName = actualFunctionName)
            false -> transformedNode
        }
    }

    private fun isSlightlyMisspelled(name: String): Boolean = TODO("However you'd like to implement")
}
```

# Using ANTLR

By exposing our AST as a public API, users have the ability to analyze and modify the tree for their own use-cases.
That being said, there are a few unique use-cases that may lie outside the scope of the AST's Visitor and VisitorTransform
methods, such as highlighting queries or tokenizing.

For these use-cases, it may be necessary to use some exposed internals of PartiQL. Internally, the
`partiql-lang-kotlin` package uses [ANTLR4](https://github.com/antlr/antlr4) to generate a parser (see the PartiQLParser). While these APIs aren't
supported by the PartiQL team, they may suit your needs for more specific use-cases.

Take, for example, the PartiQL CLI. The PartiQL CLI, to provide support for Syntax Highlighting, uses the generated
parser vended in `partiql-lang-kotlin`. The entire implementation can be found in the `:cli` subproject, but here's a
snippet of how it works:

```kotlin
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jline.utils.AttributedStringBuilder
import org.partiql.parser.antlr.PartiQLParser
import org.partiql.parser.antlr.PartiQLTokens

fun highlight() {
    // Build Token Colors (Last Token is EOF)
    val tokenIter = getTokenStream(usableInput).also { it.fill() }.tokens.iterator()
    val builder = AttributedStringBuilder()
    while (tokenIter.hasNext()) {
        val token = tokenIter.next()
        val (type, text) = token.type to token.text
        when {
            isUnrecognized(type) -> builder.styled(STYLE_ERROR, text)
            isIdentifier(type) -> builder.styled(STYLE_IDENTIFIER, text)
            isKeyword(type, text) -> builder.styled(STYLE_KEYWORD, text)
            isEOF(type) -> builder.styled(STYLE_ERROR, text.removeSuffix("<EOF>"))
            else -> builder.append(text)
        }
    }
    
    // Highlight Malformed Queries with RED
    val parser = PartiQLParser(getTokenStream(usableInput))
    try {
        parser.root()
    } catch (e: RethrowErrorListener.OffendingSymbolException) {
        TODO("Highlight the error location in RED. See the codebase for details.")
    }
}

private fun getTokenStream(input: String): CommonTokenStream {
    val inputStream = CharStreams.fromStream(input.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
    val tokenizer = PartiQLTokens(inputStream)
    tokenizer.removeErrorListeners()
    return CommonTokenStream(tokenizer)
}
```











