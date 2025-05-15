package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.UserInfoVO;
import com.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    IPage<UserInfoVO> selectUsersWithRolesPage(Page<?> page);

    /**
     * 分页查询用户列表
     * 按身份和登录时间排序
     */
    @Select("SELECT * FROM users ORDER BY identity DESC, last_login_time DESC")
    IPage<User> selectUsersPage(Page<User> page);
}
