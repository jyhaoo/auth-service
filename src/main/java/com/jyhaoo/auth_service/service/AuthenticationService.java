package com.jyhaoo.auth_service.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jyhaoo.auth_service.dto.LoginUserDto;
import com.jyhaoo.auth_service.dto.RegisterUserDto;
import com.jyhaoo.auth_service.dto.VerifyUserDto;
import com.jyhaoo.auth_service.model.User;
import com.jyhaoo.auth_service.repository.UserRepository;

import jakarta.mail.MessagingException;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final EmailService emailService;

    public AuthenticationService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
    }
    
    public User signup(RegisterUserDto registerUserDto) {
        User user = new User(registerUserDto.getUsername(), 
                             registerUserDto.getEmail(), 
                             passwordEncoder.encode(registerUserDto.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationExpiry(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto loginUserDto) {
        User user = userRepository.findByEmail(loginUserDto.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is not verified");
        }
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginUserDto.getEmail(), 
                loginUserDto.getPassword()
            )
        );

        return user;
    }

    public void verifyUser(VerifyUserDto verifyUserDto) {
        Optional<User> optionalUser = userRepository.findByEmail(verifyUserDto.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }

            if (user.getVerificationCode().equals(verifyUserDto.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationExpiry(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationEmail(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("User account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationExpiry(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(User user) {
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
}
