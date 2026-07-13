package com.wimbledon.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data


public class RegisterRequest {
    @NotBlank @Size(min=2, max=80) private String name;
    @NotBlank @Size(min=3, max=50) private String username;
    @NotBlank @Size(min=4, max=100) private String password;
}
