package com.practice.practice.dto;

import lombok.Data;

@Data
public class RegisterUserRequest {

    private String name;
    private String email;
    private String password;
}
