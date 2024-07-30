package org.partiql.plan.logical

import kotlin.random.Random

public interface Node {

    public fun getTag(): String = "-${"%06x".format(Random.nextInt())}"

    public fun getChildren(): List<Node> = emptyList()
}
