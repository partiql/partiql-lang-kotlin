package org.partiql.ktlint.rule

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TopLevelInternalRuleTest {
    @Test
    fun `top-level internal`() {
        val code =
            """
            internal fun internalTopLevelFun() {}   // ktlint error
            
            internal val internalTopLevelVal = 123  // ktlint error
            
            // No errors for below (for this rule)
            public fun publicTopLevelFun() {}
            
            public val publicTopLevelVal = 123
            
            public class PublicClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        Assertions.assertThat(TopLevelInternalRule().lint(code)).containsExactly(
            LintError(1, 14, "top-level-internal", "Top-level internal declaration found: internalTopLevelFun"),
            LintError(3, 14, "top-level-internal", "Top-level internal declaration found: internalTopLevelVal")
        )
    }
}
