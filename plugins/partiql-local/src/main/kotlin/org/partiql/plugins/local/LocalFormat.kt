package org.partiql.plugins.local

enum class LocalFormat {
    ION;

    companion object {

        fun safeValueOf(value: String): LocalFormat? = try {
            valueOf(value.uppercase().trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
