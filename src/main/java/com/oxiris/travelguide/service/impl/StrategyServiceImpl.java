package com.oxiris.travelguide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.BusinessException;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.manager.CosManager;
import com.oxiris.travelguide.mapper.StrategyCollectMapper;
import com.oxiris.travelguide.mapper.StrategyLikeMapper;
import com.oxiris.travelguide.mapper.StrategyMapper;
import com.oxiris.travelguide.model.dto.strategy.StrategyAddRequest;
import com.oxiris.travelguide.model.dto.strategy.StrategyQueryRequest;
import com.oxiris.travelguide.model.entity.Location;
import com.oxiris.travelguide.model.entity.Strategy;
import com.oxiris.travelguide.model.entity.StrategyCollect;
import com.oxiris.travelguide.model.entity.StrategyLike;
import com.oxiris.travelguide.model.entity.User;
import com.oxiris.travelguide.model.enums.strategy.OfficialStatusEnum;
import com.oxiris.travelguide.model.enums.strategy.StrategyStatusEnum;
import com.oxiris.travelguide.model.vo.LocationVO;
import com.oxiris.travelguide.model.vo.StrategyCollectVO;
import com.oxiris.travelguide.model.vo.StrategyVO;
import com.oxiris.travelguide.service.LocationService;
import com.oxiris.travelguide.service.NotifyService;
import com.oxiris.travelguide.service.StrategyService;
import com.oxiris.travelguide.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 攻略 服务层实现。
 */
@Service
@Slf4j
public class StrategyServiceImpl extends ServiceImpl<StrategyMapper, Strategy> implements StrategyService {

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    private LocationService locationService;

    @Resource
    private StrategyCollectMapper strategyCollectMapper;

    @Resource
    private StrategyLikeMapper strategyLikeMapper;

    @Resource
    private NotifyService notifyService;

    @Override
    public String uploadImage(MultipartFile file) {
        // 1. 校验文件
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 2. 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String key = "strategy/" + UUID.randomUUID().toString().replace("-", "") + suffix;
        // 3. 将MultipartFile转为临时File
        File tempFile = null;
        String url = null;
        try {
            tempFile = File.createTempFile("upload_", suffix);
            file.transferTo(tempFile);
            // 4. 尝试上传到COS，如果失败则使用本地上传
            try {
                url = cosManager.uploadFile(key, tempFile);
            } catch (Exception e) {
                log.warn("COS上传失败，将使用本地上传: {}", e.getMessage());
            }
            if (url == null) {
                // 本地上传到 static/uploads 目录
                try {
                    String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                    File destDir = new File(uploadDir);
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }
                    File destFile = new File(uploadDir + key.replace("/", "_"));
                    Files.copy(tempFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    url = "/uploads/" + key.replace("/", "_");
                    log.info("本地上传成功: {}", url);
                } catch (IOException e) {
                    log.error("本地上传失败: {}", e.getMessage());
                    // 图片上传失败不阻止攻略发布，url保持null
                }
            }
        } catch (IOException e) {
            log.error("创建临时文件失败: {}", e.getMessage());
        } finally {
            // 5. 删除临时文件
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        ThrowUtils.throwIf(url == null, ErrorCode.SYSTEM_ERROR, "图片上传失败");
        return url;
    }

    @Override
    public Long addStrategy(StrategyAddRequest strategyAddRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyAddRequest == null, ErrorCode.PARAMS_ERROR);
        String strategyTitle = strategyAddRequest.getStrategyTitle();
        String strategyContent = strategyAddRequest.getStrategyContent();
        ThrowUtils.throwIf(StrUtil.isBlank(strategyTitle), ErrorCode.PARAMS_ERROR, "攻略标题不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(strategyContent), ErrorCode.PARAMS_ERROR, "攻略内容不能为空");
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 构建攻略实体
        Strategy strategy = new Strategy();
        strategy.setUserId(loginUser.getId());
        strategy.setStrategyTitle(strategyTitle);
        strategy.setStrategyContent(strategyContent);
        // 4. 处理图片URL列表 -> JSON字符串
        if (CollUtil.isNotEmpty(strategyAddRequest.getImageUrls())) {
            strategy.setImageUrls(JSONUtil.toJsonStr(strategyAddRequest.getImageUrls()));
        }
        // 5. 处理标签列表 -> 逗号分隔字符串
        if (CollUtil.isNotEmpty(strategyAddRequest.getStrategyTags())) {
            strategy.setStrategyTags(String.join(",", strategyAddRequest.getStrategyTags()));
        }
        // 6. 处理地点列表 -> JSON字符串
        if (CollUtil.isNotEmpty(strategyAddRequest.getLocations())) {
            strategy.setLocations(JSONUtil.toJsonStr(strategyAddRequest.getLocations()));
        }
        // 6. 处理路线规划数据 -> JSON字符串
        if (StrUtil.isNotBlank(strategyAddRequest.getRouteData())) {
            strategy.setRouteData(strategyAddRequest.getRouteData());
        }
        // 7. 设置默认值
        strategy.setStrategyStatus(StrategyStatusEnum.PENDING.getValue());
        strategy.setClickCount(0);
        strategy.setLikeCount(0);
        strategy.setCollectCount(0);
        strategy.setCommentCount(0);
        strategy.setHotScore(0.0);
        strategy.setIsOfficial(OfficialStatusEnum.NORMAL.getValue());
        // 8. 保存攻略
        boolean saveResult = this.save(strategy);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "攻略上传失败");
        return strategy.getId();
    }

