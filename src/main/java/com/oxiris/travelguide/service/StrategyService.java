package com.oxiris.travelguide.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.dto.strategy.StrategyAddRequest;
import com.oxiris.travelguide.model.dto.strategy.StrategyQueryRequest;
import com.oxiris.travelguide.model.entity.Strategy;
import com.oxiris.travelguide.model.vo.StrategyCollectVO;
import com.oxiris.travelguide.model.vo.StrategyVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 攻略 服务层。
 *
 */
public interface StrategyService extends IService<Strategy> {

    /**
     * 上传图片到COS
     *
     * @param file 图片文件
     * @return 图片访问URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 用户上传攻略
     *
     * @param strategyAddRequest 攻略上传请求
     * @param request            HTTP请求
     * @return 攻略ID
     */
    Long addStrategy(StrategyAddRequest strategyAddRequest, HttpServletRequest request);

    /**
     * 获取单个攻略VO
     *
     * @param strategy 攻略实体
     * @return 攻略VO
     */
    StrategyVO getStrategyVO(Strategy strategy);

    /**
     * 获取攻略VO列表
     *
     * @param strategyList 攻略实体列表
     * @return 攻略VO列表
     */
    List<StrategyVO> getStrategyVOList(List<Strategy> strategyList);

    /**
     * 将查询请求转为QueryWrapper
     *
     * @param strategyQueryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper getQueryWrapper(StrategyQueryRequest strategyQueryRequest);

    /**
     * 计算热度分数
     *
     * @param clickCount   点击量
     * @param likeCount    点赞量
     * @param collectCount 收藏量
     * @param commentCount 评论数
     * @return 热度分数
     */
    Double calculateHotScore(Integer clickCount, Integer likeCount, Integer collectCount, Integer commentCount);

    /**
     * 获取攻略详情（点击量+1）
     *
     * @param id 攻略ID
     * @return 攻略VO
     */
    StrategyVO getStrategyDetail(Long id);

    /**
     * 分页查询主页审核通过的最新攻略列表（时间排序）
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    Page<StrategyVO> listPassedStrategies(StrategyQueryRequest strategyQueryRequest);

    /**
     * 分页查询审核通过的热门推荐列表（热度排序）
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    Page<StrategyVO> listHotStrategies(StrategyQueryRequest strategyQueryRequest);

    /**
     * 用户分页查询自己上传的攻略（按状态和时间排序）
     *
     * @param strategyQueryRequest 查询请求
     * @param request              HTTP请求
     * @return 分页攻略VO
     */
    Page<StrategyVO> listMyStrategies(StrategyQueryRequest strategyQueryRequest, HttpServletRequest request);

    /**
     * 用户删除自己上传通过的攻略（逻辑删除）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean deleteStrategy(Long id, HttpServletRequest request);

    /**
     * 管理员删除任意攻略（逻辑删除，无需归属校验）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean adminDeleteStrategy(Long id, HttpServletRequest request);

    /**
     * 管理员分页查询待审核的攻略列表
     *
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    Page<StrategyVO> listPendingStrategies(StrategyQueryRequest strategyQueryRequest);

    /**
     * 管理员修改攻略审核状态
     *
     * @param id     攻略ID
     * @param status 审核状态（1-通过，2-拒绝）
     * @return 是否成功
     */
    Boolean auditStrategy(Long id, Integer status);

    /**
     * 管理员设置/取消官方推荐
     *
     * @param id         攻略ID
     * @param isOfficial 是否官方推荐（0-否 1-是）
     * @return 是否成功
     */
    Boolean setOfficial(Long id, Integer isOfficial);

    /**
     * 分页查询指定城市审核通过的攻略（官方推荐置顶）
     *
     * @param strategyQueryRequest 查询请求（需包含location字段）
     * @return 分页攻略VO
     */
    Page<StrategyVO> listStrategiesByLocation(StrategyQueryRequest strategyQueryRequest);

    /**
     * 点赞攻略（先判断是否已点赞，未点赞则插入记录并更新点赞数）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean likeStrategy(Long id, HttpServletRequest request);

    /**
     * 收藏攻略（先判断是否已收藏，未收藏则插入记录并更新收藏数）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean collectStrategy(Long id, HttpServletRequest request);

    /**
     * 取消收藏（删除收藏记录并更新收藏数）
     *
     * @param id      攻略ID
     * @param request HTTP请求
     * @return 是否成功
     */
    Boolean uncollectStrategy(Long id, HttpServletRequest request);

    /**
     * 分页查询当前用户的收藏列表（按收藏时间倒序）
     *
     * @param strategyQueryRequest 查询请求（仅使用分页参数）
     * @param request              HTTP请求
     * @return 分页收藏VO
     */
    com.mybatisflex.core.paginate.Page<StrategyCollectVO> listUserCollectStrategies(
            com.oxiris.travelguide.model.dto.strategy.StrategyQueryRequest strategyQueryRequest,
            HttpServletRequest request);

    /**
     * 分页查询某个用户的审核通过攻略（公开）
     *
     * @param userId               用户ID
     * @param strategyQueryRequest 查询请求
     * @return 分页攻略VO
     */
    Page<StrategyVO> listUserStrategies(Long userId, StrategyQueryRequest strategyQueryRequest);
}
