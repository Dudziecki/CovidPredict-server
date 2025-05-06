package com.example.server.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Метод для хеширования пароля
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12)); // 12 — количество раундов
    }

    // Метод для проверки пароля
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}