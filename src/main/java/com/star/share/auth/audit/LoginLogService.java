package com.star.share.auth.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    /**
     * Record a login attempt
     *
     * @param userId     ID of the user
     * @param identifier Identifier used for login (e.g., username or email)
     * @param channel    Channel through which the login was attempted (e.g., web, mobile)
     * @param ip         IP address of the user
     * @param userAgent  User agent string of the user's device
     * @param status     Status of the login attempt (e.g., success, failure)
     */
    @Transactional
    public void record(Long userId,
                       String identifier,
                       String channel,
                       String ip,
                       String userAgent,
                       String status) {
        LoginLog log = LoginLog.builder()
                        .userId(userId)
                        .identifier(identifier)
                        .channel(channel)
                        .ip(ip)
                        .userAgent(userAgent)
                        .status(status)
                        .build();

        loginLogMapper.insert(log);
    }
}
