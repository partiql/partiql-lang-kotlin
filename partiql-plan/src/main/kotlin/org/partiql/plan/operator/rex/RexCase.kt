package org.partiql.plan.operator.rex

/**
 * Representative of the simple CASE-WHEN.
 */
public interface RexCase : Rex {

    public fun getMatch(): Rex

    public fun getBranches(): List<RexCaseBranch>

    public fun getDefault(): Rex

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCase(this, ctx)

    override fun getOperands(): List<Rex> {
        val operands = mutableListOf<Rex>()
        operands.add(getMatch())
        operands.addAll(getBranches().map { it.getResult() })
        operands.addAll(getBranches().map { it.getCondition() })
        operands.add(getDefault())
        return operands
    }

    /**
     * Default [RexCase] meant for extension.
     */
    abstract class Base(match: Rex, branches: List<RexCaseBranch>, default: Rex) : RexCase {

        // DO NOT USE FINAL
        private var _match = match
        private var _branches = branches
        private var _default = default
        private var _operands: List<Rex>? = null

        override fun getMatch(): Rex = _match

        override fun getBranches(): List<RexCaseBranch> = _branches

        override fun getDefault(): Rex = _default

        override fun getOperands(): List<Rex> {
            if (_operands == null) {
                _operands = super.getOperands()
            }
            return _operands!!
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RexCase) return false
            if (_match != other.getMatch()) return false
            if (_branches != other.getBranches()) return false
            if (_default != other.getDefault()) return false
            return true
        }

        override fun hashCode(): Int {
            var result = _match.hashCode()
            result = 31 * result + _branches.hashCode()
            result = 31 * result + _default.hashCode()
            return result
        }
    }
}
