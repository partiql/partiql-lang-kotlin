package org.partiql.ast.v1.graph

/**
 * TODO docs, equals, hashcode
 */
public enum class GraphDirection {
    LEFT,
    UNDIRECTED,
    RIGHT,
    LEFT_OR_UNDIRECTED,
    UNDIRECTED_OR_RIGHT,
    LEFT_OR_RIGHT,
    LEFT_UNDIRECTED_OR_RIGHT,
    OTHER,
}
