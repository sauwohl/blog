CREATE TABLE `user_ip_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account` varchar(50) NOT NULL COMMENT '账号',
  `ip` varchar(50) NOT NULL COMMENT 'IP地址',
  `count` int(11) DEFAULT 1 COMMENT '访问次数',
  `is_common` tinyint(1) DEFAULT 0 COMMENT '是否常用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_account` (`account`),
  KEY `idx_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP记录表'; 