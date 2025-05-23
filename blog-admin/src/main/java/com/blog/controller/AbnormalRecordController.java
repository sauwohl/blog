package com.blog.controller;

import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;
import com.blog.dto.Result;
import com.blog.dto.OperVO;
import com.blog.entity.AccountAbnormalRecord;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.AbnormalRecordService;
import com.blog.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/abnormal-records")
public class AbnormalRecordController {

    @Autowired
    private AbnormalRecordService abnormalRecordService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private ArticleMapper articleMapper;



    /**
     * 分页查询异常记录
     * @param page 当前页码
     * @param perPage 每页数量
     * @param category 异常类型（可选）
     * @param status 处理状态（可选）
     * @param beginDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @return 异常记录列表
     */
    @GetMapping
    public Result listAbnormalRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        return Result.ok(abnormalRecordService.listAbnormalRecords(page, perPage, category, status, beginDate, endDate));
    }

    /**
     * 通过ID查询异常内容详情
     * @param id 异常记录ID
     * @return 异常内容详情
     */
    @GetMapping("/{id}/detail")
    public Result getAbnormalRecordDetail(@PathVariable String id) {
        // 1. 先获取异常记录的基本信息
        AbnormalRecordDTO record = abnormalRecordService.getAbnormalRecord(id);
        if (record == null) {
            return Result.fail("异常记录不存在");
        }

        // 2. 获取原始记录以访问详细信息
        AccountAbnormalRecord originalRecord = abnormalRecordMapper.selectById(id);
        if (originalRecord == null) {
            return Result.fail("异常记录不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("account", record.getAccount());
        data.put("publish_time", record.getPublishTime());
        data.put("description", record.getDescription());

        // 3. 如果是可疑内容，需要包含博客信息
        if (record.getCategory() == 2) {  // SUSPICIOUS_ACTIVITY
            try {
                // 从abnormalDetail中解析文章ID
                ObjectMapper mapper = new ObjectMapper();
                JsonNode detailNode = mapper.readTree(originalRecord.getAbnormalDetail());
                Long articleId = detailNode.get("articleId").asLong();

                // 查询文章信息
                Article article = articleMapper.selectById(articleId);
                if (article != null) {
                    data.put("title", article.getTitle());
                    data.put("content", article.getContent());
                } else {
                    data.put("title", "文章已删除");
                    data.put("content", "文章内容不可用");
                }
            } catch (Exception e) {
                data.put("title", "解析失败");
                data.put("content", "内容解析失败");
            }
        } else {
            // 其他类型的异常，直接返回详细信息
            data.put("detail", originalRecord.getAbnormalDetail());
        }

        return Result.ok()
                .setMessage("获取异常记录成功")
                .setData(data);
    }

    /**
     * 处理异常记录（包括踢蹬操作）
     */
    @PutMapping("/{id}/handle")
    public Result handleAbnormalRecord(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean kickOut) {
        
        // 获取异常记录详情
        AbnormalRecordDTO record = abnormalRecordService.getAbnormalRecord(id);
        if (record == null) {
            return Result.fail("异常记录不存在");
        }
        
        // 如果需要踢蹬，先执行踢蹬操作
        if (kickOut && record.getAccount() != null) {
            OperVO operResult = userService.kickOutUser(record.getAccount());
            if (operResult == null) {
                return Result.fail("踢蹬操作失败");
            }
        }
        
        // 标记异常记录为已处理
        abnormalRecordService.resolveAbnormalRecord(id);
        
        return Result.ok(null).setExtra("redirectUrl", "/user/login");
    }
} 