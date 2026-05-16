package com.oxiris.travelguide.mapper;

import com.mybatisflex.core.BaseMapper;
import com.oxiris.travelguide.model.entity.Strategy;
import org.apache.ibatis.annotations.Select;

/**
 * 攻略 映射层。
 *
 */
public interface StrategyMapper extends BaseMapper<Strategy> {

    /**
     * 根据ID查询攻略（包含逻辑删除的）
     */
    @Select("SELECT * FROM strategy WHERE id = #{id}")
    Strategy selectByIdIncludeDeleted(Long id);
}
