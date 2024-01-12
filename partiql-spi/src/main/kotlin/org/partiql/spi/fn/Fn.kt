package org.partiql.spi.fn

/**
 * Simple common interface shared between [FnScalar] and [FnAggregation].
 */
@FnExperimental
public interface Fn {

    public val signature: FnSignature
}
