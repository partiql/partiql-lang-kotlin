package org.partiql.ast

public interface AstVisitor<R, C>

/**
 * Base AstNode Interface
 *
 */
public interface AstNode {

    public val children: List<AstNode>

    public fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R
}

/**
 * Node Interface
 */
public interface IdentifierNode : AstNode {

    public val id: String
    public val caseSensitive: Boolean

    fun component1(): String

    fun component2(): Boolean

    fun copy(id: String = this.id, caseSensitive: Boolean = this.caseSensitive)

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

/**
 * Generated Implementation
 */
internal open class IdentifierNodeGenImpl(
    override val id: String,
    override val caseSensitive: Boolean,
) : IdentifierNode {

    // the rest of the node code including children and visitors
    override fun component1(): String {
        TODO("Not yet implemented")
    }

    override fun component2(): Boolean {
        TODO("Not yet implemented")
    }

    override fun copy(id: String, caseSensitive: Boolean) {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }

    override val children: List<AstNode>
        get() = TODO("Not yet implemented")

    override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R {
        TODO("Not yet implemented")
    }
}

// We get everything via super
// Hand write any custom logic, needs to be wired to the default factory
internal class IdentifierNodeImpl(id: String, caseSensitive: Boolean) : IdentifierNodeGenImpl(id, caseSensitive) {

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is IdentifierNode) {
            return false
        }
        // ignore case unless BOTH are case-sensitive
        val ignoreCase = !(other.caseSensitive && this.caseSensitive)
        return other.id.equals(this.id, ignoreCase)
    }
}

// default impl
fun identifierNode(id: String, case: Boolean): IdentifierNode {
    // decorate with additional functionality to fix equality for identifiers
    return IdentifierNodeImpl(id, case)
}
