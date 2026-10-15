package com.harish.student_management.service;


import com.harish.student_management.dto.*;

public interface AuthService {

    UserResponseDto register(UserRequestDto request);

    LoginResponseDto login(LoginRequestDto request);

    String forgotPassword(ForgotPasswordRequestDto request);

    String resetPassword(ResetPasswordRequestDto request);

    String changePassword(ChangePasswordRequestDto request, String username);

    LogoutResponseDto logout(String token);

}