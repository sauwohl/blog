package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UserIpRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserIpRecordMapper extends BaseMapper<UserIpRecord> {
    
    /**
     * 查询用户的常用IP记录
     */
    @Select("SELECT * FROM user_ip_record WHERE account = #{account} AND is_common = 1")
    List<UserIpRecord> listCommonIps(@Param("account") String account);
    
    /**
     * 查询IP地址的访问记录
     */
    @Select("SELECT * FROM user_ip_record WHERE account = #{account} AND ip = #{ip}")
    UserIpRecord getRecord(@Param("account") String account, @Param("ip") String ip);
} 