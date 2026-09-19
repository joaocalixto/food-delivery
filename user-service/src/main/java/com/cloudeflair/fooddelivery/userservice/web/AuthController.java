package com.cloudeflair.fooddelivery.userservice.web;

import com.cloudeflair.fooddelivery.userservice.domain.User;
import com.cloudeflair.fooddelivery.userservice.repository.UserRepository;
import com.cloudeflair.fooddelivery.userservice.security.PasswordHasher;
import com.cloudeflair.fooddelivery.userservice.security.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repository;
    private final PasswordHasher hasher;
    private final TokenService tokenService;

    public AuthController(UserRepository repository, PasswordHasher hasher, TokenService tokenService) {
        this.repository = repository;
        this.hasher = hasher;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.TokenResponse register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        if (repository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ja cadastrado");
        }
        String salt = hasher.newSalt();
        String hash = hasher.hash(req.password(), salt);
        User user = repository.save(new User(req.email(), req.name(), hash, salt));
        return new AuthDtos.TokenResponse(user.getId(), user.getEmail(), tokenService.issue(user.getId()));
    }

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        User user = repository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
        if (!hasher.matches(req.password(), user.getSalt(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        }
        return new AuthDtos.TokenResponse(user.getId(), user.getEmail(), tokenService.issue(user.getId()));
    }
}
