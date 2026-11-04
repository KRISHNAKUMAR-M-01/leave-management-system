package com.company.leavemanagement.dto.response;

public record LoginResponse(String token, String email, String fullName, String role) {

}
