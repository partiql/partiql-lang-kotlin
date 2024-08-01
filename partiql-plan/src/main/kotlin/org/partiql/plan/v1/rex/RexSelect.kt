package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 */
public interface RexSelect : Rex {

    public fun getInput(): Rel

    public fun getConstructor(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSelect(this, ctx)

    public abstract class Base(input: Rel, constructor: Rex) : RexSelect {

        private var _input = input
        private var _constructor = constructor

        public override fun getInput(): Rel = _input

        public override fun getConstructor(): Rex = _constructor

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Base) return false

            if (_input != other._input) return false
            if (_constructor != other._constructor) return false

            return true
        }

        public override fun hashCode(): Int {
            var result = _input.hashCode()
            result = 31 * result + _constructor.hashCode()
            return result
        }
    }
}
