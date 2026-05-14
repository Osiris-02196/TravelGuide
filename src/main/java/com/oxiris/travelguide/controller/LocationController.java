package com.oxiris.travelguide.controller;

import com.oxiris.travelguide.common.BaseResponse;
import com.oxiris.travelguide.common.ErrorCode;
import com.oxiris.travelguide.common.ResultUtils;
import com.oxiris.travelguide.exception.ThrowUtils;
import com.oxiris.travelguide.model.vo.LocationVO;
import com.oxiris.travelguide.service.LocationService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地点 控制层。
 *
 */
@RestController
@RequestMapping("/location")
public class LocationController {

    @Resource
    private LocationService locationService;

    /**
     * 查询所有省级地点（level=2），供前端省/直辖市下拉框使用
     *
     * @return 省级地点VO列表
     */
    @GetMapping("/provinces")
    public BaseResponse<List<LocationVO>> listProvinces() {
        List<LocationVO> result = locationService.listProvinces();
        return ResultUtils.success(result);
    }

    /**
     * 根据省份ID查询下辖市级地点（level=3），供前端城市下拉框使用
     *
     * @param provinceId 省级地点ID
     * @return 市级地点VO列表
     */
    @GetMapping("/cities")
    public BaseResponse<List<LocationVO>> listCities(@RequestParam Long provinceId) {
        ThrowUtils.throwIf(provinceId == null || provinceId <= 0, ErrorCode.PARAMS_ERROR, "省份ID不合法");
        List<LocationVO> result = locationService.listCitiesByProvinceId(provinceId);
        return ResultUtils.success(result);
    }

    /**
     * 根据关键词搜索地级市（level=3），供前端搜索框使用
     *
     * @param keyword 搜索关键词（地级市名称模糊匹配）
     * @return 市级地点VO列表
     */
    @GetMapping("/search")
    public BaseResponse<List<LocationVO>> searchCities(@RequestParam String keyword) {
        ThrowUtils.throwIf(keyword == null || keyword.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        List<LocationVO> result = locationService.searchCities(keyword.trim());
        return ResultUtils.success(result);
    }
}