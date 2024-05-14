package org.partiql.planner.internal.ddl

import org.junit.jupiter.api.Test
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.typeAtomicInt2
import org.partiql.planner.internal.ir.typeCollection
import org.partiql.planner.internal.ir.typeRecord
import org.partiql.planner.internal.ir.typeRecordField
import org.partiql.planner.internal.typer.ShapeNormalizer
import kotlin.test.assertEquals

internal class ShapeNormalizerTest {
    val normalizer = ShapeNormalizer()

    @Test
    fun test() {
        val shape = typeCollection(
            type = typeRecord(
                listOf(
                    typeRecordField(
                        identifierSymbol("a", caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE),
                        typeAtomicInt2(),
                        emptyList(),
                        false,
                        null
                    )
                ),
                emptyList()
            ),
            false,
            emptyList()
        )

        val expectedShape = typeCollection(
            type = typeRecord(
                listOf(
                    typeRecordField(
                        identifierSymbol("a", caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE),
                        typeAtomicInt2(),
                        emptyList(),
                        false,
                        null
                    )
                ),
                emptyList()
            ),
            false,
            emptyList()
        )

        assertEquals(expectedShape, normalizer.normalize(shape, "tbl"))
    }
}
