package com.blog.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class Result {
    private Integer code;
    private String message;
    private Object data;
    private Map<String, Object> extra;

    public Result() {
        this.extra = new HashMap<>();
    }

    public static Result ok() {
        Result r = new Result();
        r.setCode(200);
        return r;
    }

    public static Result ok(Object data) {
        Result r = new Result();
        r.setCode(200);
        r.setData(data);
        return r;
    }

    public static Result fail(String message) {
        Result r = new Result();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    /**
     * 添加额外数据
     *
     * @param key 数据键
     * @param value 数据值
     * @return Result对象本身，支持链式调用
     */
    public Result setExtra(String key, Object value) {
        this.extra.put(key, value);
        return this;
    }

    public Result setMessage(String message) {
        this.message = message;
        return this;
    }

    public Result setData(Object data) {
        this.data = data;
        return this;
    }
}
