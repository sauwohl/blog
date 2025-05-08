package com.blog.dto;

import com.blog.enums.AccountOperationType;
import lombok.Data;

@Data
public class OperVO {
    /**
     * 用户账号
     */
    private String account;
    
    /**
     * 操作类型
     * 0：解封
     * 1：封禁
     * 2：踢蹬
     */
    private String operate;

    /**
     * 操作结果消息
     */
    private String message;

    public OperVO() {
    }

    public OperVO(String account, String operate) {
        this.account = account;
        this.operate = operate;
        this.message = getMessageByOperate(operate);
    }

    public OperVO(String account, AccountOperationType operationType) {
        this.account = account;
        this.operate = String.valueOf(operationType.getCode());
        this.message = operationType.getDescription();
    }

    private String getMessageByOperate(String operate) {
        switch (operate) {
            case "0":
                return "解封";
            case "1":
                return "封禁";
            case "2":
                return "踢蹬";
            default:
                return "未知操作";
        }
    }
}
