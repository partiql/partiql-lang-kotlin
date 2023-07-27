package org.partiql.transpiler.block

// a <> b  <-> a concat b

infix fun Block.concat(rhs: Block): Block = link(this, rhs)

infix fun Block.concat(text: String): Block = link(this, text(text))

infix operator fun Block.plus(rhs: Block): Block = link(this, rhs)

infix operator fun Block.plus(text: String): Block = link(this, text(text))

// Shorthand

val NIL = Block.Nil

val NL = Block.NL

fun text(text: String) = Block.Text(text)

fun link(lhs: Block, rhs: Block) = Block.Link(lhs, rhs)

fun nest(block: () -> Block) = Block.Nest(block())

fun list(start: String?, end: String?, delimiter: String? = ",", items: () -> List<Block>): Block {
    var h: Block = NIL
    h = if (start != null) h + start else h
    h += nest {
        val kids = items()
        var list: Block = NIL
        kids.foldIndexed(list) { i, a, item ->
            list += item
            list = if (delimiter != null && (i + 1) < kids.size) a + delimiter else a
            list
        }
    }
    h = if (end != null) h + end else h
    return h
}
