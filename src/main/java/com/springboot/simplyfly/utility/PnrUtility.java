package com.springboot.simplyfly.utility;

import java.security.SecureRandom;

public class PnrUtility {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PnrUtility() {
    }

    // Generate a unique-format 6-character alphanumeric PNR code.
    public static String generatePnr() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        return sb.toString();
    }
}