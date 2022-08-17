package org.partiql.spi

interface Split {

    class Batch(
        val splits: List<Split>,
        val noMore: Boolean,
    )
}

interface SplitSource : Iterator<Split>, AutoCloseable
