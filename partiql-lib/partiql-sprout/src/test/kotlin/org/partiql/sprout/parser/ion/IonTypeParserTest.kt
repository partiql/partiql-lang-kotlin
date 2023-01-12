package org.partiql.sprout.parser.ion

import org.junit.jupiter.api.Test
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.Universe

internal class IonTypeParserTest {

    @Test
    fun parseSimple() {
        val input = """
          // Sum
          x::[
            u::{},
            v::{}
          ]
          // Product
          y::{
            a: x,
            b: z
          }
          // Enum
          z::[
            FOO,
            BAR
          ]
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    @Test
    fun parseNestedSumTypes() {
        val input = """
          t::[
            u::[
              a::{},
              b::{},
              c::{}
            ],
            v::[
              x::{},
              y::{},
              z::{}
            ]
          ]
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    // Inline Enum
    @Test
    fun parseInlineEnum() {
        val input = """
          // Sum
          x::[
            u::{},
            v::{}
          ]
          // Product + Inline Enum
          y::{
            a: x,
            b: [ FOO, BAR ],
          }
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    @Test
    fun parseAllTypes() {
        val input = """
          t::{
            // Ion Scalar
            my_bool:        bool,
            my_int:         int,
            my_long:        long,
            my_float:       float,
            my_double:      double,
            my_bytes:       bytes,
            my_string:      string,

            // Collections
            my_list:        list::[int],
            my_set:         set::[int],
            my_map:         map::[int,bool],
            list_of_lists:  list::[list::[int]],
            list_of_sets:   list::[set::[int]],
            list_of_maps:   list::[map::[int,bool]],
            map_of_lists:   map::[int,list::[bool]],
            map_of_sets:    map::[int,set::[bool]],
            map_of_maps:    map::[int,map::[int,bool]]
          }
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    @Test
    fun parseNullableTypes() {
        val input = """
          t::{
            my_bool:        optional::bool,
            my_int:         optional::int,
            my_long:        optional::long,
            my_float:       optional::float,
            my_double:      optional::double,
            my_bytes:       optional::bytes,
            my_string:      optional::string,
          }
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    @Test
    fun parseNullableCollectionTypes() {
        val input = """
          t::{
            my_list: optional::list::[int],
            my_set:  optional::set::[int],
            my_map:  optional::map::[int,bool],
          }
          u::{
            my_list: list::[optional::int],
            my_set:  set::[optional::int],
            my_map:  map::[int, optional::bool],
          }
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    @Test
    fun parseInlineTypes() {
        val input = """
             foo::{
                 a: [ x::{} ],                // inline sum foo.a
                 b: v::[ x::{} ],             // inline sum foo.v
                 c: optional::[ x::{} ],      // inline sum foo.c, optional field of foo
                 d: optional::x::[ x::{} ],   // inline sum foo.v, optional field of foo
                 e: {},                       // inline product foo.e
                 f: y::{},                    // inline product foo.y
                 g: optional::{},             // inline product foo.g, optional field of foo
                 h: optional::z::{},          // inline product foo.z, optional field of foo
             }
        """.trimIndent()
        val model = IonTypeParser.parse("test", input)
        println(model.pretty())
    }

    // This could be better
    private fun Universe.pretty() = buildString {
        appendLine("universe::[")
        val indent = 1
        val lead = "  ".repeat(indent)
        append(lead).appendLine("types::[")
        fun append(def: TypeDef) {
            append(lead).appendLine(def.pretty(indent + 1))
            def.children.forEach { append(it) }
        }
        types.forEach { append(it) }
        append(lead).appendLine("]")
        append("]")
    }

    private fun TypeDef.pretty(indent: Int = 0) = buildString {
        val lead = "  ".repeat(indent)
        append(lead).append(this@pretty.toString())
        if (ref.nullable) append(ref.id + "?")
    }
}
