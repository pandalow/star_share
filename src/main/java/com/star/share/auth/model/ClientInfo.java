package com.star.share.auth.model;

/**
 * Client information
 * record the client IP address and user-agent,
 * aim for risk management and activation record
 *
 * @param ip
 * @param userAgent
 */
public record ClientInfo(String ip, String userAgent) {
}
