package com.wordlearning.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min = 3, max = 20, message = "username 3-20 chars")
    private String username;
    @NotBlank @Size(min = 6, max = 128, message = "password 6-128 chars")
    private String password;
    @Email @NotBlank
    private String email;
    private String nickname;
}
