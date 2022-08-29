package org.partiql.lang.ots_work.interfaces

enum class ScalarOpId(val alias: String) {
    ScalarCast("cast"),
    ScalarIs("is"),
    Not("not"),
    Pos("+"),
    Neg("-"),
    BinaryPlus("+"),
    BinaryMinus("-"),
    BinaryTimes("*"),
    BinaryDivide("/"),
    BinaryModulo("%"),
    BinaryConcat("||"),
    Like("like")
}
