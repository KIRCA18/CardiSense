package mk.ukim.finki.backend.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.dto.*;
import mk.ukim.finki.backend.service.interfaces.JwtService;
import mk.ukim.finki.backend.service.interfaces.UserService;
import mk.ukim.finki.backend.web.mapper.UserMapper;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.web.request.CreateUserRequest;
import mk.ukim.finki.backend.web.request.LogInRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserDto> signUp(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserMapper.toResponse(user));
    }

    @PostMapping("/log-in")
    public ResponseEntity<UserDto> logIn(@Valid @RequestBody LogInRequest request) {
        User user = userService.logIn(request.email(), request.password());

        String token = jwtService.generateToken(user);
        ResponseCookie cookie = authCookie(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(UserMapper.toResponse(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<UserDto> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        userService.verifyEmail(request.email(), request.code());
        User user = userService.findByEmail(request.email());

        String token = jwtService.generateToken(user);
        ResponseCookie cookie = authCookie(token);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(UserMapper.toResponse(user));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendCodeRequest request) {
        userService.resendCode(request.email());
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie authCookie(String token) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();
    }
}
