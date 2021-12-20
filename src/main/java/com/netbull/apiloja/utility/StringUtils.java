package com.netbull.apiloja.utility;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class StringUtils {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public PasswordEncoder getEncoder() {
        return encoder;
    }

    public String encryptPassword(String password) {
        return encoder.encode(password);
    }
}
