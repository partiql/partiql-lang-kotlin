package org.partiql.lang.partiqlisl

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionschema.IonSchemaSystemBuilder
import org.junit.Assert
import org.junit.Test
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.DATE_ANNOTATION
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.TIME_ANNOTATION

class PartiQLISLSchemaTests {

    private val ION = IonSystemBuilder.standard().build()
    private val ISS = IonSchemaSystemBuilder.standard().addAuthority(getResourceAuthority(ION)).build()
    private val schema = loadPartiqlIsl(ISS)

    @Test
    fun missingTypeTest() {
        val missingType = schema.getType("missing") ?: error("couldn't find 'missing' type")
        Assert.assertTrue(missingType.isl.isReadOnly)
        Assert.assertNull(missingType.isl.container)

        val violations = missingType.validate(ION.singleValue("$MISSING_ANNOTATION::null"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun missingTypeTest_noAnnotation() {
        val missingType = schema.getType("missing") ?: error("couldn't find 'missing' type")
        Assert.assertTrue(missingType.isl.isReadOnly)
        Assert.assertNull(missingType.isl.container)

        val violations = missingType.validate(ION.singleValue("null"))
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("missing annotation(s): $MISSING_ANNOTATION", violations.violations[0].message)
    }

    @Test
    fun missingTypeTest_invalid_null() {
        val missingType = schema.getType("missing") ?: error("couldn't find 'missing' type")
        Assert.assertTrue(missingType.isl.isReadOnly)
        Assert.assertNull(missingType.isl.container)

        val violations = missingType.validate(ION.singleValue("$MISSING_ANNOTATION::null.int"))
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("expected type null, found int", violations.violations[0].message)
    }

    @Test
    fun testBagType() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")

        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("$BAG_ANNOTATION::[1,2,3]"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
        Assert.assertFalse(violations.iterator().hasNext())
    }

    @Test
    fun testBagType_duplicate_values() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("$BAG_ANNOTATION::[1,2,3,2]"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
        Assert.assertFalse(violations.iterator().hasNext())
    }

    @Test
    fun testBagType_nested_bags() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(
            ION.singleValue(
                """
            $BAG_ANNOTATION::[1,
               2, 
               $BAG_ANNOTATION::["a", "a", "b"],
               2]
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
        Assert.assertFalse(violations.iterator().hasNext())
    }

    @Test
    fun testBagType_with_bag_annotation() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("bag::[1,2,3]"))
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("missing annotation(s): $BAG_ANNOTATION", violations.violations[0].message)
    }

    @Test
    fun testBagType_missing_annotation() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("[1,2,3]"))
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("missing annotation(s): $BAG_ANNOTATION", violations.violations[0].message)
    }

    @Test
    fun testBagType_misc_bag() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("$BAG_ANNOTATION::[1,'two',\"three\"]"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun testBagType_empty_bag() {
        val bagType = schema.getType("bag") ?: error("couldn't find 'bag' type")
        Assert.assertTrue(bagType.isl.isReadOnly)
        Assert.assertNull(bagType.isl.container)

        val violations = bagType.validate(ION.singleValue("$BAG_ANNOTATION::[]"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun dateTypeTest() {
        val dateType = schema.getType("date") ?: error("couldn't find 'date' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        val violations = dateType.validate(ION.singleValue("$DATE_ANNOTATION::1992-02-29"))
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun dateTypeTest_missing_annotation() {
        val dateType = schema.getType("date") ?: error("couldn't find 'date' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        val violations = dateType.validate(ION.singleValue("1992-02-29"))
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("missing annotation(s): $DATE_ANNOTATION", violations.violations[0].message)
    }

    @Test
    fun dateTypeTest_invalid_date() {
        val dateType = schema.getType("date") ?: error("couldn't find 'date' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        val violations = dateType.validate(ION.singleValue("$DATE_ANNOTATION::2000-01-01T00:00Z"))
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals("expected type {timestamp_precision:day}", violations.violations[0].message)
    }

    @Test
    fun timeTypeTest() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        // time - 23:59:59.009999(HH:MM:SS.MMMMMM) is represented as ion struct
        val violations = dateType.validate(
            ION.singleValue(
                """
            $TIME_ANNOTATION::{
                hour: 23,
                min: 59,
                sec: 59,
                sec_fraction: 9999
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun timeTypeTest_missing_partiql_time_annotation() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        // time - 24:59:1(HH:MM:SS.MMMMMM)
        val violations = dateType.validate(
            ION.singleValue(
                """
            {
                hour: 23,
                min: 59,
                sec: 1
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals(
            """
            Validation failed:
            - missing annotation(s): $TIME_ANNOTATION
            """.trimIndent(),
            violations.toString().trimIndent()
        )
    }

    @Test
    fun timeTypeTest_optional_seconds_fraction() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        // time - 23:59:59(HH:MM:SS.MMMMMM) is represented as ion struct
        val violations = dateType.validate(
            ION.singleValue(
                """
            $TIME_ANNOTATION::{
                hour: 23,
                min: 59,
                sec: 59
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertTrue(violations.isValid())
    }

    @Test
    fun timeTypeTest_missing_seconds() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        // time - 23:59(HH:MM:SS.MMMMMM) is invalid as sec field is missing
        val violations = dateType.validate(
            ION.singleValue(
                """
            $TIME_ANNOTATION::{
                hour: 23,
                min: 59
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals(
            """
            Validation failed:
            - one or more fields don't match expectations
              - sec
                - expected range::[1,1] occurrences, found 0
            """.trimIndent(),
            violations.toString().trimIndent()
        )
    }

    @Test
    fun timeTypeTest_invalid_hour() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        // time - 24:59:1(HH:MM:SS.MMMMMM)
        val violations = dateType.validate(
            ION.singleValue(
                """
            $TIME_ANNOTATION::{
                hour: 24,
                min: 59,
                sec: 1
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals(
            """
            Validation failed:
            - one or more fields don't match expectations
              - hour: 24
                - invalid value 24
            """.trimIndent(),
            violations.toString().trimIndent()
        )
    }

    @Test
    fun timeTypeTest_content_closed() {
        val dateType = schema.getType("time") ?: error("couldn't find 'time' type")
        Assert.assertTrue(dateType.isl.isReadOnly)
        Assert.assertNull(dateType.isl.container)

        val violations = dateType.validate(
            ION.singleValue(
                """
            $TIME_ANNOTATION::{
                hour: 23,
                min: 59,
                sec: 1,
                sec_fraction: 999999,
                foo: 12
            }
                """.trimIndent()
            )
        )
        Assert.assertNotNull(violations)
        Assert.assertFalse(violations.isValid())
        Assert.assertTrue(violations.violations.size > 0)
        Assert.assertEquals(
            """
            Validation failed:
            - found one or more unexpected fields
              - foo: 12
            """.trimIndent(),
            violations.toString().trimIndent()
        )
    }
}
