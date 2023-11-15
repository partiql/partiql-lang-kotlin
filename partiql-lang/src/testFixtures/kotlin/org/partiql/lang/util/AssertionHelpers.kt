package org.partiql.lang.util

import com.amazon.ion.IonValue
import org.junit.jupiter.api.fail

/**
 * Asserts that the [expectedResult] is equivalent [actualResult] using `ion-java`'s very strict notion of equality.
 *
 * If there is a mismatch, prints helpful messages to the console including both [expectedResult] and [actualResult]
 * on a single line and also pretty-printed.  This to help identify differences between the values and
 * to help them to be easier to read.
 */
fun assertIonEquals(expectedResult: IonValue, actualResult: IonValue, message: String?) {
    if (actualResult != expectedResult) {
        print("The actual value must match expected value")
        message?.let { print(it) }
        println()

        println("expected: $expectedResult")
        println("actual  : ${actualResult}\n")

        println("expected (pretty):\n${expectedResult.toPrettyString().trim()}")
        println("actual (pretty):\n${actualResult.toPrettyString().trim()}")

        fail("expected and actual values  do not match (see console output)")
    }
}
