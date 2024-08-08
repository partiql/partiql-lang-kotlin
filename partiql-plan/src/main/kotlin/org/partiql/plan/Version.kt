package org.partiql.plan

/**
 * Marker interface for some version structure.
 */
public interface Version {

    /**
     * The only required method is toString.
     */
    public override fun toString(): String
}
