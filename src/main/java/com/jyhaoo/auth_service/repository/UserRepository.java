package com.jyhaoo.auth_service.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.jyhaoo.auth_service.model.User;

public interface UserRepository extends CrudRepository<User, Object>{
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);
}
