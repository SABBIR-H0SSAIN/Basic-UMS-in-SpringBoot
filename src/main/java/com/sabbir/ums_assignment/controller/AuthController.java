package com.sabbir.ums_assignment.controller;

import com.sabbir.ums_assignment.dto.*;
import com.sabbir.ums_assignment.exception.ApiException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    // The frontend uses the existing session to display the correct navigation links.
    @GetMapping("/me")
    public Map<String, String> currentUser(Authentication authentication) {
        return Map.of("username", authentication.getName(), "role",
                authentication.getAuthorities().iterator().next().getAuthority().substring(5));
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest body,
                                   HttpServletRequest request, HttpServletResponse response) {
        if (body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username().trim(), body.password()));
        // Rotate the session ID to prevent session fixation after successful login.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        // The next request to /csrf will create a fresh token for the logged-in session.
        new HttpSessionCsrfTokenRepository().saveToken(null, request, response);
        return Map.of("username", authentication.getName(), "role",
                authentication.getAuthorities().iterator().next().getAuthority().substring(5));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());
        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", "").path("/").httpOnly(true)
                .maxAge(0).sameSite("Lax").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return Map.of("message", "Logged out successfully");
    }
}
