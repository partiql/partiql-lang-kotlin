package org.partiql.lang.eval

interface GlobalsCheck {
    fun isDefined(name: BindingName): Boolean

    companion object {
        fun of(session: EvaluationSession): GlobalsCheck = SessionGlobalsCheck(session)

        val empty: GlobalsCheck = EmptyGlobalsCheck()
    }

    private class SessionGlobalsCheck(val session: EvaluationSession) : GlobalsCheck {
        override fun isDefined(name: BindingName): Boolean {
            return session.globals[name] != null
        }
    }

    private class EmptyGlobalsCheck() : GlobalsCheck {
        override fun isDefined(name: BindingName): Boolean = false
    }
}
