package com.routewise.sa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    // ---- Email (register and login) ----

    @Test fun validEmailIsAccepted() = assertTrue(InputValidator.isValidEmail("sipho.dlamini@routewise.co.za"))

    @Test fun emailWithSurroundingSpacesIsAccepted() = assertTrue(InputValidator.isValidEmail("  user@example.com  "))

    @Test fun emailWithoutAtIsRejected() = assertFalse(InputValidator.isValidEmail("userexample.com"))

    @Test fun emailWithoutDomainIsRejected() = assertFalse(InputValidator.isValidEmail("user@"))

    @Test fun emailWithoutTldIsRejected() = assertFalse(InputValidator.isValidEmail("user@example"))

    @Test fun emptyEmailIsRejected() = assertFalse(InputValidator.isValidEmail(""))

    @Test fun emailWithSpecialCharactersIsRejected() = assertFalse(InputValidator.isValidEmail("<script>@x.com"))

    @Test fun veryLongEmailIsRejected() = assertFalse(InputValidator.isValidEmail("a".repeat(300) + "@example.com"))

    @Test fun emptyEmailGivesErrorMessage() = assertEquals("Enter your email address", InputValidator.emailError(""))

    @Test fun badEmailGivesErrorMessage() = assertNotNull(InputValidator.emailError("abc@"))

    @Test fun goodEmailGivesNoError() = assertNull(InputValidator.emailError("a@b.co"))

    // ---- Password ----

    @Test fun shortPasswordIsRejected() = assertFalse(InputValidator.isValidPassword("12345"))

    @Test fun minimumLengthPasswordIsAccepted() = assertTrue(InputValidator.isValidPassword("123456"))

    @Test fun veryLongPasswordIsRejected() = assertFalse(InputValidator.isValidPassword("x".repeat(65)))

    @Test fun emptyPasswordGivesErrorMessage() = assertEquals("Enter your password", InputValidator.passwordError(""))

    @Test fun shortPasswordGivesErrorMessage() = assertNotNull(InputValidator.passwordError("abc"))

    @Test fun goodPasswordGivesNoError() = assertNull(InputValidator.passwordError("Passw0rd!"))

    // ---- Name and phone ----

    @Test fun normalNameIsAccepted() = assertTrue(InputValidator.isValidName("Neo Madima"))

    @Test fun blankNameIsRejected() = assertFalse(InputValidator.isValidName("   "))

    @Test fun singleCharacterNameIsRejected() = assertFalse(InputValidator.isValidName("A"))

    @Test fun veryLongNameIsRejected() = assertFalse(InputValidator.isValidName("A".repeat(61)))

    @Test fun blankPhoneIsAccepted() = assertTrue(InputValidator.isValidOptionalPhone(""))

    @Test fun southAfricanPhoneIsAccepted() = assertTrue(InputValidator.isValidOptionalPhone("+27 82 456 7890"))

    @Test fun lettersInPhoneAreRejected() = assertFalse(InputValidator.isValidOptionalPhone("abc12345"))

    // ---- Incident report ----

    @Test fun descriptionAtLimitIsAccepted() = assertTrue(InputValidator.isDescriptionWithinLimit("x".repeat(500)))

    @Test fun descriptionOverLimitIsRejected() = assertFalse(InputValidator.isDescriptionWithinLimit("x".repeat(501)))

    @Test fun descriptionWithSpecialCharactersIsAccepted() =
        assertTrue(InputValidator.isDescriptionWithinLimit("<>\"'%&; DROP TABLE users;--"))

    @Test fun emptyDescriptionIsAccepted() = assertTrue(InputValidator.isDescriptionWithinLimit(""))

    @Test fun emptyRoadNameIsRejected() = assertFalse(InputValidator.isValidRoadName(""))

    @Test fun whitespaceRoadNameIsRejected() = assertFalse(InputValidator.isValidRoadName("    "))

    @Test fun normalRoadNameIsAccepted() = assertTrue(InputValidator.isValidRoadName("N1 Ben Schoeman"))

    @Test fun veryLongRoadNameIsRejected() = assertFalse(InputValidator.isValidRoadName("R".repeat(101)))
}
