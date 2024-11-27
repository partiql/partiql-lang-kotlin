package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * Representative of the simple CASE-WHEN.
 */
public interface RexCase : Rex {

    public fun getMatch(): Rex?

    public fun getBranches(): List<Branch>

    public fun getDefault(): Rex?

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitCase(this, ctx)

   
        val children = mutableListOf<Rex>()
        val match = getMatch()
        val branches = getBranches()
        val default = getDefault()
        if (match != null) {
            children.add(match)
        }
        for (branch in branches) {
            children.add(branch.getCondition())
            children.add(branch.getResult())
        }
        if (default != null) {
            children.add(default)
        }
        return children
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
 * Internal implementation of [RexCase].
 */
internal class RexCaseImpl(match: Rex?, branches: List<RexCase.Branch>, default: Rex?, type: RexType) : RexCase {

    // DO NOT USE FINAL
    private var _match = match
    private var _branches = branches
    private var _default = default
    private var _children: Collection<Rex>? = null
    private var _type = type

    override fun getMatch(): Rex? = _match

    override fun getBranches(): List<RexCase.Branch> = _branches

    override fun getDefault(): Rex? = _default

   
        if (_children == null) {
            _children = super.getChildren()
        }
        return _children!!
    }

    override fun getType(): RexType = _type

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
