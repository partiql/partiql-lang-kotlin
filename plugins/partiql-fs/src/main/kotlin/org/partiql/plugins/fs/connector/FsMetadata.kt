package org.partiql.plugins.fs.connector

import org.partiql.plugins.fs.index.FsIndex
import org.partiql.plugins.fs.index.FsNode
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.PartiQLException
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.connector.sql.SqlMetadata
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.types.StaticType

internal class FsMetadata(
    private val index: FsIndex,
    private val info: InfoSchema,
) : SqlMetadata(ignore, info) {

    private companion object {

        // TODO remove session from SqlMetadata.
        @JvmStatic
        val ignore = object : ConnectorSession {
            override fun getQueryId(): String = ""
            override fun getUserId(): String = ""
        }
    }

    override fun getObject(path: BindingPath): ConnectorHandle.Obj? {
        val (obj, match) = index.search(path) ?: return null
        return ConnectorHandle.Obj(
            path = ConnectorPath(match),
            entity = FsObject(obj.shape)
        )
    }

    override fun ls(path: BindingPath): List<ConnectorHandle<*>> {
        return index.list(path).map {
            when (it) {
                is FsNode.Obj -> ConnectorHandle.Obj(
                    path = ConnectorPath(path.normalized + it.name),
                    entity = FsObject(it.shape)
                )
                is FsNode.Scope -> ConnectorHandle.Scope(
                    path = ConnectorPath(path.normalized + it.name),
                    entity = it.name,
                )
            }
        }
    }

    private fun BindingName.normalize() = when(this.case) {
        BindingCase.SENSITIVE -> this.name
        BindingCase.INSENSITIVE -> this.name.uppercase()
    }

    /**
     * FS CREATE TABLE Semantics:
     * 1. given a binding path
     *      if size = 1: create the table under root
     *      if size > 1: let path be p0....pn, pn is the table name, p0 is a directory d0 under root,
     *                   p1 is a directory under d1, if any d_x does not exist, we through an error.
     * 2. assumes the table to be created is named t, if the target directory already contains a table named t
     *    an exception is thrown.
     *
     * TODO: Seperate metadata layer and data layer, may as well be two directories under root.
     */
    override fun createTable(
        path: BindingPath,
        shape: StaticType,
        checkExpression: List<String>,
        unique: List<String>,
        primaryKey: List<String>
    ): ConnectorHandle.Obj {
        val (tableName, dirs) =  when (path.steps.size){
            1 -> path.steps.first().normalize() to null
            else -> path.steps.last().normalize() to BindingPath(path.steps.dropLast(1))
        }

        // if we are not creating under root
        val dirPath = if (dirs != null) {
            val handles = ls(dirs)
            when(handles.size) {
                0 -> throw PartiQLException("dir not exist")
                1 -> {
                    val handle = handles.first()
                    when(handle) {
                        is ConnectorHandle.Scope -> handle.path
                        else -> throw PartiQLException("Path did not bind to a scope")
                    }
                }
                else -> throw PartiQLException("ambiguous binding")
            }
        } else {
            null
        }

        // check table already exists
        if (ls(path).any { it is ConnectorHandle.Obj }) throw PartiQLException("Table already exists")



        index.createTable(
            dirPath,
            tableName,
            shape,
            checkExpression,
            unique,
            primaryKey
        )

        return ConnectorHandle.Obj(
            ConnectorPath(path.normalized),
            FsObject(shape)
        )
    }
}
