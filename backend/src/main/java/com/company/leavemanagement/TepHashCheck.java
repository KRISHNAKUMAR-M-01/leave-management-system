package com.company.leavemanagement;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TepHashCheck implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    public TepHashCheck(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String password = "Krishnakumar@Admin123";

        String hash1 = passwordEncoder.encode(password);
        String hash2 = passwordEncoder.encode(password);

        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Right password matches: " + passwordEncoder.matches(password, hash1));
        System.out.println("Wrong password matches: " + passwordEncoder.matches("wrong", hash1));
    }
}
