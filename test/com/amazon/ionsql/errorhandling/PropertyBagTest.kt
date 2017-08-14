package com.amazon.ionsql.errorhandling

import com.amazon.ionsql.Base
import org.junit.Test


class PropertyBagTest : Base() {

    val lineNo = Property.LINE_NO
    val columnNo = Property.COLUMN_NO
    val emptyBag: PropertyBag = PropertyBag()
    val onlyColumnBag: PropertyBag = PropertyBag().addProperty(columnNo, java.lang.Long(11L))


    @Test fun getPropFromEmptyBag() {
        assertNull(emptyBag.getProperty(lineNo, Long::class.javaObjectType))
    }

    @Test fun getAbsentPropFromNonEmptyBag() {
        assertNull(onlyColumnBag.getProperty(lineNo, Long::class.javaObjectType))
    }


    @Test fun getPresentPropFromNonEmptyBag() {
        assertEquals(11L, onlyColumnBag.getProperty(columnNo, Long::class.javaObjectType))
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPresentPropFromNonEmptyBagWrongType() {
        onlyColumnBag.getProperty(columnNo, String::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
     fun addPropWithIncompatiletype() {
        assertNull(emptyBag.addProperty(lineNo, String::class.javaObjectType))
    }
}