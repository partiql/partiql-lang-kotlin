/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.Base
import com.amazon.ionsql.util.exprValue

abstract class EvaluatorBase : Base() {
    val evaluator = EvaluatingCompiler(ion)

    fun eval(source: String): ExprValue =
        evaluator
            .compile(source)
            .eval(
                Bindings.over {
                    when (it) {
                        "a" -> literal("{b:{c:{d:{e:5, f:6}}}}").exprValue()
                        "i" -> literal("1").exprValue()
                        "f" -> literal("2e0").exprValue()
                        "d" -> literal("3d0").exprValue()
                        "s" -> literal("\"hello\"").exprValue()
                        "animals" -> literal(
                            """
                            [
                              {name: "Kumo", type: "dog"},
                              {name: "Mochi", type: "dog"},
                              {name: "Lilikoi", type: "unicorn"},
                            ]
                            """).exprValue()
                        "animal_types" -> literal(
                            """
                            [
                              {id: "dog", is_magic: false},
                              {id: "cat", is_magic: false},
                              {id: "unicorn", is_magic: true},
                            ]
                            """).exprValue()
                        "stores" -> literal(
                            """
                            [
                              {
                                id: "5",
                                books: [
                                  {title:"A", price: 5.0, categories:["sci-fi", "action"]},
                                  {title:"B", price: 2.0, categories:["sci-fi", "comedy"]},
                                  {title:"C", price: 7.0, categories:["action", "suspense"]},
                                  {title:"D", price: 9.0, categories:["suspense"]},
                                ]
                              },
                              {
                                id: "6",
                                books: [
                                  {title:"A", price: 5.0, categories:["sci-fi", "action"]},
                                  {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
                                  {title:"F", price: 10.0, categories:["history"]},
                                ]
                              }
                            ]
                            """).exprValue()
                        "friends" -> literal(
                            """
                            {
                               kumo: {
                                 type: "dog",
                                 likes: {
                                   mochi: { type: "dog" },
                                   zoe: { type: "human" },
                                 }
                               },
                               mochi: {
                                 type: "dog",
                                 likes: {
                                   kumo: { type: "dog" },
                                   brownie: { type: "cat" },
                                 }
                               },
                            }
                            """).exprValue()
                        else -> null
                    }
                }
            )

    fun voidEval(source: String) {
        // force materialization
        eval(source).ionValue
    }

    fun assertEval(source: String,
                   expectedLit: String,
                   block: AssertExprValue.() -> Unit = { }) {
        val expectedIon = literal(expectedLit)
        val exprVal = eval(source)
        AssertExprValue(exprVal)
            .apply {
                assertIonValue(expectedIon)
            }
            .run(block)
    }
}
