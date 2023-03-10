package examples

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import org.partiql.lang.domains.PartiqlAst
import kotlin.test.Test

class BreakingChanges {
    @Test
    fun `api change - PIG upgrade version disallowing imported builders`() {
        // Newer versions made this builder an interface with a private implementation, so users can no longer import
        // the TypeDomain's builders. The recommended way to create the objects is to use `<TypeDomain>.build { ... }`
        // pattern:
        PartiqlAst.build {
            lit(value = ionInt(1), emptyMetaContainer())
        }
    }
}
