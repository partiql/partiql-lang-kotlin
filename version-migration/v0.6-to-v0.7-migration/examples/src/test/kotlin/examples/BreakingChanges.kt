package examples

import org.partiql.lang.errors.ErrorCode
import kotlin.test.Test

class BreakingChanges {
    // Breaking change -- `ErrorCode.EVALUATOR_SQL_EXCEPTION` removal. No equivalent `ErrorCode` and was only used
    //                     in testing
    // Breaking change -- `NodeMetadata` removal. Only used in testing. Not part of any public APIs.

    @Test
    fun `api change - renaming of field SEMANTIC_INFERENCER_ERROR`() {
        fun printOnSemanticInferencerError(someErrorCode: ErrorCode) {
            when (someErrorCode) {
                ErrorCode.SEMANTIC_INFERENCER_ERROR -> println("Semantic inferencer error occurred")
                else -> println("Some other error occurred")
            }
        }
    }
}
