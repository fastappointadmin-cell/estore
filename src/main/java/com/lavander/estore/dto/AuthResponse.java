package com.lavander.estore.dto;

public record AuthResponse(String token, UserDto user) {
}
