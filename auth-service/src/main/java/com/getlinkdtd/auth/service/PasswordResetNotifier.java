package com.getlinkdtd.auth.service;

import com.getlinkdtd.auth.domain.User;

public interface PasswordResetNotifier {
    void send(User user, String rawToken);
}
