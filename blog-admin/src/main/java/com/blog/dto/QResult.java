package com.blog.dto;

import java.util.List;

public class QResult {
    private List<?> data;  // 当前页的数据
    private long total;    // 总记录数
    private int page;
    private int size;

    public QResult(List<?> data, long total, int page, int size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
