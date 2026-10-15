package com.harish.student_management.exception;

public class SchoolAlreadyExistsException extends RuntimeException {

    public SchoolAlreadyExistsException(String message) {
        super(message);
    }
}
