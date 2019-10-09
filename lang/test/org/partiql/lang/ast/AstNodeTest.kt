package org.partiql.lang.ast

import com.amazon.ion.*
import com.amazon.ion.system.*
import org.junit.*
import org.junit.Assert.*

class AstNodeTest {
    // This test builds some invalid AST nodes but since we are focusing on testing AstNode interface we only care
    // about the node and it's children so it's ok

    val ion: IonSystem = IonSystemBuilder.standard().build()

    private fun <T> Iterator<T>.toList() = this.asSequence().toList()

    @Test
    fun leafNodeNoPotentialChildren() {
        // A Literal node can't have any children
        val leafNode = Literal(ion.singleValue("1"), metaContainerOf())

        assertTrue(leafNode.children.isEmpty())
        assertEquals(listOf(leafNode), leafNode.iterator().toList())
    }

    @Test
    fun leafNodeWithPotentialChildren() {
        // An NAry node can have any children.
        val leafNode = NAry(NAryOp.ADD, listOf(), metaContainerOf())

        assertTrue(leafNode.children.isEmpty())
        assertEquals(listOf(leafNode), leafNode.iterator().toList())
    }

    @Test
    fun nodeWithSingleChildrenLeaf() {
        val childNode = NAry(NAryOp.ADD, listOf(), metaContainerOf())
        val rootNode = NAry(NAryOp.ADD, listOf(childNode), metaContainerOf())

        assertEquals(listOf(childNode), rootNode.children)
        assertEquals(listOf(rootNode, childNode), rootNode.iterator().toList())
    }

    @Test
    fun nodeWithMultipleChildren() {
        val childNode1 = NAry(NAryOp.ADD, listOf(), metaContainerOf())
        val childNode2 = NAry(NAryOp.ADD, listOf(), metaContainerOf())
        val rootNode = NAry(NAryOp.ADD, listOf(childNode1, childNode2), metaContainerOf())

        assertEquals(listOf(childNode1, childNode2), rootNode.children)
        assertEquals(listOf(rootNode, childNode1, childNode2), rootNode.iterator().toList())
    }

    @Test
    fun nodeWithMultipleNonLeafChildren() {
        val depth2_1 = NAry(NAryOp.ADD, listOf(), metaContainerOf())
        val depth2_2 = NAry(NAryOp.ADD, listOf(), metaContainerOf())

        val depth1_1 = NAry(NAryOp.ADD, listOf(depth2_1), metaContainerOf())
        val depth1_2 = NAry(NAryOp.ADD, listOf(), metaContainerOf())
        val depth1_3 = NAry(NAryOp.ADD, listOf(depth2_2), metaContainerOf())
        val depth0 = NAry(NAryOp.ADD, listOf(depth1_1, depth1_2, depth1_3), metaContainerOf())

        assertEquals(listOf(depth1_1, depth1_2, depth1_3), depth0.children)
        assertEquals(listOf(depth0, depth1_1, depth2_1, depth1_2, depth1_3, depth2_2), depth0.iterator().toList())
    }
}
