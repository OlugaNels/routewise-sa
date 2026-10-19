package com.routewise.sa.util

/**
 * Pure input checks used by the login, register and report screens.
 * No Android classes, so they can be unit tested on the JVM.
 */
object InputValidator {

    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PASSWORD_LENGTH = 64
    const val MAX_NAME_LENGTH = 60
    const val MAX_DESCRIPTION_LENGTH = 500
    const val MAX_ROAD_NAME_LENGTH = 100

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PHONE_REGEX = Regex("^\\+?[0-9 ]{9,15}$")

    fun isValidEmail(email: String): Boolean {
        val e = email.trim()
        return e.length <= 254 && EMAIL_REGEX.matches(e)
    }

    fun isValidPassword(password: String): Boolean =
        password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH

    fun isValidName(name: String): Boolean {
        val n = name.trim()
        return n.length in 2..MAX_NAME_LENGTH
    }

    /** Phone is optional: blank is accepted, anything else must look like a phone number. */
    fun isValidOptionalPhone(phone: String): Boolean {
        val p = phone.trim()
        return p.isEmpty() || PHONE_REGEX.matches(p)
    }

    fun isDescriptionWithinLimit(description: String): Boolean =
        description.length <= MAX_DESCRIPTION_LENGTH

    fun isValidRoadName(road: String): Boolean {
        val r = road.trim()
        return r.isNotEmpty() && r.length <= MAX_ROAD_NAME_LENGTH
    }

    /** Returns an error message for the email field, or null when it is valid. */
    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email address"
        !isValidEmail(email) -> "Enter a valid email, e.g. name@example.com"
        else -> null
    }

    /** Returns an error message for the password field, or null when it is valid. */
    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "Enter your password"
        password.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
        password.length > MAX_PASSWORD_LENGTH -> "Password must be at most $MAX_PASSWORD_LENGTH characters"
        else -> null
    }
}
