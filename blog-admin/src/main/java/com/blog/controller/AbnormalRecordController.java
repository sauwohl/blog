package com.blog.controller;

import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;
import com.blog.dto.Result;
import com.blog.dto.OperVO;
import com.blog.service.AbnormalRecordService;
import com.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/abnormal-records")
public class AbnormalRecordController {

    @Autowired
    private AbnormalRecordService abnormalRecordService;
    
    @Autowired
    private UserService userService;

    /**
     * 分页查询异常记录
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