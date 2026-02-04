package com.wecureit.dto.response;

public record CardDetailResponse(Long id, String pan, String last4, Integer expMonth, Integer expYear) {}
