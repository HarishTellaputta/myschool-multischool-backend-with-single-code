package com.harish.student_management.enums;

public enum UserRole {
    SCHOOL_ADMIN("SCHOOL_ADMIN"),
    TEACHER("TEACHER"),
    PARENT("PARENT"),
    STUDENT("STUDENT");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public static UserRole fromString(String role) {
        for (UserRole userRole : UserRole.values()) {
            if (userRole.role.equalsIgnoreCase(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }
}
