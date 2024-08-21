package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * Representative of the simple CASE-WHEN.
 */
public interface RexCase : Rex {

    public fun getMatch(): Rex?

    public fun getBranches(): List<Branch>

    public fun getDefault(): Rex?

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCase(this, ctx)

    override fun getOperands(): List<Rex> {
        val operands = mutableListOf<Rex>()
        val match = getMatch()
        val branches = getBranches()
        val default = getDefault()
        if (match != null) {
            operands.add(match)
        }
        for (branch in branches) {
            operands.add(branch.getCondition())
            operands.add(branch.getResult())
        }
        if (default != null) {
            operands.add(default)
        }
        return operands
    }

    /**
     * TODO DOCUMENTATION
     */
    public interface Branch {
        public fun getCondition(): Rex
        public fun getResult(): Rex
    }
}

/**
 * Default [RexCase] meant for extension.
 */
internal class RexCaseImpl(match: Rex?, branches: List<RexCase.Branch>, default: Rex?) : RexCase {

    // DO NOT USE FINAL
    private var _match = match
    private var _branches = branches
    private var _default = default
    private var _operands: List<Rex>? = null

    override fun getMatch(): Rex? = _match

    override fun getBranches(): List<RexCase.Branch> = _branches

    override fun getDefault(): Rex? = _default

    override fun getOperands(): List<Rex> {
        if (_operands == null) {
            _operands = super.getOperands()
        }
        return _operands!!
    }

    override fun getType(): PType {
        TODO("Not yet implemented")
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

    /**
     * CASE-WHEN branch
     *
     * @param condition
     * @param result
     */
    internal class Branch(condition: Rex, result: Rex) : RexCase.Branch {

        // DO NOT USE FINAL
        private var _condition = condition
        private var _result = result

        override fun getCondition(): Rex = _condition

        override fun getResult(): Rex = _result
    }
}
