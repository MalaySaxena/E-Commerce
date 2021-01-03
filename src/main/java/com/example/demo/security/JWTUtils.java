package com.example.demo.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.example.demo.model.persistence.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

@Component
public class JWTUtils {

    private JWTAuthenticationFilter jwtAuthenticationFilter;
    private JWTAuthenticationVerificationFilter jwtAuthenticationVerificationFilter;

    public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

        private AuthenticationManager authenticationManager;

        public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
            this.authenticationManager = authenticationManager;
        }

        @Override
        public Authentication attemptAuthentication(HttpServletRequest req,
                                                    HttpServletResponse res) throws AuthenticationException {
            try {
                User credentials = new ObjectMapper()
                        .readValue(req.getInputStream(), User.class);

                return authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                credentials.getUsername(),
                                credentials.getPassword(),
                                new ArrayList<>()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void successfulAuthentication(HttpServletRequest req,
                                                HttpServletResponse res,
                                                FilterChain chain,
                                                Authentication auth) throws IOException, ServletException {

            String token = JWT.create()
                    .withSubject(((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername())
                    .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                    .sign(HMAC512(SecurityConstants.SECRET.getBytes()));
            res.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
        }
    }

    public class JWTAuthenticationVerificationFilter extends BasicAuthenticationFilter {

        public JWTAuthenticationVerificationFilter(AuthenticationManager authManager) {
            super(authManager);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            String header = req.getHeader(SecurityConstants.HEADER_STRING);

            if (header == null || !header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
                chain.doFilter(req, res);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = getAuthentication(req);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(req, res);
        }

        private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest req) {
            String token = req.getHeader(SecurityConstants.HEADER_STRING);
            if (token != null) {
                String user = JWT.require(HMAC512(SecurityConstants.SECRET.getBytes())).build()
                        .verify(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
                        .getSubject();
                if (user != null) {
                    return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                }
                return null;
            }
            return null;
        }

    }

    public JWTUtils(AuthenticationManager authenticationManager) {
        this.jwtAuthenticationFilter = new JWTAuthenticationFilter(authenticationManager);
        this.jwtAuthenticationVerificationFilter = new JWTAuthenticationVerificationFilter(authenticationManager);
    }

    public JWTAuthenticationFilter getJwtAuthenticationFilter() {
        return jwtAuthenticationFilter;
    }

    public void setJwtAuthenticationFilter(JWTAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    public JWTAuthenticationVerificationFilter getJwtAuthenticationVerificationFilter() {
        return jwtAuthenticationVerificationFilter;
    }

    public void setJwtAuthenticationVerificationFilter(JWTAuthenticationVerificationFilter jwtAuthenticationVerificationFilter) {
        this.jwtAuthenticationVerificationFilter = jwtAuthenticationVerificationFilter;
    }
}
