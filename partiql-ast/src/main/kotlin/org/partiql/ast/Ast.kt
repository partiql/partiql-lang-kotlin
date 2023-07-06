package org.partiql.ast

import org.partiql.ast.builder.AstFactoryImpl

/**
 * Singleton instance of the default factory; also accessible via `AstFactory.DEFAULT`.
 */
object Ast : AstBaseFactory()

/**
 * AstBaseFactory can be used to create a factory which extends from the factory provided by AstFactory.DEFAULT.
 */
public abstract class AstBaseFactory : AstFactoryImpl() {
    // internal default overrides here
}

public abstract class MyFactory : AstBaseFactory() {

    override fun identifierSymbol(symbol: String, caseSensitivity: Identifier.CaseSensitivity): Identifier.Symbol {
        return ComparableIdentifier(_id(), symbol, caseSensitivity)
    }
}

//
class ComparableIdentifier(
    override val _id: String,
    override val symbol: String,
    override val caseSensitivity: Identifier.CaseSensitivity,
) : Identifier.Symbol {

    override fun copy(symbol: String, caseSensitivity: Identifier.CaseSensitivity): Identifier.Symbol {
        TODO("Not yet implemented")
    }

    override val children: List<AstNode> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Identifier.Symbol) return false // different type
        if (other === this) return true // same object
        return when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> this.symbol == other.symbol
            Identifier.CaseSensitivity.INSENSITIVE -> this.symbol.lowercase() == other.symbol.lowercase()
        }
    }
}
