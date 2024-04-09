package org.partiql.ktlint.rule

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TopLevelPublicRuleTest {
    @Test
    fun `top-level public`() {
        val code =
            """
            public fun publicTopLevelFun() {}   // ktlint error
            
            public val publicTopLevelVal = 123  // ktlint error
            
            public var publicTopLevelVar = 456  // ktlint error
            
            fun publicTopLevelFun2() {} // ktlint error
            
            val publicTopLevelVal = 123 // ktlint error
            
            var publicTopLevelVar = 456 // ktlint error
            
            // No errors for below (for this rule)
            internal fun internalTopLevelFun() {}
            
            internal val internalTopLevelVal = 123
            
            internal var publicTopLevelVar = 456
            
            public class PublicClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        Assertions.assertThat(TopLevelPublicRule().lint(code)).containsExactly(
            LintError(1, 12, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun"),
            LintError(3, 12, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelVal"),
            LintError(5, 12, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelVar"),
            LintError(7, 5, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun2"),
            LintError(9, 5, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelVal"),
            LintError(11, 5, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelVar")
        )
    }

    @Test
    fun `top-level public with file name`() {
        val code =
            """
            @file:JvmName("SomeName")
            public fun publicTopLevelFun() {}
            
            public val publicTopLevelVal = 123
            
            public var publicTopLevelVar = 456
            
            fun publicTopLevelFun2() {}
            
            val publicTopLevelVal = 123
            
            var publicTopLevelVar = 456
            
            // No errors for below (for this rule)
            internal fun internalTopLevelFun() {}
            
            internal val internalTopLevelVal = 123
            
            internal var publicTopLevelVar = 456
            
            public class PublicClass {
                internal fun internalFun() {}
            
                internal val internalVal = 123
                
                public fun publicFun() {}
                
                public val publicVal = 123
            }
            """.trimIndent()
        Assertions.assertThat(TopLevelPublicRule().lint(code)).containsExactly()
    }

    @Test
    fun `different modifier levels`() {
        val code =
            """
            public fun publicTopLevelFun() {}   // ktlint error

            fun publicTopLevelFun2() {}         // ktlint error
            
            internal fun publicTopLevelFun() {}
            
            protected fun publicTopLevelFun() {}
            
            private fun publicTopLevelFun() {}
            """.trimIndent()
        Assertions.assertThat(TopLevelPublicRule().lint(code)).containsExactly(
            LintError(1, 12, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun"),
            LintError(3, 5, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun2"),
        )
    }

    @Test
    fun `different top level annotation`() {
        val code =
            """
            @file:OptIn(PartiQLValueExperimental::class)
            public fun publicTopLevelFun() {}   // ktlint error

            fun publicTopLevelFun2() {}         // ktlint error
            
            internal fun publicTopLevelFun() {}
            
            protected fun publicTopLevelFun() {}
            
            private fun publicTopLevelFun() {}
            """.trimIndent()
        Assertions.assertThat(TopLevelPublicRule().lint(code)).containsExactly(
            LintError(2, 12, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun"),
            LintError(4, 5, "top-level-public", "Top-level public declaration found without `@file:JvmName` annotation: publicTopLevelFun2"),
        )
    }
}
