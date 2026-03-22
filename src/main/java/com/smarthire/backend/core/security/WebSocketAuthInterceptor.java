package com.smarthire.backend.core.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    if (jwtUtil.isTokenValid(token)) {
                        Claims claims = jwtUtil.parseToken(token);
                        String email = claims.getSubject();
                        Long userId = claims.get("userId", Long.class);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        // Principal name = userId.toString()
                        // để khớp với convertAndSendToUser(userId.toString(), ...)
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()) {
                                    @Override
                                    public String getName() {
                                        return userId.toString();
                                    }
                                };

                        accessor.setUser(authentication);
                        log.info("WebSocket STOMP connected: {} (userId={})", email, userId);
                    } else {
                        log.warn("WebSocket STOMP connect with invalid/expired token");
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication error: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket STOMP connect without Authorization header");
            }
        }

        return message;
    }
}
