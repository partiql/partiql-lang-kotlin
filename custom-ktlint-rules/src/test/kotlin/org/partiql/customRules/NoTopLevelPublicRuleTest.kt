package org.partiql.customRules

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NoTopLevelPublicRuleTest {
    @Test
    fun `No top level public`() {
        val code =
            """
            public fun publicTopLevelFun() {}   // ktlint error
            
            public val publicTopLevelVal = 123  // ktlint error
            
            // No errors for below (for this rule)
            internal fun internalTopLevelFun() {}
            
            internal val internalTopLevelVal = 123
            
            public class PublicClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        assertThat(NoTopLevelPublicRule().lint(code)).containsExactly(
            LintError(1, 12, "no-top-level-public", "Top level public declaration found: publicTopLevelFun"),
            LintError(3, 12, "no-top-level-public", "Top level public declaration found: publicTopLevelVal")
        )
    }

    @Test
    fun `No top level public with file name`() {
        val code =
            """
            @file:JvmName("SomeName")
            public fun publicTopLevelFun() {}   // no error
            
            public val publicTopLevelVal = 123  // no error
            
            // No errors for below (for this rule)
            internal fun internalTopLevelFun() {}
            
            internal val internalTopLevelVal = 123
            
            public class PublicClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        assertThat(NoTopLevelPublicRule().lint(code)).containsExactly()
    }
}
