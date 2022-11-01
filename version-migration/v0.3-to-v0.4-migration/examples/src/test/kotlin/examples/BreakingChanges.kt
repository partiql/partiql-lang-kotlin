package examples

import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionInt
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlAst.Builder.lit
import kotlin.test.Test

class BreakingChanges {
    @Test
    fun `api change - PIG upgrade version disallowing imported builders`() {
        // PartiQL v0.3.1 and older used partiql-ir-generator (PIG) v0.3.0 and older, which allowed for specifying PIG-
        // generated objects using the <TypeDomain>.Builder object and importing the builder functions. These were
        // unintended exposed APIs and bypassed the recommended way to create domain objects.
        //
        // The following uses the imported builder function to create the literal, 1
        lit(value = ionInt(1), emptyMetaContainer())

        // Newer versions made this builder an interface with a private implementation. The recommended way to create
        // the objects is to use `<TypeDomain>.build { ... }`:
        PartiqlAst.build {
            lit(value = ionInt(1), emptyMetaContainer())
        }
    }
}
