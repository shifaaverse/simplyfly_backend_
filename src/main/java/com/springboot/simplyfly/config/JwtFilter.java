package com.springboot.simplyfly.config;

import com.springboot.simplyfly.model.User;
import com.springboot.simplyfly.service.MyUserDetailsService;
import com.springboot.simplyfly.utility.JwtUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtility jwtUtility;
    private final MyUserDetailsService myUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Lets read the token from the header (Authorization)
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String token = authorizationHeader.split(" ")[1];
                String username = jwtUtility.extractUsername(token);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);

                    // Blocked / not approved users are not logged in.
                    // Only user-details API is allowed so that it can show the proper message (412)
                    boolean isActive = ((User) userDetails).isActive();
                    boolean isUserDetailsApi = "/api/auth/user-details".equals(request.getRequestURI());

                    if ((isActive || isUserDetailsApi)
                            && jwtUtility.validateToken(token, userDetails.getUsername())) {

                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        usernamePasswordAuthenticationToken
                                .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    }
                }
            } catch (Exception e) {
                // Expired / wrong token: stay not logged in, Spring will send 401
                logger.warn("Invalid or expired token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

}