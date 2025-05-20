package com.blog.controller;

import com.blog.dto.OperVO;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.service.EmailService;
import com.blog.service.AbnormalRecordService;
import com.blog.dto.Result;
import com.blog.dto.CreateUserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private AbnormalRecordService abnormalRecordService;

    /**
     * 查询可疑内容
     * 展示被标记为可疑活动的文章内容
     */
    @GetMapping("/suspicious-contents")
    public Result listSuspiciousContents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(abnormalRecordService.listSuspiciousContents(page, size));
    }

    /**
     * 分页查询所有允许登录的账号列表，按登录状态排序
     */

    @GetMapping
    public Result listUsers(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.listUsersWithPagination(page, size));
    }


    /**
     * 新增账号
     */
    @PostMapping
    public Result createUser(@RequestBody CreateUserDTO createUserDTO) {
        return Result.ok(userService.createUser(createUserDTO));
    }

    /**
     * 查看指定用户
     */
    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        return Result.ok(user);
    }
    /**
     * 重置密码
     */
    @PostMapping("/{id}/reset-password")
    public Result resetPassword(@PathVariable("id") Long userId) {
        return Result.ok(userService.resetPassword(userId));
    }

    /**
     * 账号操作接口
     * 支持踢出、封禁、解封操作
     *
     * @param operVO 操作请求参数
     * @param authorization 认证token
     * @return 操作结果，包含重定向信息
     */
    @PostMapping("/account/operation")
    public ResponseEntity<Result> handleAccountOperation(
            @RequestBody OperVO operVO,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        
        OperVO operResult;
        String redirectUrl = null;
        
        switch (operVO.getOperate()) {
            case "2": // 踢蹬
                operResult = userService.kickOutUser(operVO.getAccount());
                redirectUrl = "/user/login";
                break;
            case "1": // 封禁
                operResult = userService.banUser(operVO.getAccount());
                redirectUrl = "/user/login";
                break;
            case "0": // 解封
                operResult = userService.unbanUser(operVO.getAccount());
                break;
            default:
                return ResponseEntity.badRequest()
                    .body(Result.fail("无效的操作类型"));
        }

        Result result = Result.ok(operResult);
        if (redirectUrl != null) {
            result.setExtra("redirectUrl", redirectUrl);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查用户是否被封禁
     */
    @GetMapping("/{account}/status")
    public Result checkUserStatus(@PathVariable String account) {
        boolean isBanned = userService.isUserBanned(account);
        return Result.ok(isBanned ? "账号已被封禁" : "账号状态正常");
    }

}
