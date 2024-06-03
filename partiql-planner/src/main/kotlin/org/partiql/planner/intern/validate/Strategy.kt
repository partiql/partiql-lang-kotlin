package org.partiql.planner.intern.validate

/**
 * Variable resolution strategies — https://partiql.org/assets/PartiQL-Specification.pdf#page=35
 *
 * | Value      | Strategy              | Scoping Rules |
 * |------------+-----------------------+---------------|
 * | LOCAL      | local-first lookup    | Rules 1, 2    |
 * | GLOBAL     | global-first lookup   | Rule 3        |
 */
internal enum class Strategy {
    LOCAL,
    GLOBAL,
}
