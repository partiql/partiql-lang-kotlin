package org.partiql.customRules

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class NoTopLevelInternalRuleTest {
    private val wrappingRuleAssertThat = assertThatRule { NoTopLevelInternalRule() }

    @Test
    fun `No top level internal`() {
        // whenever KTLINT_DEBUG env variable is set to "ast" or -DktlintDebug=ast is used
        // com.pinterest.ktlint.test.(lint|format) will print AST (along with other debug info) to the stderr.
        // this can be extremely helpful while writing and testing rules.
        // uncomment the line below to take a quick look at it
        // System.setProperty("ktlintDebug", "ast")
        val code =
            """
            internal fun internalTopLevelFun() {}   // ktlint error
            
            internal val internalTopLevelVal = 123  // ktlint error
            
            // No errors for below
            public fun publicTopLevelFun() {}
            
            public val publicTopLevelVal = 123
            
            public class InternalClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        wrappingRuleAssertThat(code)
            .hasLintViolationsWithoutAutoCorrect(
                LintViolation(1, 14, "Top level declaration found: internalTopLevelFun"),
                LintViolation(3, 14, "Top level declaration found: internalTopLevelVal")
            )
    }
}
