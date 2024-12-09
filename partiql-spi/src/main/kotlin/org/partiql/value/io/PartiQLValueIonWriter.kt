package org.partiql.value.io

import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ion.system.IonWriterBuilder
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.toIon
import java.io.OutputStream

@OptIn(PartiQLValueExperimental::class)
public class PartiQLValueIonWriter internal constructor(
    private val ionWriter: IonWriter,
) : PartiQLValueWriter {

    override fun append(value: PartiQLValue): PartiQLValueWriter {
        value.toIon().writeTo(ionWriter)
        return this
    }

    override fun close() {
        ionWriter.close()
    }
}

@OptIn(PartiQLValueExperimental::class)
public class PartiQLValueIonWriterBuilder private constructor(
    private var ionWriterBuilder: IonWriterBuilder,
) {

    public companion object {
        @JvmStatic
        public fun standardIonTextBuilder(): PartiQLValueIonWriterBuilder = PartiQLValueIonWriterBuilder(
            ionWriterBuilder = IonTextWriterBuilder.standard()
        )

        @JvmStatic
        public fun standardIonBinaryBuilder(): PartiQLValueIonWriterBuilder = PartiQLValueIonWriterBuilder(
            ionWriterBuilder = IonBinaryWriterBuilder.standard()
        )
    }

    public fun build(output: OutputStream): PartiQLValueWriter =
        PartiQLValueIonWriter(
            ionWriter = ionWriterBuilder.build(output),
        )

    public fun ionWriterBuilder(ionWriterBuilder: IonWriterBuilder): PartiQLValueIonWriterBuilder = this.apply {
        this.ionWriterBuilder = ionWriterBuilder
    }
}
