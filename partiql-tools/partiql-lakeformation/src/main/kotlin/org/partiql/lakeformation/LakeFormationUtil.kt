package org.partiql.lakeformation

import org.partiql.lakeformation.datafilter.LakeFormationDataFilter
import org.partiql.lang.syntax.Parser

object LakeFormationUtil {
    fun extractDataCell(query: String, parser: Parser): LakeFormationDataFilter {
        val ast = parser.parseAstStatement(query)
        val lakeFormationDataFilter = DataFilterExtractor.extractDataFilter(ast)
        if (lakeFormationDataFilter.rowFilter != null) {
            RowFilterValidator(parser).validate(lakeFormationDataFilter.rowFilter)
        }
        return lakeFormationDataFilter
    }
}
