package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.config.PasswordResetProperties;
import com.getlinkdtd.auth.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "getlink.auth.password-reset", name = "log-token", havingValue = "true")
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPasswordResetNotifier.class);
    private final PasswordResetProperties properties;

    public LoggingPasswordResetNotifier(PasswordResetProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(User user, String rawToken) {
        LOGGER.info("Development password reset URL: {}/reset-password?token={}",
                properties.frontendBaseUrl(), rawToken);
    }
}
