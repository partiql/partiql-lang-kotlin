package org.partiql.shape

import org.partiql.value.PartiQLType

/**
 * Manages all interactions with a [PShape].
 */
public interface PartiQLShapeSystem {

    public fun create(type: PartiQLType, constraints: Iterable<Constraint>, metas: Iterable<Meta>)

    public fun add(shape: PShape, constraint: Constraint)

    public fun remove(shape: PShape, constraint: Constraint)

    public fun add(shape: PShape, meta: Meta)

    public fun remove(shape: PShape, constraint: Meta)

    public companion object {
        @JvmStatic
        public fun default(): PartiQLShapeSystem = DEFAULT
    }

    private object DEFAULT : PartiQLShapeSystem {
        override fun create(type: PartiQLType, constraints: Iterable<Constraint>, metas: Iterable<Meta>) {
            TODO("Not yet implemented")
        }

        override fun add(shape: PShape, constraint: Constraint) {
            TODO("Not yet implemented")
        }

        override fun add(shape: PShape, meta: Meta) {
            TODO("Not yet implemented")
        }

        override fun remove(shape: PShape, constraint: Constraint) {
            TODO("Not yet implemented")
        }

        override fun remove(shape: PShape, constraint: Meta) {
            TODO("Not yet implemented")
        }

    }
}
