package examples

import org.partiql.lang.errors.ErrorCode
import kotlin.test.Test

class BreakingChanges {
    @Test
    fun `api change - renaming of field SEMANTIC_INFERENCER_ERROR`() {
        fun printOnSemanticProblem(someErrorCode: ErrorCode) {
            when (someErrorCode) {
                ErrorCode.SEMANTIC_PROBLEM -> println("Semantic inferencer problem occurred")
                else -> println("Some other error occurred")
            }
        }
    }
}
