package org.partiql.plan.rex

public interface RexError : Rex {

    public fun getMessage(): String

    public fun getTrace(): List<Rex>

    override fun getType(): RexType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}

internal class RexErrorImpl(message: String, trace: List<Rex>) : RexError {

    // DO NOT USE FINAL
    private var _message = message
    private var _trace = trace

    override fun getMessage(): String = _message
    override fun getTrace(): List<Rex> = _trace
}

public interface RexMissing : Rex {

    public fun getMessage(): String

    public fun getTrace(): List<Rex>

    override fun getType(): RexType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitMissing(this, ctx)
}

internal class RexMissingImpl(message: String, trace: List<Rex>) : RexMissing {

    // DO NOT USE FINAL
    private var _message = message
    private var _trace = trace

    override fun getMessage(): String = _message
    override fun getTrace(): List<Rex> = _trace
}
