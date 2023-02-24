package org.partiql.lang.infer

import org.partiql.lang.eval.BindingName

public class QualifiedObjectName(
    public val catalogName: BindingName?,
    public val schemaName: BindingName?,
    public val objectName: BindingName?
)
