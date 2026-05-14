package com.oxiris.travelguide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.mapper.LocationMapper;
import com.oxiris.travelguide.model.entity.Location;
import com.oxiris.travelguide.model.vo.LocationVO;
import com.oxiris.travelguide.service.LocationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 地点 服务层实现。
 *
 */
@Service
public class LocationServiceImpl extends ServiceImpl<LocationMapper, Location> implements LocationService {

    @Override
    public List<LocationVO> listProvinces() {
        // 查询所有省级地点（level=2），按likeCount降序、id升序
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("level", 2)
                .orderBy("likeCount", false)
                .orderBy("id", true);
        List<Location> locationList = this.list(queryWrapper);
        // 转换为VO
        return locationList.stream().map(this::getLocationVO).collect(Collectors.toList());
    }

    @Override
    public List<LocationVO> listCitiesByProvinceId(Long provinceId) {
        // 1. 校验参数
        ThrowUtils.throwIf(provinceId == null || provinceId <= 0, ErrorCode.PARAMS_ERROR, "省份ID不合法");
        // 2. 查询指定省份下的市级地点（level=3），按likeCount降序、id升序
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("level", 3)
                .eq("parentId", provinceId)
                .orderBy("likeCount", false)
                .orderBy("id", true);
        List<Location> locationList = this.list(queryWrapper);
        // 3. 转换为VO
        return locationList.stream().map(this::getLocationVO).collect(Collectors.toList());
    }

    @Override
    public List<LocationVO> searchCities(String keyword) {
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(keyword), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        // 2. 模糊搜索地级市（level=3），按likeCount降序、id升序
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("level", 3)
                .like("locationName", keyword)
                .orderBy("likeCount", false)
                .orderBy("id", true);
        List<Location> locationList = this.list(queryWrapper);
        // 3. 转换为VO
        return locationList.stream().map(this::getLocationVO).collect(Collectors.toList());
    }

    @Override
    public LocationVO getLocationVO(Location location) {
        if (location == null) {
            return null;
        }
        LocationVO locationVO = new LocationVO();
        BeanUtil.copyProperties(location, locationVO);
        return locationVO;
    }
}