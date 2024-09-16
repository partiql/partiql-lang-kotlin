package org.partiql.lang.syntax

import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.amazon.ion.system.IonTextWriterBuilder
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

class ParserTest(
    _name: String,
    _statement: String,
    _isSuccess: Boolean
) {
    public val name: String = _name
    private val statement: String = _statement
    private val isSuccess: Boolean = _isSuccess
    private val result: String = when (isSuccess) {
        true -> "SyntaxSuccess"
        false -> "SyntaxFail"
    }

    fun write(path: String? = null) {
        val writer = writer(path)
        writeTest(writer)
        writer.flush()
    }

    companion object {
        const val ROOT = "../test/partiql-tests-runner/src/test/resources/ported/syntax"
    }

    private fun writeTest(writer: IonWriter) {
        writer.stepIn(IonType.STRUCT)
        writer.setFieldName("name")
        writer.writeString(name)
        writer.setFieldName("statement")
        writer.writeString(statement)
        writer.setFieldName("assert")
        writeAssertion(writer)
        writer.stepOut()
    }

    private fun writeAssertion(writer: IonWriter) {
        writer.stepIn(IonType.LIST)
        writer.stepIn(IonType.STRUCT)
        writer.setFieldName("result")
        writer.writeSymbol(result)
        writer.stepOut()
        writer.stepOut()
    }

    private fun writer(path: String? = null): IonWriter {
        if (path == null) {
            return IonTextWriterBuilder.pretty().build(System.out as Appendable)
        }
        val nPath = Paths.get(ROOT, path)
        val file = File(nPath.toUri())
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val fileWriter = FileWriter(file, true)
        return IonTextWriterBuilder.pretty().build(fileWriter)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParserTest) return false

        if (name != other.name) return false
        if (statement != other.statement) return false
        if (isSuccess != other.isSuccess) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + statement.hashCode()
        result = 31 * result + isSuccess.hashCode()
        return result
    }
}
