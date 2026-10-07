package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private static final int MINIMUM_LENGTH = 10;
    private static final int MAXIMUM_LENGTH = 25;

    public void validate(String password) {
        boolean validLength = password != null
                && password.length() >= MINIMUM_LENGTH
                && password.length() <= MAXIMUM_LENGTH;
        boolean hasUppercase = password != null && password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password != null && password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password != null && password.chars().anyMatch(Character::isDigit);
        boolean hasSymbol = password != null && password.chars()
                .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));

        if (!(validLength && hasUppercase && hasLowercase && hasDigit && hasSymbol)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_POLICY_VIOLATION",
                    "Password must be 10-25 characters and include uppercase, lowercase, number and special character");
        }
    }
}
