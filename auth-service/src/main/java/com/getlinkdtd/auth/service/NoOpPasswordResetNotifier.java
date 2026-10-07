package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.domain.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "getlink.auth.password-reset",
        name = "log-token",
        havingValue = "false",
        matchIfMissing = true)
public class NoOpPasswordResetNotifier implements PasswordResetNotifier {
    @Override
    public void send(User user, String rawToken) {
        // Production-safe placeholder until an SMTP or SES implementation is configured.
    }
}