    @Override
    public StrategyVO getStrategyVO(Strategy strategy) {
        if (strategy == null) {
            return null;
        }
        StrategyVO strategyVO = new StrategyVO();
        BeanUtil.copyProperties(strategy, strategyVO);
        return strategyVO;
    }

    @Override
    public List<StrategyVO> getStrategyVOList(List<Strategy> strategyList) {
        if (CollUtil.isEmpty(strategyList)) {
            return new ArrayList<>();
        }
        return strategyList.stream().map(this::getStrategyVO).collect(Collectors.toList());
    }

    /**
     * 列表用的VO转换：内容截断前20字、图片只取前3张
     */
    private StrategyVO getStrategyVOForList(Strategy strategy) {
        if (strategy == null) {
            return null;
        }
        StrategyVO strategyVO = new StrategyVO();
        BeanUtil.copyProperties(strategy, strategyVO);
        // 确保点赞数、收藏数、评论数被显式设置
        strategyVO.setLikeCount(strategy.getLikeCount());
        strategyVO.setCollectCount(strategy.getCollectCount());
        strategyVO.setCommentCount(strategy.getCommentCount());
        // 内容截断前20字
        String content = strategy.getStrategyContent();
        if (StrUtil.isNotBlank(content) && content.length() > 20) {
            strategyVO.setStrategyContent(content.substring(0, 20) + "...");
        }
        // 图片只取前3张
        String imageUrlsJson = strategy.getImageUrls();
        if (StrUtil.isNotBlank(imageUrlsJson)) {
            List<String> imageUrls = JSONUtil.toList(imageUrlsJson, String.class);
            if (imageUrls.size() > 3) {
                imageUrls = new ArrayList<>(imageUrls.subList(0, 3));
            }
            strategyVO.setImageUrls(JSONUtil.toJsonStr(imageUrls));
        }
        // ===== 注入用户信息 =====
        if (strategy.getUserId() != null) {
            User user = userService.getById(strategy.getUserId());
            if (user != null) {
                strategyVO.setUserName(user.getUserName());
                strategyVO.setUserAvatar(user.getUserAvatar());
            }
        }
        // ===== 将地点ID列表转换为地点名称字符串 =====
        String locationsJson = strategy.getLocations();
        if (StrUtil.isNotBlank(locationsJson)) {
            try {
                List<String> locationIds = JSONUtil.toList(locationsJson, String.class);
                if (CollUtil.isNotEmpty(locationIds)) {
                    List<Long> ids = locationIds.stream()
                            .map(idStr -> {
                                try { return Long.valueOf(idStr); } catch (NumberFormatException e) { return null; }
                            })
                            .filter(id -> id != null)
                            .collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(ids)) {
                        List<Location> locations = locationService.listByIds(ids);
                        if (CollUtil.isNotEmpty(locations)) {
                            String locationNameStr = locations.stream()
                                    .map(Location::getLocationName)
                                    .filter(StrUtil::isNotBlank)
                                    .collect(Collectors.joining(" · "));
                            strategyVO.setLocations(locationNameStr);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析locations失败: {}", e.getMessage());
            }
        }
        return strategyVO;
    }

    /**
     * 列表用的批量VO转换
     */
    private List<StrategyVO> getStrategyVOListForList(List<Strategy> strategyList) {
        if (CollUtil.isEmpty(strategyList)) {
            return new ArrayList<>();
        }
        return strategyList.stream().map(this::getStrategyVOForList).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(StrategyQueryRequest strategyQueryRequest) {
        if (strategyQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = strategyQueryRequest.getId();
        String strategyTitle = strategyQueryRequest.getStrategyTitle();
        Integer strategyStatus = strategyQueryRequest.getStrategyStatus();
        Long userId = strategyQueryRequest.getUserId();
        String strategyTags = strategyQueryRequest.getStrategyTags();
        String location = strategyQueryRequest.getLocation();
        Integer isOfficial = strategyQueryRequest.getIsOfficial();
        String sortField = strategyQueryRequest.getSortField();
        String sortOrder = strategyQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create();
        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (userId != null) {
            queryWrapper.eq("userId", userId);
        }
        if (strategyStatus != null) {
            queryWrapper.eq("strategyStatus", strategyStatus);
        }
        if (isOfficial != null) {
            queryWrapper.eq("isOfficial", isOfficial);
        }
        if (StrUtil.isNotBlank(strategyTitle)) {
            queryWrapper.like("strategyTitle", strategyTitle);
        }
        if (StrUtil.isNotBlank(strategyTags)) {
            queryWrapper.like("strategyTags", strategyTags);
        }
        if (StrUtil.isNotBlank(location)) {
            queryWrapper.like("locations", location);
        }
        // 关键词搜索：同时匹配攻略标题和地点名称
        String keyword = strategyQueryRequest.getKeyword();
        if (StrUtil.isNotBlank(keyword)) {
            // 查找匹配的地点名称，获取地点ID
            List<LocationVO> matchedLocations = locationService.searchCities(keyword);
            if (CollUtil.isNotEmpty(matchedLocations)) {
                // 构建 title LIKE keyword OR locations 包含地点ID
                QueryColumn titleColumn = new QueryColumn("strategyTitle");
                QueryCondition kwCondition = titleColumn.like(keyword);
                for (LocationVO loc : matchedLocations) {
                    QueryColumn locColumn = new QueryColumn("locations");
                    kwCondition = kwCondition.or(locColumn.like("\"" + loc.getId() + "\""));
                }
                queryWrapper.and(kwCondition);
            } else {
                // 没有匹配的地点，仅按标题搜索
                queryWrapper.like("strategyTitle", keyword);
            }
        }
        if (StrUtil.isNotBlank(sortField)) {
            boolean isAsc = "ascend".equals(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    @Override
    public Double calculateHotScore(Integer clickCount, Integer likeCount, Integer collectCount, Integer commentCount) {
        // 热度公式: hotScore = clickCount*0.1 + likeCount*0.2 + collectCount*0.3 + commentCount*0.4
        double c = clickCount != null ? clickCount : 0;
        double l = likeCount != null ? likeCount : 0;
        double col = collectCount != null ? collectCount : 0;
        double com = commentCount != null ? commentCount : 0;
        return c * 0.1 + l * 0.2 + col * 0.3 + com * 0.4;
    }

    @Override
    public StrategyVO getStrategyDetail(Long id) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 3. 点击量+1
        strategy.setClickCount(strategy.getClickCount() + 1);
        // 4. 更新热度分数
        Double hotScore = calculateHotScore(strategy.getClickCount(), strategy.getLikeCount(),
                strategy.getCollectCount(), strategy.getCommentCount());
        strategy.setHotScore(hotScore);
        this.updateById(strategy);
        // 5. 转换VO并注入用户信息
        StrategyVO strategyVO = getStrategyVO(strategy);
        if (strategy.getUserId() != null) {
            User user = userService.getById(strategy.getUserId());
            if (user != null) {
                strategyVO.setUserName(user.getUserName());
                strategyVO.setUserAvatar(user.getUserAvatar());
            }
        }
        return strategyVO;
    }

    @Override
    public Page<StrategyVO> listPassedStrategies(StrategyQueryRequest strategyQueryRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 设置只查审核通过的
        strategyQueryRequest.setStrategyStatus(StrategyStatusEnum.APPROVED.getValue());
        // 3. 分页查询
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize),
                this.getQueryWrapper(strategyQueryRequest));
        // 4. 转换VO
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Page<StrategyVO> listHotStrategies(StrategyQueryRequest strategyQueryRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 设置只查审核通过的
        strategyQueryRequest.setStrategyStatus(StrategyStatusEnum.APPROVED.getValue());
        // 3. 设置按热度排序
        strategyQueryRequest.setSortField("hotScore");
        strategyQueryRequest.setSortOrder("descend");
        // 4. 分页查询
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize),
                this.getQueryWrapper(strategyQueryRequest));
        // 5. 转换VO
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Page<StrategyVO> listMyStrategies(StrategyQueryRequest strategyQueryRequest, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 设置查询当前用户的攻略
        strategyQueryRequest.setUserId(loginUser.getId());
        // 4. 设置时间排序
        strategyQueryRequest.setSortField("createTime");
        strategyQueryRequest.setSortOrder("descend");
        // 5. 分页查询
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize),
                this.getQueryWrapper(strategyQueryRequest));
        // 6. 转换VO
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Boolean deleteStrategy(Long id, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 4. 校验：只能删除自己的攻略
        ThrowUtils.throwIf(!strategy.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "只能删除自己的攻略");
        // 5. 校验：只能删除审核通过的攻略
        ThrowUtils.throwIf(!StrategyStatusEnum.APPROVED.getValue().equals(strategy.getStrategyStatus()),
                ErrorCode.OPERATION_ERROR, "只能删除审核通过的攻略");
        // 6. 逻辑删除
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除攻略失败");
        return true;
    }

    @Override
    public Boolean adminDeleteStrategy(Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 校验登录（@AuthCheck 已校验管理员角色）
        userService.getLoginUser(request);
        // 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 逻辑删除（管理员可删除任意状态的攻略）
        boolean result = this.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除攻略失败");
        return true;
    }

    @Override
    public Page<StrategyVO> listPendingStrategies(StrategyQueryRequest strategyQueryRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        strategyQueryRequest.setStrategyStatus(StrategyStatusEnum.PENDING.getValue());
        // 2. 设置时间排序
        strategyQueryRequest.setSortField("createTime");
        strategyQueryRequest.setSortOrder("descend");
        // 4. 分页查询
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize),
                this.getQueryWrapper(strategyQueryRequest));
        // 5. 转换VO
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Boolean auditStrategy(Long id, Integer status) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(status == null || (!StrategyStatusEnum.APPROVED.getValue().equals(status) && !StrategyStatusEnum.REJECTED.getValue().equals(status)),
                ErrorCode.PARAMS_ERROR, "审核状态必须为1(通过)或2(拒绝)");
        // 2. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 3. 校验当前状态必须为待审核
        ThrowUtils.throwIf(!StrategyStatusEnum.PENDING.getValue().equals(strategy.getStrategyStatus()),
                ErrorCode.OPERATION_ERROR, "只能审核待审核状态的攻略");
        // 4. 更新审核状态
        strategy.setStrategyStatus(status);
        boolean result = this.updateById(strategy);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核攻略失败");
        return true;
    }

    @Override
    public Boolean setOfficial(Long id, Integer isOfficial) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(isOfficial == null || (!OfficialStatusEnum.NORMAL.getValue().equals(isOfficial) && !OfficialStatusEnum.OFFICIAL.getValue().equals(isOfficial)),
                ErrorCode.PARAMS_ERROR, "官方推荐状态必须为0(否)或1(是)");
        // 2. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 3. 只有审核通过的攻略才能设为官方推荐
        ThrowUtils.throwIf(OfficialStatusEnum.OFFICIAL.getValue().equals(isOfficial) && !StrategyStatusEnum.APPROVED.getValue().equals(strategy.getStrategyStatus()),
                ErrorCode.OPERATION_ERROR, "只有审核通过的攻略才能设为官方推荐");
        // 4. 更新官方推荐状态
        strategy.setIsOfficial(isOfficial);
        boolean result = this.updateById(strategy);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "设置官方推荐失败");
        return true;
    }

    @Override
    public Page<StrategyVO> listStrategiesByLocation(StrategyQueryRequest strategyQueryRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        String location = strategyQueryRequest.getLocation();
        ThrowUtils.throwIf(StrUtil.isBlank(location), ErrorCode.PARAMS_ERROR, "地点不能为空");
        // 2. 设置只查审核通过的
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Integer isOfficial = strategyQueryRequest.getIsOfficial();
        String sortField = strategyQueryRequest.getSortField();
        // 3. 构建自定义查询：官方推荐置顶；locations 存的是地点 ID 的 JSON，需按 ID 子串匹配
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("strategyStatus", StrategyStatusEnum.APPROVED.getValue())
                .like("locations", location);
        if (isOfficial != null) {
            queryWrapper.eq("isOfficial", isOfficial);
        }
        queryWrapper.orderBy("isOfficial", false);
        if (StrUtil.isNotBlank(sortField) && "hotScore".equals(sortField)) {
            queryWrapper.orderBy("hotScore", false);
        } else {
            queryWrapper.orderBy("createTime", false);
        }
        // 4. 分页查询
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize), queryWrapper);
        // 5. 转换VO
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Boolean likeStrategy(Long id, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 4. 检查是否已点赞
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("strategyId", id);
        long likeCount = strategyLikeMapper.selectCountByQuery(queryWrapper);
        if (likeCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复点赞");
        }
        // 5. 插入点赞记录
        StrategyLike like = new StrategyLike();
        like.setUserId(loginUser.getId());
        like.setStrategyId(id);
        boolean saveResult = strategyLikeMapper.insert(like, true) > 0;
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "点赞失败");
        // 6. 更新攻略点赞数
        strategy.setLikeCount(strategy.getLikeCount() != null ? strategy.getLikeCount() + 1 : 1);
        // 7. 更新热度分数
        Double hotScore = calculateHotScore(strategy.getClickCount(), strategy.getLikeCount(),
                strategy.getCollectCount(), strategy.getCommentCount());
        strategy.setHotScore(hotScore);
        boolean updateResult = this.updateById(strategy);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新点赞数失败");
        // 8. 发送通知给攻略作者
        if (!strategy.getUserId().equals(loginUser.getId())) {
            try {
                notifyService.createAndPushNotify(strategy.getUserId(), loginUser.getId(),
                        "like", 1, id);
            } catch (Exception e) {
                log.warn("发送点赞通知失败: {}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    public Boolean collectStrategy(Long id, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 4. 判断是否已收藏
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("strategyId", id);
        long count = strategyCollectMapper.selectCountByQuery(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "请勿重复收藏");
        // 5. 插入收藏记录
        StrategyCollect collect = new StrategyCollect();
        collect.setUserId(loginUser.getId());
        collect.setStrategyId(id);
        boolean saveResult = strategyCollectMapper.insert(collect, true) > 0;
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "收藏失败");
        // 6. 更新攻略收藏数
        strategy.setCollectCount(strategy.getCollectCount() + 1);
        // 7. 更新热度分数
        Double hotScore = calculateHotScore(strategy.getClickCount(), strategy.getLikeCount(),
                strategy.getCollectCount(), strategy.getCommentCount());
        strategy.setHotScore(hotScore);
        boolean updateResult = this.updateById(strategy);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新收藏数失败");
        // 8. 发送通知给攻略作者
        if (!strategy.getUserId().equals(loginUser.getId())) {
            try {
                notifyService.createAndPushNotify(strategy.getUserId(), loginUser.getId(),
                        "collect", 1, id);
            } catch (Exception e) {
                log.warn("发送收藏通知失败: {}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    public Boolean uncollectStrategy(Long id, HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 查询攻略
        Strategy strategy = this.getById(id);
        ThrowUtils.throwIf(strategy == null, ErrorCode.NOT_FOUND_ERROR, "攻略不存在");
        // 4. 查找收藏记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .eq("strategyId", id);
        StrategyCollect collect = strategyCollectMapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(collect == null, ErrorCode.OPERATION_ERROR, "尚未收藏该攻略");
        // 5. 删除收藏记录
        boolean deleteResult = strategyCollectMapper.deleteById(collect.getId()) > 0;
        ThrowUtils.throwIf(!deleteResult, ErrorCode.OPERATION_ERROR, "取消收藏失败");
        // 6. 更新攻略收藏数（减1，最小为0）
        int newCollectCount = Math.max(strategy.getCollectCount() - 1, 0);
        strategy.setCollectCount(newCollectCount);
        // 7. 更新热度分数
        Double hotScore = calculateHotScore(strategy.getClickCount(), strategy.getLikeCount(),
                newCollectCount, strategy.getCommentCount());
        strategy.setHotScore(hotScore);
        boolean updateResult = this.updateById(strategy);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新收藏数失败");
        return true;
    }

    @Override
    public Page<StrategyVO> listUserStrategies(Long userId, StrategyQueryRequest strategyQueryRequest) {
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        strategyQueryRequest.setStrategyStatus(StrategyStatusEnum.APPROVED.getValue());
        strategyQueryRequest.setUserId(userId);
        strategyQueryRequest.setSortField("createTime");
        strategyQueryRequest.setSortOrder("descend");
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<Strategy> strategyPage = this.page(Page.of(pageNum, pageSize),
                this.getQueryWrapper(strategyQueryRequest));
        Page<StrategyVO> strategyVOPage = new Page<>(pageNum, pageSize, strategyPage.getTotalRow());
        List<StrategyVO> strategyVOList = this.getStrategyVOListForList(strategyPage.getRecords());
        strategyVOPage.setRecords(strategyVOList);
        return strategyVOPage;
    }

    @Override
    public Page<StrategyCollectVO> listUserCollectStrategies(StrategyQueryRequest strategyQueryRequest,
                                                             HttpServletRequest request) {
        // 1. 校验参数
        ThrowUtils.throwIf(strategyQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 3. 分页查询收藏记录（按收藏时间倒序）
        long pageNum = strategyQueryRequest.getPageNum();
        long pageSize = strategyQueryRequest.getPageSize();
        Page<StrategyCollect> collectPage = strategyCollectMapper.paginate(
                Page.of(pageNum, pageSize),
                QueryWrapper.create()
                        .eq("userId", loginUser.getId())
                        .orderBy("createTime", false)
        );
        List<StrategyCollect> collectList = collectPage.getRecords();
        // 4. 转换为VO（包含攻略摘要信息）
        List<StrategyCollectVO> voList = new ArrayList<>();
        if (CollUtil.isNotEmpty(collectList)) {
            // 批量查询攻略
            List<Long> strategyIds = collectList.stream()
                    .map(StrategyCollect::getStrategyId)
                    .collect(Collectors.toList());
            List<Strategy> strategyList = this.listByIds(strategyIds);
            // 构建 strategyId -> Strategy 映射
            java.util.Map<Long, Strategy> strategyMap = strategyList.stream()
                    .collect(Collectors.toMap(Strategy::getId, s -> s));
            for (StrategyCollect collect : collectList) {
                StrategyCollectVO vo = new StrategyCollectVO();
                vo.setId(collect.getId());
                vo.setStrategyId(collect.getStrategyId());
                vo.setCreateTime(collect.getCreateTime());
                Strategy strategy = strategyMap.get(collect.getStrategyId());
                if (strategy != null) {
                    vo.setStrategyTitle(strategy.getStrategyTitle());
                    // 取第一张图片作为封面
                    String imageUrlsJson = strategy.getImageUrls();
                    if (StrUtil.isNotBlank(imageUrlsJson)) {
                        List<String> imageUrls = JSONUtil.toList(imageUrlsJson, String.class);
                        if (CollUtil.isNotEmpty(imageUrls)) {
                            vo.setCoverImage(imageUrls.get(0));
                        }
                        vo.setImageUrls(imageUrlsJson);
                    }
                    vo.setStrategyContent(strategy.getStrategyContent());
                    vo.setLocations(strategy.getLocations());
                    vo.setStrategyTags(strategy.getStrategyTags());
                    // 用户信息
                    if (strategy.getUserId() != null) {
                        User user = userService.getById(strategy.getUserId());
                        if (user != null) {
                            vo.setUserName(user.getUserName());
                            vo.setUserAvatar(user.getUserAvatar());
                        }
                    }
                    vo.setCollectCount(strategy.getCollectCount());
                    vo.setLikeCount(strategy.getLikeCount());
                }
                voList.add(vo);
            }
        }
        // 5. 构建分页结果
        Page<StrategyCollectVO> voPage = new Page<>(pageNum, pageSize, collectPage.getTotalRow());
        voPage.setRecords(voList);
        return voPage;
    }
}