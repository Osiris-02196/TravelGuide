// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /location/cities */
export async function listCities(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listCitiesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListLocationVO>('/location/cities', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /location/provinces */
export async function listProvinces(options?: { [key: string]: any }) {
  return request<API.BaseResponseListLocationVO>('/location/provinces', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /location/search */
export async function searchCities(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.searchCitiesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListLocationVO>('/location/search', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
