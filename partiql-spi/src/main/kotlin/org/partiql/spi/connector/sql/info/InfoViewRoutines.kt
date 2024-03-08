package org.partiql.spi.connector.sql.info

import org.partiql.shape.PShape
import org.partiql.shape.constraints.Fields
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.Index
import org.partiql.value.AnyType
import org.partiql.value.BagType
import org.partiql.value.CharVarUnboundedType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.nullValue

/**
 * This provides the INFORMATION_SCHEMA.ROUTINES view for an [SqlConnector].
 */
internal class InfoViewRoutines @OptIn(FnExperimental::class) constructor(private val index: Index<Fn>) : InfoView {

    override val schema = PShape.of(
        type = BagType(AnyType),
        constraint = Fields(
            fields = listOf(
                Fields.Field("ROUTINE_NAME", CharVarUnboundedType),
                Fields.Field("ROUTINE_TYPE", CharVarUnboundedType)
            ),
            isClosed = true
        )
    )

    @OptIn(PartiQLValueExperimental::class)
    override fun value(): PartiQLValue {
        return bagValue(listOf(nullValue()))
    }
}
