package org.partiql.isl.internal

import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Test
import org.partiql.isl.builder.IonSchema
import org.partiql.isl.extensions.IonTypes

class IonSchemaWriterTest {

    @Test
    internal fun basic() {
        val schema = IonSchema.build {
            schema {
                definitions += definition {
                    name = "Person"
                    constraints += constraintType(IonTypes.struct)
                    constraints += constraintFields {
                        fields = mutableMapOf(
                            "title" to typeInline {
                                constraints += constraintType(IonTypes.symbol)
                                constraints += constraintValidValues {
                                    values = mutableListOf(
                                        valueIon(ionSymbol("Mr")),
                                        valueIon(ionSymbol("Mrs")),
                                        valueIon(ionSymbol("Miss")),
                                        valueIon(ionSymbol("Ms")),
                                        valueIon(ionSymbol("Mx")),
                                        valueIon(ionSymbol("Dr")),
                                    )
                                }
                            },
                            "firstName" to typeInline {
                                occurs = occursRequired()
                                constraints += constraintType(IonTypes.string)
                            },
                            "middleName" to typeRef("string"),
                            "lastName" to typeInline {
                                occurs = occursRequired()
                                constraints += constraintType(IonTypes.string)
                            },
                            "age" to typeInline {
                                constraints += constraintType(IonTypes.int)
                                constraints += constraintValidValues {
                                    values += valueRange(rangeInt(0, 130))
                                }
                            }
                        )
                    }
                }
            }
        }
        val values = IonSchemaWriter.toIon(schema)
        values.forEach { println(it) }
    }
}
