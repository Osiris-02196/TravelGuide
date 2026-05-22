package com.oxiris.travelguide.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Enumeration;

/**
 * 高德地图 API 代理控制器
 * 前端 JS API 配置 serviceHost 后，所有请求经过此代理转发到高德服务器，
 * securityJsCode 只保存在后端，不暴露给前端。
 */
@RestController
@RequestMapping("/amap-proxy")
public class AmapProxyController {

    @Value("${amap.security-js-code}")
    private String securityJsCode;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 代理 GET 请求到高德 API
     * 高德 JS API 的 serviceHost 机制会发 GET 请求到本代理
     */
    @GetMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request) {
        // 1. 提取请求路径中 /amap-proxy/ 后面的部分
        String requestPath = request.getRequestURI();
        String proxyPath = requestPath.substring("/api/amap-proxy".length());

        // 2. 组装高德真实 URL
        String url = "https://restapi.amap.com" + proxyPath;

        // 3. 获取所有查询参数并注入 securityJsCode
        StringBuilder queryString = new StringBuilder();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            if (queryString.length() > 0) queryString.append("&");
            queryString.append(name).append("=").append(value);
        }
        if (queryString.length() > 0) queryString.append("&");
        queryString.append("jscode=").append(securityJsCode);

        // 4. 转发请求
        ResponseEntity<String> response = restTemplate.getForEntity(
                URI.create(url + "?" + queryString),
                String.class
        );
        return ResponseEntity.status(response.getStatusCode())
                .body(response.getBody());
    }
}

