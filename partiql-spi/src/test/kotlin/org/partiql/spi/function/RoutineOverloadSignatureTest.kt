/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import java.util.LinkedList

class RoutineOverloadSignatureTest {

    @Test
    fun copiesAndProtectsParameterTypes() {
        val parameterTypes = mutableListOf(PType.string())
        val signature = RoutineOverloadSignature("tokenize", parameterTypes)

        parameterTypes += PType.integer()

        assertEquals(listOf(PType.string()), signature.parameterTypes)
        assertThrows<UnsupportedOperationException> {
            (signature.parameterTypes as MutableList<PType>).add(PType.dynamic())
        }
    }

    @Test
    fun deeplySnapshotsRowsAndMetadata() {
        val fieldType = PType.integer()
        fieldType.metas["nested"] = "before"
        val fields = mutableListOf(PTypeField.of("value", fieldType))
        val row = PType.row(fields)
        row.metas["row"] = "before"
        val signature = RoutineOverloadSignature("inspect", listOf(row))

        fields.clear()
        fieldType.metas["nested"] = "after"
        row.metas["row"] = "after"

        val firstRead = signature.parameterTypes.single()
        assertEquals("before", firstRead.metas["row"])
        assertEquals("before", firstRead.fields.single().type.metas["nested"])
        assertThrows<UnsupportedOperationException> {
            (firstRead.fields as MutableCollection<PTypeField>).clear()
        }

        firstRead.metas["row"] = "reader mutation"
        firstRead.fields.single().type.metas["nested"] = "reader mutation"
        val secondRead = signature.parameterTypes.single()

        assertNotSame(firstRead, secondRead)
        assertEquals("before", secondRead.metas["row"])
        assertEquals("before", secondRead.fields.single().type.metas["nested"])
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun deeplySnapshotsSupportedMutableMetadataValues() {
        val list = mutableListOf<Any>(mutableListOf("list"))
        val set = linkedSetOf("set")
        val map = linkedMapOf<String, Any>("nested" to mutableListOf("map"))
        val objectArray = arrayOf<Any>("array")
        val primitiveArray = intArrayOf(1)
        val arrayListArray = arrayOf(arrayListOf("array list"))
        val linkedListArray = arrayOf(LinkedList(listOf("linked list")))
        val type = PType.integer()
        type.metas["list"] = list
        type.metas["set"] = set
        type.metas["map"] = map
        type.metas["objectArray"] = objectArray
        type.metas["primitiveArray"] = primitiveArray
        type.metas["arrayListArray"] = arrayListArray
        type.metas["linkedListArray"] = linkedListArray
        val signature = RoutineOverloadSignature("inspect", listOf(type))

        (list.single() as MutableList<String>) += "source mutation"
        list += "source mutation"
        set += "source mutation"
        (map.getValue("nested") as MutableList<String>) += "source mutation"
        objectArray[0] = "source mutation"
        primitiveArray[0] = 2
        arrayListArray.single() += "source mutation"
        linkedListArray.single() += "source mutation"

        val firstMetadata = signature.parameterTypes.single().metas
        assertEquals(listOf(listOf("list")), firstMetadata["list"])
        assertEquals(setOf("set"), firstMetadata["set"])
        assertEquals(mapOf("nested" to listOf("map")), firstMetadata["map"])
        assertEquals("array", (firstMetadata["objectArray"] as Array<*>).single())
        assertEquals(1, (firstMetadata["primitiveArray"] as IntArray).single())
        val copiedArrayListArray = firstMetadata["arrayListArray"] as Array<*>
        assertEquals(ArrayList::class.java, copiedArrayListArray.javaClass.componentType)
        assertEquals(listOf("array list"), copiedArrayListArray.single())
        val copiedLinkedListArray = firstMetadata["linkedListArray"] as Array<*>
        assertEquals(Any::class.java, copiedLinkedListArray.javaClass.componentType)
        assertEquals(listOf("linked list"), copiedLinkedListArray.single())

        (firstMetadata["list"] as MutableList<Any>).add("reader mutation")
        (firstMetadata["objectArray"] as Array<Any>)[0] = "reader mutation"
        (firstMetadata["primitiveArray"] as IntArray)[0] = 3
        (copiedArrayListArray.single() as MutableList<String>) += "reader mutation"
        (copiedLinkedListArray.single() as MutableList<String>) += "reader mutation"
        val secondMetadata = signature.parameterTypes.single().metas

        assertEquals(listOf(listOf("list")), secondMetadata["list"])
        assertEquals("array", (secondMetadata["objectArray"] as Array<*>).single())
        assertEquals(1, (secondMetadata["primitiveArray"] as IntArray).single())
        assertEquals(
            listOf("array list"),
            (secondMetadata["arrayListArray"] as Array<*>).single(),
        )
        assertEquals(
            listOf("linked list"),
            (secondMetadata["linkedListArray"] as Array<*>).single(),
        )
    }

    @Test
    fun rejectsUnsupportedAndCyclicMetadataValues() {
        val unsupported = PType.integer()
        unsupported.metas["unsupported"] = StringBuilder("mutable")
        val unsupportedError = assertThrows<IllegalArgumentException> {
            RoutineOverloadSignature("inspect", listOf(unsupported))
        }
        assertEquals(
            "Unsupported routine parameter metadata value type: java.lang.StringBuilder",
            unsupportedError.message,
        )

        val cycle = mutableListOf<Any>()
        cycle.add(cycle)
        val cyclic = PType.integer()
        cyclic.metas["cycle"] = cycle
        val cycleError = assertThrows<IllegalArgumentException> {
            RoutineOverloadSignature("inspect", listOf(cyclic))
        }
        assertEquals("Cyclic routine parameter metadata is not supported.", cycleError.message)
    }

    @Suppress("DEPRECATION")
    @Test
    fun snapshotsEverySupportedParameterTypeShape() {
        val types = listOf(
            PType.dynamic(),
            PType.bool(),
            PType.tinyint(),
            PType.smallint(),
            PType.integer(),
            PType.bigint(),
            PType.numeric(10, 2),
            PType.decimal(10, 2),
            PType.real(),
            PType.doublePrecision(),
            PType.character(4),
            PType.varchar(8),
            PType.string(),
            PType.blob(16),
            PType.clob(16),
            PType.date(),
            PType.time(3),
            PType.timez(3),
            PType.timestamp(3),
            PType.timestampz(3),
            PType.array(PType.integer()),
            PType.bag(PType.string()),
            PType.row(PTypeField.of("value", PType.integer())),
            PType.struct(),
            PType.map(PType.string(), PType.integer()),
            PType.unknown(),
            PType.variant("custom"),
            PType.intervalYear(2),
            PType.intervalMonth(2),
            PType.intervalYearMonth(2),
            PType.intervalDay(2),
            PType.intervalHour(2),
            PType.intervalMinute(2),
            PType.intervalSecond(2, 3),
            PType.intervalDayHour(2),
            PType.intervalDayMinute(2),
            PType.intervalDaySecond(2, 3),
            PType.intervalHourMinute(2),
            PType.intervalHourSecond(2, 3),
            PType.intervalMinuteSecond(2, 3),
        )
        types.forEachIndexed { index, type -> type.metas["index"] = index }
        val signature = RoutineOverloadSignature("all_types", types)

        types.forEach { it.metas.clear() }
        val snapshot = signature.parameterTypes

        assertEquals(types, snapshot)
        snapshot.forEachIndexed { index, type ->
            assertNotSame(types[index], type)
            assertEquals(index, type.metas["index"])
        }
        assertFalse(snapshot.any { it.metas.isEmpty() })
    }
}
