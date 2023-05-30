package org.partiql.examples

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.types.StaticType

abstract class MergeKeysBaseExprFunction : ExprFunction

/**
 * For the Given [ExprValue] representing collection of structs, merges key/values based on the given inputs in flatten list
 * for values.
 *
 * E.g.
 * Given:
 *  [
 *      {'Name':'certificate','Values':['abc', 'cde']},
 *      {'Name':'certificate','Values':['ghj', 'klu']},
 *      {'Name':'test','Values':['ghj', 'klu']}
 *  ]
 *  'Name' as mergeKey
 *  'Values' as valueKey
 *
 *  Expected:
 *  [
 *      {'test': ['ghj', 'klu']},
 *      {'certificate': ['abc', 'cde', 'ghj', 'klu']}
 *  ]
 */
class MergeKeyValues : MergeKeysBaseExprFunction() {
    override val signature = FunctionSignature(
        name = "merge_key_values",
        requiredParameters = listOf(
            StaticType.unionOf(StaticType.BAG, StaticType.LIST, StaticType.SEXP),
            StaticType.STRING,
            StaticType.STRING
        ),
        returnType = StaticType.LIST
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val mergeKey = required[1].stringValue()
        val valueKey = required[2].stringValue()
        val result = HashMap<String, MutableList<ExprValue>>()

        required[0].forEach {
            if (it.type != ExprValueType.STRUCT) {
                throw Exception("All elements on input collection must be of type struct. Erroneous value: $it")
            }

            val binding = it.bindings[BindingName(mergeKey, BindingCase.INSENSITIVE)]
            val bindingValue = binding!!.stringValue()
            val valueElem = it.bindings[BindingName(valueKey, BindingCase.INSENSITIVE)]

            if (valueElem != null) {
                if (result[bindingValue] == null) {
                    result[bindingValue] = mutableListOf(valueElem)
                } else {
                    result[bindingValue]!!.add(valueElem)
                }
            }
        }

        val keys = result.keys.map { ExprValue.newString(it) }
        val values = result.values.map { ExprValue.newList(it).flatten() }

        val listOfStructs = keys.zip(values)
            .map {
                ExprValue.newStruct(
                    listOf(ExprValue.newList(it.second).namedValue(it.first)).asSequence(),
                    StructOrdering.UNORDERED
                )
            }
        return ExprValue.newList(listOfStructs)
    }
}
