package org.partiql.lang.domains

import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.api.Test
import java.math.BigInteger

// TODO: Delete this whole test directory
class AstTest {
    @Test
    fun test() {
        val item = PartiqlAst.build {
            lit(ionInt(1))
        }
        assert(item.value.bigIntegerValue == BigInteger.ONE)
    }
}
