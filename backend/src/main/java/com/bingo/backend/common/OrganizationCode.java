package com.bingo.backend.common;

public enum OrganizationCode {
    GEOURP,
    CIVIAL,
    ACI,
    ADMIN;

    public static OrganizationCode fromSerial(String serial) {
        int value = Integer.parseInt(serial);
        if (value >= 1 && value <= 1000) {
            return GEOURP;
        }
        if (value >= 1001 && value <= 2000) {
            return CIVIAL;
        }
        if (value >= 2001 && value <= 3000) {
            return ACI;
        }
        throw new IllegalArgumentException("Serial fuera de rango: " + serial);
    }
}
