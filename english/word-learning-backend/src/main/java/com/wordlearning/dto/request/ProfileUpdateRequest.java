package com.wordlearning.dto.request;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String nickname;
    private String bio;
    private String avatarUrl;
}
