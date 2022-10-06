package OTS.ITF.org.partiql.ots.type

/**
 * For now, we assume all the type parameters are optional and all the type parameters
 * are integers.
 *
 * It is exactly the same as what is specified in the query as parameters
 * for a type. e.g.
 *      1. In the expression `CAST(1 AS CHARACTER)`, the parameters of `CHARACTER` type is an empty list
 *      2. In the expression `CAST(1 AS DECIMAL(1))`, the parameters of `DECIMAL` type is [1]
 *      2. In the expression `CAST(1 AS DECIMAL(2, 1))`, the parameters of `DECIMAL` type is [2, 1]
 */
typealias TypeParameters = List<Int>
