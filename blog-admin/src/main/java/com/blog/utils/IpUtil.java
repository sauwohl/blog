package com.blog.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpUtil {
    
    private static final String UNKNOWN = "unknown";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String SEPARATOR = ",";
    
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress;
        try {
            // 获取代理前的真实IP
            ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
                ipAddress = request.getRemoteAddr();
                if (LOCALHOST.equals(ipAddress)) {
                    // 根据网卡取本机配置的IP
                    try {
                        InetAddress inet = InetAddress.getLocalHost();
                        ipAddress = inet.getHostAddress();
                    } catch (UnknownHostException e) {
                        // 忽略异常
                    }
                }
            }
            // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
            if (ipAddress != null && ipAddress.contains(SEPARATOR)) {
                ipAddress = ipAddress.split(",")[0];
            }
        } catch (Exception e) {
            ipAddress = "";
        }
        return ipAddress;
    }
    
    /**
     * 判断IP是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        try {
            byte[] addr = textToNumericFormatV4(ip);
            if (addr == null) {
                return false;
            }
            
            // 10.x.x.x/8
            final byte b0 = addr[0];
            if (b0 == 10) {
                return true;
            }
            
            // 172.16.x.x/12
            final byte b1 = addr[1];
            if (b0 == (byte) 172 && (b1 >= 16 && b1 <= 31)) {
                return true;
            }
            
            // 192.168.x.x/16
            if (b0 == (byte) 192 && b1 == (byte) 168) {
                return true;
            }
            
            // 127.0.0.1
            if (b0 == (byte) 127) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
    
    /**
     * 将IPv4地址转换成字节数组
     */
    private static byte[] textToNumericFormatV4(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        try {
            for (int i = 0; i < elements.length; i++) {
                bytes[i] = (byte) Integer.parseInt(elements[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return bytes;
    }
    
    /**
     * 获取IP地址的大致位置（基于IP地址段判断）
     */
    public static String getIpLocation(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "未知位置";
        }
        
        // 判断是否为内网IP
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        
        try {
            byte[] addr = textToNumericFormatV4(ip);
            if (addr == null) {
                return "未知位置";
            }
            
            // 简单的IP地址段判断，实际项目中可以使用IP地址库
            if (addr[0] == (byte)202 && addr[1] >= (byte)96 && addr[1] <= (byte)111) {
                return "北京";
            }
            if (addr[0] == (byte)210 && addr[1] >= (byte)21 && addr[1] <= (byte)47) {
                return "上海";
            }
            if (addr[0] == (byte)121 && addr[1] >= (byte)0 && addr[1] <= (byte)63) {
                return "广州";
            }
            
            return "其他地区";
        } catch (Exception e) {
            return "未知位置";
        }
    }
} 