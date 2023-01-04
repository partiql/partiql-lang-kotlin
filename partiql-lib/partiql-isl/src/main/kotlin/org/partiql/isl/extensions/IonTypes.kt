package org.partiql.isl.extensions

import org.partiql.isl.Type

object IonTypes {

    val blob = Type.Ref("blob", false)

    val bool = Type.Ref("bool", false)

    val clob = Type.Ref("clob", false)

    val decimal = Type.Ref("decimal", false)

    val float = Type.Ref("float", false)

    val int = Type.Ref("int", false)

    val string = Type.Ref("string", false)

    val symbol = Type.Ref("symbol", false)

    val timestamp = Type.Ref("timestamp", false)

    val list = Type.Ref("list", false)

    val sexp = Type.Ref("sexp", false)

    val struct = Type.Ref("struct", false)

    val any = Type.Ref("any", false)

    val lob = Type.Ref("lob", false)

    val number = Type.Ref("number", false)

    val text = Type.Ref("text", false)

    val nothing = Type.Ref("nothing", false)

    // --- nominal nullable types

    val nullableBlob = Type.Ref("blob", true)

    val nullableBool = Type.Ref("bool", true)

    val nullableClob = Type.Ref("clob", true)

    val nullableDecimal = Type.Ref("decimal", true)

    val nullableFloat = Type.Ref("float", true)

    val nullableInt = Type.Ref("int", true)

    val nullableString = Type.Ref("string", true)

    val nullableSymbol = Type.Ref("symbol", true)

    val nullableTimestamp = Type.Ref("timestamp", true)

    val nullableList = Type.Ref("list", true)

    val nullableSexp = Type.Ref("sexp", true)

    val nullableStruct = Type.Ref("struct", true)
}
