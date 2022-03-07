package org.partiql.lang.eval.relation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RelationTests {

    @Test
    fun relType() {
        val rel = relation(RelationType.BAG) {  }
        assertEquals(RelationType.BAG, rel.relType)
    }

    @Test
    fun `0 yields`() {
        val rel = relation(RelationType.BAG) {  }
        assertEquals(RelationType.BAG, rel.relType)
        assertFalse(rel.nextRow())
        assertThrows<IllegalStateException> { rel.nextRow() }
    }

    @Test
    fun `1 yield`() {
        val rel = relation(RelationType.BAG) { yield() }
        assertTrue(rel.nextRow())
        assertFalse(rel.nextRow())
        assertThrows<IllegalStateException> { rel.nextRow() }
    }

    @Test
    fun `2 yields`() {
        val rel = relation(RelationType.BAG) {
            yield()
            yield()
        }
        assertTrue(rel.nextRow())
        assertTrue(rel.nextRow())
        assertFalse(rel.nextRow())
        assertThrows<IllegalStateException> { rel.nextRow() }
    }

    @Test
    fun `3 yields`() {
        val rel = relation(RelationType.BAG) {
            yield()
            yield()
            yield()
        }
        assertTrue(rel.nextRow())
        assertTrue(rel.nextRow())
        assertTrue(rel.nextRow())
        assertFalse(rel.nextRow())
        assertThrows<IllegalStateException> { rel.nextRow() }
    }
}