package com.blog.controller;

import com.blog.dto.OperVO;
import com.blog.entity.User;
import com.blog.service.UserService;
import com.blog.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    @Autowired
    private UserService userService;

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
    public Result createUser(@RequestBody User user2) {

        return Result.ok(userService.createUser(user2));
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

    @GetMapping("/account/operation")
    public Result getAccountOperation() {
        String account = "user123";
        int operate = 1; // 封禁

        OperVO response = new OperVO(account, operate);
        return Result.ok(response);
    }

}
