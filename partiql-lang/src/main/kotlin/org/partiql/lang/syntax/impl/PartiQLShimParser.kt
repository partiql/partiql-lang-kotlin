package org.partiql.lang.syntax.impl

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.MetaContainer
import org.partiql.ast.helpers.toLegacyAst
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.ParserException
import org.partiql.parser.PartiQLLexerException
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.SourceLocations

/**
 * Implementation of [Parser] which uses a [org.partiql.ast.AstNode] tree, then translates to the legacy interface.
 *
 * @property delegate
 */
internal class PartiQLShimParser(
    private val delegate: PartiQLParser,
) : Parser {

    // required for PropertyValueMap debug information
    private val ion = IonSystemBuilder.standard().build()

    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        val result = try {
            delegate.parse(source)
        } catch (ex: PartiQLLexerException) {
            throw ex.shim()
        } catch (ex: PartiQLParserException) {
            throw ex.shim()
        }
        val statement = try {
            val metas = result.locations.toMetas()
            result.root.toLegacyAst(metas)
        } catch (ex: Exception) {
            throw ParserException(
                message = ex.message ?: "",
                errorCode = ErrorCode.PARSE_INVALID_QUERY,
                cause = ex,
            )
        }
        if (statement !is PartiqlAst.Statement) {
            throw ParserException(
                message = "Expected statement, got ${statement::class.qualifiedName}",
                errorCode = ErrorCode.PARSE_INVALID_QUERY,
            )
        }
        return statement
    }

    /**
     * The legacy parser tests assert on ParserExcept, not LexerException.
     */
    private fun PartiQLLexerException.shim(): ParserException {
        val ctx = PropertyValueMap()
        ctx[Property.LINE_NUMBER] = location.line.toLong()
        ctx[Property.COLUMN_NUMBER] = location.offset.toLong()
        ctx[Property.TOKEN_STRING] = token
        ctx[Property.TOKEN_DESCRIPTION] = tokenType
        ctx[Property.TOKEN_VALUE] = ion.newSymbol(token)
        return ParserException(message, ErrorCode.PARSE_UNEXPECTED_TOKEN, ctx, cause)
    }

    private fun PartiQLParserException.shim(): ParserException {
        val ctx = PropertyValueMap()
        ctx[Property.LINE_NUMBER] = location.line.toLong()
        ctx[Property.COLUMN_NUMBER] = location.offset.toLong()
        ctx[Property.TOKEN_DESCRIPTION] = tokenType
        ctx[Property.TOKEN_VALUE] = ion.newSymbol(token)
        return ParserException(message, ErrorCode.PARSE_UNEXPECTED_TOKEN, ctx, cause)
    }

    private fun SourceLocations.toMetas(): Map<String, MetaContainer> = mapValues {
        metaContainerOf(
            SourceLocationMeta(
                lineNum = it.value.line.toLong(),
                charOffset = it.value.offset.toLong(),
                length = it.value.lengthLegacy.toLong(),
            )
        )
    }
}
