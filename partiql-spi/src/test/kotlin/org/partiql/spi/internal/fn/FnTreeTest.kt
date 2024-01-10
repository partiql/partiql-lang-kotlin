// package org.partiql.spi.internal.fn
//
// import org.junit.jupiter.api.Test
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__FLOAT32_FLOAT32__FLOAT32
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__FLOAT64_FLOAT64__FLOAT64
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__INT16_INT16__INT16
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__INT32_INT32__INT32
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__INT64_INT64__INT64
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__INT8_INT8__INT8
// import org.partiql.plugin.internal.fn.scalar.Fn_PLUS__INT_INT__INT
// import org.partiql.types.function.FunctionSignature
// import kotlin.test.assertEquals
//
// class FnTreeTest {
//
//     @Test
//     fun lookup() {
//         val tree = FnIndexTree<FunctionSignature.Scalar>()
//         tree.insert(
//             listOf("INFORMATION_SCHEMA", "PLUS"),
//             listOf(
//                 Fn_PLUS__INT8_INT8__INT8.signature,
//                 Fn_PLUS__INT16_INT16__INT16.signature,
//                 Fn_PLUS__INT32_INT32__INT32.signature,
//                 Fn_PLUS__INT64_INT64__INT64.signature,
//                 Fn_PLUS__INT_INT__INT.signature,
//             )
//         )
//         tree.insert(
//             listOf("my_functions", "plus"),
//             listOf(
//                 Fn_PLUS__FLOAT32_FLOAT32__FLOAT32.signature,
//                 Fn_PLUS__FLOAT64_FLOAT64__FLOAT64.signature,
//             )
//         )
//         assertEquals(5, tree.search(listOf("plus")).size)
//         assertEquals(2, tree.search(listOf("my_functions", "plus")).size)
//         assertEquals(0, tree.search(listOf()).size)
//         assertEquals(0, tree.search(listOf("foo")).size)
//         assertEquals(0, tree.search(listOf("foo", "bar", "baz")).size)
//     }
// }
