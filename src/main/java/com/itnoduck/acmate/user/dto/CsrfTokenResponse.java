package com.itnoduck.acmate.user.dto;

public record CsrfTokenResponse(String token, String headerName, String parameterName) {
}
