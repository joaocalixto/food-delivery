package com.cloudeflair.fooddelivery.userservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    // Hash do password (SHA-256 com salt). Simplificado para a aula;
    // em producao use BCrypt/Argon2.
    private String passwordHash;
    private String salt;

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    public User() {
    }

    public User(String email, String name, String passwordHash, String salt) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
