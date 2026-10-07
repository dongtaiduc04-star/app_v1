package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.web.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTests {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsPasswordsAtBothLengthBoundaries() {
        assertThatCode(() -> policy.validate("Aa1!bcdefg")).doesNotThrowAnyException();
        assertThatCode(() -> policy.validate("Aa1!bcdefghijklmnopqrstuv")).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordsOutsideLengthRangeOrMissingARequiredCharacterGroup() {
        assertRejected("Aa1!bcdef");
        assertRejected("Aa1!bcdefghijklmnopqrstuvw");
        assertRejected("aa1!bcdefg");
        assertRejected("AA1!BCDEFG");
        assertRejected("Aaa!bcdefg");
        assertRejected("Aa11bcdefg");
        assertRejected("Aa1 bcdefg");
    }

    private void assertRejected(String password) {
        assertThatThrownBy(() -> policy.validate(password))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo("PASSWORD_POLICY_VIOLATION"));
    }
}
