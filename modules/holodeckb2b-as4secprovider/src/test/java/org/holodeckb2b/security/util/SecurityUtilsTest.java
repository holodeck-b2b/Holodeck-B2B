/*
 * Copyright (C) 2024 The Holodeck B2B Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.security.util;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.wss4j.dom.str.STRParser;
import org.holodeckb2b.ebms3.security.util.SecurityUtils;
import org.holodeckb2b.interfaces.security.X509ReferenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Functional tests for security utility functions.
 * <p>
 * The {@link SecurityUtils} class provides conversion functions between Holodeck B2B's
 * certificate reference types and WSS4J's representations, as well as password generation.
 */
class SecurityUtilsTest {

    // ==================================================================================
    // Tests for X509 reference type conversions (Holodeck B2B -> WSS4J)
    // ==================================================================================

    @Nested
    @DisplayName("When converting X509 reference types to WSS4J format")
    class X509ToWSS4JConversion {

        @Test
        @DisplayName("BSTReference should convert to DirectReference")
        void bstReferenceConvertsToDirectReference() {
            String result = SecurityUtils.getWSS4JX509KeyId(X509ReferenceType.BSTReference);

            assertEquals("DirectReference", result);
        }

        @Test
        @DisplayName("KeyIdentifier should convert to SKIKeyIdentifier")
        void keyIdentifierConvertsToSKI() {
            String result = SecurityUtils.getWSS4JX509KeyId(X509ReferenceType.KeyIdentifier);

            assertEquals("SKIKeyIdentifier", result);
        }

        @Test
        @DisplayName("IssuerAndSerial should convert to IssuerSerial")
        void issuerAndSerialConvertsCorrectly() {
            String result = SecurityUtils.getWSS4JX509KeyId(X509ReferenceType.IssuerAndSerial);

            assertEquals("IssuerSerial", result);
        }

    }

    // ==================================================================================
    // Tests for X509 reference type conversions (WSS4J -> Holodeck B2B)
    // ==================================================================================

    @Nested
    @DisplayName("When converting WSS4J reference types to Holodeck B2B format")
    class WSS4JToX509Conversion {

        @Test
        @DisplayName("DIRECT_REF should convert to BSTReference")
        void directRefConvertsToBST() {
            X509ReferenceType result = SecurityUtils.getKeyReferenceType(STRParser.REFERENCE_TYPE.DIRECT_REF);

            assertEquals(X509ReferenceType.BSTReference, result);
        }

        @Test
        @DisplayName("KEY_IDENTIFIER should convert to KeyIdentifier")
        void keyIdentifierConvertsCorrectly() {
            X509ReferenceType result = SecurityUtils.getKeyReferenceType(STRParser.REFERENCE_TYPE.KEY_IDENTIFIER);

            assertEquals(X509ReferenceType.KeyIdentifier, result);
        }

        @Test
        @DisplayName("ISSUER_SERIAL should convert to IssuerAndSerial")
        void issuerSerialConvertsCorrectly() {
            X509ReferenceType result = SecurityUtils.getKeyReferenceType(STRParser.REFERENCE_TYPE.ISSUER_SERIAL);

            assertEquals(X509ReferenceType.IssuerAndSerial, result);
        }
    }

    // ==================================================================================
    // Tests for password generation
    // ==================================================================================

    @Nested
    @DisplayName("When generating passwords")
    class PasswordGeneration {

        @Test
        @DisplayName("it should generate a non-null password")
        void generatesNonNullPassword() {
            char[] password = SecurityUtils.generatePassword();

            assertNotNull(password);
        }

        @Test
        @DisplayName("it should generate a password with content")
        void generatesNonEmptyPassword() {
            char[] password = SecurityUtils.generatePassword();

            assertTrue(password.length > 0);
        }

        @Test
        @DisplayName("it should generate 16 character passwords")
        void generates16CharacterPassword() {
            char[] password = SecurityUtils.generatePassword();

            assertEquals(16, password.length);
        }

        @Test
        @DisplayName("it should generate different passwords on each call")
        void generatesDifferentPasswords() {
            char[] password1 = SecurityUtils.generatePassword();
            char[] password2 = SecurityUtils.generatePassword();

            // Passwords should be different (with extremely high probability)
            assertFalse(java.util.Arrays.equals(password1, password2));
        }

        @Test
        @DisplayName("it should generate hexadecimal passwords")
        void generatesHexadecimalPassword() {
            char[] password = SecurityUtils.generatePassword();

            String passwordStr = new String(password);
            // All characters should be valid hex digits
            assertTrue(passwordStr.matches("[0-9a-f]+"));
        }
    }
}
