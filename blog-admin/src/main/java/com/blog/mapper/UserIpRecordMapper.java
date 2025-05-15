package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.UserIpRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserIpRecordMapper extends BaseMapper<UserIpRecord> {
    
    @Select("SELECT * FROM user_ip_records WHERE user_id = #{userId} AND is_common = true ORDER BY login_count DESC")
    List<UserIpRecord> findCommonIpsByUserId(Long userId);
    
    @Select("SELECT * FROM user_ip_records WHERE user_id = #{userId} AND ip_address = #{ipAddress}")
    UserIpRecord findByUserIdAndIp(Long userId, String ipAddress);
} 