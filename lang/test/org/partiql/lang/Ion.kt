package org.partiql.lang

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder

/**
 * One IonSystem instance for all of our unit tests.
 *
 * This is needed when working with JUnit5's [org.junit.jupiter.params.provider.ArgumentsProvider] which
 * isn't factored to allow dependency-injecting an [IonSystem] instance as we normally would.
 *
 * (Singletons like this probably should be avoided in production code.)
 */
internal val ION: IonSystem = IonSystemBuilder.standard().build()
