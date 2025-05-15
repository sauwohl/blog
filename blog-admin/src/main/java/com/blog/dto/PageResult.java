package com.blog.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private String totalCount;    // 总记录数
    private String page;          // 当前页码
    private String perPage;       // 每页记录数
    private String message;       // 消息
    private PageData<T> data;     // 数据

    @Data
    public static class PageData<T> {
        private List<T> results;  // 结果列表
    }

    public static <T> PageResult<T> of(List<T> results, long total, int page, int perPage) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotalCount(String.valueOf(total));
        pageResult.setPage(String.valueOf(page));
        pageResult.setPerPage(String.valueOf(perPage));
        
        PageData<T> pageData = new PageData<>();
        pageData.setResults(results);
        pageResult.setData(pageData);
        
        return pageResult;
    }
} 