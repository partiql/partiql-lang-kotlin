package org.partiql.plugins.jdbc

import java.sql.ResultSet
import kotlin.reflect.KClass

public interface ReadFunction {
    public fun read(resultSet: ResultSet, columnIndex: Int) : Any

    public companion object {
        public fun <T: Any> of(implementation: ReadFunctionImplementation<T>): ReadFunction = object : ReadFunction {
            override fun read(resultSet: ResultSet, columnIndex: Int): T {
                return implementation.read(resultSet, columnIndex)
            }
        }
    }

    public interface ReadFunctionImplementation<T> {
        public fun read(resultSet: ResultSet?, columnIndex: Int): T
    }
}
