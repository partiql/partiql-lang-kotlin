package org.partiql.lib.tpc.formats.ion

import org.partiql.lib.tpc.ResultSet
import org.partiql.lib.tpc.formats.ResultSetWriter

/**
 * This is an early, highly-simplified implementation of a ResultSetWriter which outputs Ion to a file. I believe we
 * can greatly improve by having a streaming writer as well as having the binary Ion option
 *
 * I may tackle those now depending on how things are going.
 */
class IonResultSetWriter : ResultSetWriter {

    override fun open() {
        TODO("Not yet implemented")
    }

    override fun write(records: ResultSet) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}