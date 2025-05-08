package com.blog.enums;

public enum AccountOperationType {
    KICK_OUT(2, "踢蹬"),
    BAN(1, "封禁"),
    UNBAN(0, "解封");

    private final int code;
    private final String description;

    AccountOperationType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static AccountOperationType getByCode(int code) {
        for (AccountOperationType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid operation type code: " + code);
    }
} 