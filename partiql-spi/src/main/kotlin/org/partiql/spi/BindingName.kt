package org.partiql.spi

/**
 * Encapsulates the data necessary to perform a binding lookup.
 */
data class BindingName(val name: String, val bindingCase: BindingCase) {
    val loweredName: String by lazy(LazyThreadSafetyMode.PUBLICATION) { name.toLowerCase() }
    /**
     * Compares [name] to [otherName] using the rules specified by [bindingCase].
     */
    fun isEquivalentTo(otherName: String?) = otherName != null && name.isBindingNameEquivalent(otherName, bindingCase)

    /**
     * Compares this string to [other] using the rules specified by [case].
     */
    private fun String.isBindingNameEquivalent(other: String, case: BindingCase): Boolean =
        when (case) {
            BindingCase.SENSITIVE -> this.equals(other)
            BindingCase.INSENSITIVE -> this.equals(other, ignoreCase = true)
        }
}
