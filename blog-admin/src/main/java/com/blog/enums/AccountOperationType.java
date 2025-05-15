package com.blog.enums;

import lombok.Getter;

@Getter
public enum AccountOperationType {
    UNBAN("0", "解封"),
    BAN("1", "封禁"),
    KICK_OUT("2", "踢蹬");

    private final String code;
    private final String message;

    AccountOperationType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static AccountOperationType getByCode(String code) {
        for (AccountOperationType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid operation type code: " + code);
    }
} 