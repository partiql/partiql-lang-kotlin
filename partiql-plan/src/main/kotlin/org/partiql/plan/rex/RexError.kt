package org.partiql.plan.rex

import org.partiql.plan.Visitor

public interface RexError : Rex {

    public fun getMessage(): String

    public fun getTrace(): List<Rex>

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}

internal class RexErrorImpl(message: String, trace: List<Rex>) : RexError {

    // DO NOT USE FINAL
    private var _message = message
    private var _trace = trace
    private var _type = RexType.dynamic()

    override fun getMessage(): String = _message
    override fun getType(): RexType = _type
    override fun getTrace(): List<Rex> = _trace
}

public interface RexMissing : Rex {

    public fun getMessage(): String

    public fun getTrace(): List<Rex>

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitMissing(this, ctx)
}

internal class RexMissingImpl(message: String, trace: List<Rex>) : RexMissing {

    // DO NOT USE FINAL
    private var _message = message
    private var _trace = trace
    private var _type = RexType.dynamic()

    override fun getMessage(): String = _message
    override fun getType(): RexType = _type
    override fun getTrace(): List<Rex> = _trace
}
