package com.springboot.simplyfly.utility;

import com.springboot.simplyfly.enums.GovtIdType;

import java.util.regex.Pattern;

// Checks the format of a government ID number.
public class GovtIdUtility {

    private static final Pattern AADHAAR = Pattern.compile("^[0-9]{12}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern PASSPORT = Pattern.compile("^[A-Z][0-9]{7}$");
    private static final Pattern DRIVING_LICENSE = Pattern.compile("^[A-Z0-9]{8,16}$");

    private GovtIdUtility() {
    }

    // Returns true when the number matches the format of the given ID type.
    public static boolean isValid(GovtIdType type, String number) {
        if (type == null || number == null)
            return false;

        String value = number.trim().toUpperCase().replace(" ", "").replace("-", "");

        return switch (type) {
            case AADHAAR -> AADHAAR.matcher(value).matches();
            case PAN -> PAN.matcher(value).matches();
            case PASSPORT -> PASSPORT.matcher(value).matches();
            case DRIVING_LICENSE -> DRIVING_LICENSE.matcher(value).matches();
        };
    }
}
