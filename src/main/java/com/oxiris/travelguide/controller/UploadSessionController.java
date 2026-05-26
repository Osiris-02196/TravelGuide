package com.oxiris.travelguide.controller;

import cn.hutool.json.JSONUtil;
import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.service.StrategyService;
import com.oxiris.travelguide.service.UserService;
import com.oxiris.travelguide.websocket.NotifyWebSocketHandler;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 上传会话控制器。
 * 手机扫码上传图片：PC 端创建会话 → 手机端扫码上传 → 通过 WebSocket 实时同步到 PC。
 */
@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadSessionController {

    private static final String SESSION_KEY_PREFIX = "upload:session:";
    private static final long SESSION_TTL_SECONDS = 1800; // 30分钟

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private StrategyService strategyService;

    @Resource
    private UserService userService;

    @Resource
    private NotifyWebSocketHandler notifyWebSocketHandler;

    /**
     * PC端：创建上传会话，返回 token
     */
    @PostMapping("/session")
    public BaseResponse<Map<String, Object>> createSession(HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 2. 生成唯一 token
        String token = UUID.randomUUID().toString().replace("-", "");
        // 3. 构建会话数据
        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("userId", loginUser.getId());
        sessionData.put("imageUrls", new ArrayList<String>());
        sessionData.put("status", "active");
        // 4. 存入 Redis（带 TTL）
        String key = SESSION_KEY_PREFIX + token;
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(sessionData), SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("创建上传会话: token={}, userId={}", token, loginUser.getId());
        // 5. 返回 token
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        return ResultUtils.success(result);
    }

    /**
     * PC端：轮询获取会话中的图片列表及完成状态
     */
    @GetMapping("/session/{token}/images")
    public BaseResponse<Map<String, Object>> getSessionImages(@PathVariable String token) {
        // 1. 从 Redis 获取会话数据
        String key = SESSION_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(json == null, ErrorCode.NOT_FOUND_ERROR, "上传会话不存在或已过期");
        // 2. 解析并返回图片列表及完成状态
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = JSONUtil.toBean(json, Map.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imageUrls", sessionData.getOrDefault("imageUrls", Collections.emptyList()));
        result.put("completed", "completed".equals(sessionData.get("status")));
        return ResultUtils.success(result);
    }

    /**
     * 手机端：上传单张图片到会话
     */
    @PostMapping("/session/{token}/images")
    public BaseResponse<String> uploadImageToSession(
            @PathVariable String token,
            @RequestParam("file") MultipartFile file) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 1. 获取会话
        String key = SESSION_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(json == null, ErrorCode.NOT_FOUND_ERROR, "上传会话不存在或已过期");
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = JSONUtil.toBean(json, Map.class);
        ThrowUtils.throwIf(!"active".equals(sessionData.get("status")), ErrorCode.OPERATION_ERROR, "上传会话已关闭");
        // 2. 上传图片（复用现有 COS 上传逻辑）
        String imageUrl = strategyService.uploadImage(file);
        ThrowUtils.throwIf(imageUrl == null, ErrorCode.OPERATION_ERROR, "图片上传失败");
        // 3. 更新会话中的图片列表
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) sessionData.getOrDefault("imageUrls", new ArrayList<String>());
        imageUrls.add(imageUrl);
        sessionData.put("imageUrls", imageUrls);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(sessionData), SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        // 4. 通过 WebSocket 通知 PC 端有新图片
        Long userId = Long.valueOf(sessionData.get("userId").toString());
        Map<String, Object> wsMessage = new LinkedHashMap<>();
        wsMessage.put("type", "uploadProgress");
        wsMessage.put("token", token);
        wsMessage.put("imageUrl", imageUrl);
        wsMessage.put("count", imageUrls.size());
        notifyWebSocketHandler.pushMessage(userId, wsMessage);
        log.info("手机端上传图片成功: token={}, imageUrl={}, total={}", token, imageUrl, imageUrls.size());
        return ResultUtils.success(imageUrl);
    }

    /**
     * 手机端：标记会话上传完成
     */
    @PutMapping("/session/{token}/complete")
    public BaseResponse<Void> completeSession(@PathVariable String token) {
        String key = SESSION_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(json == null, ErrorCode.NOT_FOUND_ERROR, "上传会话不存在或已过期");
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = JSONUtil.toBean(json, Map.class);
        sessionData.put("status", "completed");
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(sessionData), SESSION_TTL_SECONDS, TimeUnit.SECONDS);
        // 通过 WebSocket 通知 PC 端上传完成
        Long userId = Long.valueOf(sessionData.get("userId").toString());
        Map<String, Object> wsMessage = new LinkedHashMap<>();
        wsMessage.put("type", "uploadComplete");
        wsMessage.put("token", token);
        notifyWebSocketHandler.pushMessage(userId, wsMessage);
        log.info("上传会话已完成: token={}", token);
        return ResultUtils.success(null);
    }

    /**
     * PC端：取消/删除上传会话
     */
    @DeleteMapping("/session/{token}")
    public BaseResponse<Void> deleteSession(@PathVariable String token, HttpServletRequest request) {
        // 验证登录
        User loginUser = userService.getLoginUser(request);
        String key = SESSION_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().get(key);
        ThrowUtils.throwIf(json == null, ErrorCode.NOT_FOUND_ERROR, "上传会话不存在或已过期");
        @SuppressWarnings("unchecked")
        Map<String, Object> sessionData = JSONUtil.toBean(json, Map.class);
        Long ownerId = Long.valueOf(sessionData.get("userId").toString());
        ThrowUtils.throwIf(!loginUser.getId().equals(ownerId), ErrorCode.NO_AUTH_ERROR, "无权操作此会话");
        // 删除 Redis key
        stringRedisTemplate.delete(key);
        log.info("删除上传会话: token={}, userId={}", token, loginUser.getId());
        return ResultUtils.success(null);
    }
}
