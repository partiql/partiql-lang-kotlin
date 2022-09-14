package org.partiql.lang.ots_work.interfaces.operator

enum class ScalarOpId(val alias: String) {
    ScalarCast("cast"),
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
