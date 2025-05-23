package com.blog.controller;

import com.blog.dto.OperVO;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.service.EmailService;
import com.blog.service.AbnormalRecordService;
import com.blog.dto.Result;
import com.blog.dto.CreateUserDTO;
import com.blog.utils.AESUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
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

    @Autowired
    private AESUtil aesUtil;

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
     * 支持按账号模糊搜索
     */
    @GetMapping
    public Result listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String account) {
        Map<String, Object> data = userService.listUsersWithPagination(page, size, account);
        return Result.ok()
                .setMessage("获取用户列表成功")
                .setData(data);
    }

    /**
     * 新增账号
     * 系统会生成随机密码，通过邮件发送明文密码给用户
     * 返回AES加密后的密码给前端
     */
    @PostMapping
    public Result createUser(@RequestBody CreateUserDTO createUserDTO) {
        String encryptedPassword = userService.createUser(createUserDTO);
        return Result.ok()
                .setMessage("用户创建成功，账号信息已发送至邮箱")
                .setData(encryptedPassword);
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
     * 根据账号查询用户
     */
    @GetMapping("/account/{account}")
    public Result getUserByAccount(@PathVariable String account) {
        User user = userService.getUserByAccount(account);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        System.out.println(user.getAccount());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", user.getId());
        userData.put("username", user.getUsername());
        userData.put("password", user.getPassword());
        userData.put("phone", user.getPhone());
        userData.put("status", user.getStatus());
        userData.put("createdAt", user.getCreatedAt() != null ? dateFormat.format(user.getCreatedAt()) : null);
        userData.put("updatedAt", user.getUpdatedAt() != null ? dateFormat.format(user.getUpdatedAt()) : null);
        userData.put("account", user.getAccount());
        userData.put("profile", user.getBio());

        return Result.ok()
                .setMessage("获取用户信息成功")
                .setData(userData);
    }

    /**
     * 重置密码
     * 返回AES加密后的新密码
     */
    @PostMapping("/{id}/reset-password")
    public Result resetPassword(@PathVariable("id") Long userId) {
        String encryptedPassword = userService.resetPassword(userId);
        return Result.ok()
                .setMessage("密码重置成功")
                .setData(encryptedPassword);
    }

    /**
     * 账号操作接口
     * 支持踢出、封禁、解封操作
     *
     * @param operVO 操作请求参数
     * @return 操作结果，包含重定向信息
     */
    @PostMapping("/account/operation")
    public ResponseEntity<Result> handleAccountOperation(
            @RequestBody OperVO operVO) {
        
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
