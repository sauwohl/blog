CREATE TABLE `account_abnormal_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `abnormal_type` varchar(50) NOT NULL COMMENT '异常类型',
  `abnormal_detail` text COMMENT '异常详细信息',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `location` varchar(255) DEFAULT NULL COMMENT 'IP地理位置',
  `is_resolved` tinyint(1) DEFAULT '0' COMMENT '是否已解决',
  `resolve_method` varchar(100) DEFAULT NULL COMMENT '解决方式',
  `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
  `operator_id` varchar(50) DEFAULT NULL COMMENT '处理人ID',
  `risk_level` tinyint DEFAULT '1' COMMENT '风险等级：1-低，2-中，3-高',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_abnormal_type` (`abnormal_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号异常记录表'; 