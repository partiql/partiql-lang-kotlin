package org.partiql.lakeformation

import org.partiql.lakeformation.datafilter.LakeFormationDataFilter
import org.partiql.lang.syntax.Parser

/**
 * For now, this should be the only publicly available API.
 *
 * Example usage:
 * ```
 * ...
 * val parser = PartiQLParserBuilder.standard().build()
 * val query = "..."
 * val lakeFormationDataFilter = LakeFormationUtil.extractDataCell(query, parser)
 * ```
 * See [LakeFormationDataFilter] for the data filter constructed.
 *
 * Notice it is required for the end user to define how to map the result to the LakeFormation API call.
 */
object LakeFormationUtil {

    fun extractDataCell(query: String, parser: Parser): LakeFormationDataFilter {
        val ast = parser.parseAstStatement(query)
        val lakeFormationDataFilter = DataFilterExtractor.extractDataFilter(ast)
        if (lakeFormationDataFilter.rowFilter != null) {
            // This takes in a parser to avoid multiple construction of the partiql Parser.
            RowFilterValidator(parser).validate(lakeFormationDataFilter.rowFilter)
        }
        return lakeFormationDataFilter
    }
}
