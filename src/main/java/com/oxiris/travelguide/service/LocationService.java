package com.oxiris.travelguide.service;

import com.mybatisflex.core.service.IService;
import com.oxiris.travelguide.model.entity.Location;
import com.oxiris.travelguide.model.vo.LocationVO;

import java.util.List;

/**
 * 地点 服务层。
 *
 */
public interface LocationService extends IService<Location> {

    /**
     * 查询所有省级地点（level=2），按likeCount降序、id升序排列
     *
     * @return 省级地点VO列表
     */
    List<LocationVO> listProvinces();

    /**
     * 根据父级ID（省份ID）查询下辖市级地点（level=3），按likeCount降序、id升序排列
     *
     * @param provinceId 省级地点ID
     * @return 市级地点VO列表
     */
    List<LocationVO> listCitiesByProvinceId(Long provinceId);

    /**
     * 根据关键词搜索地级市（level=3），返回模糊匹配的市级地点列表
     *
     * @param keyword 搜索关键词（地级市名称模糊匹配）
     * @return 市级地点VO列表
     */
    List<LocationVO> searchCities(String keyword);

    /**
     * 将地点实体转换为VO
     *
     * @param location 地点实体
     * @return 地点VO
     */
    LocationVO getLocationVO(Location location);
}