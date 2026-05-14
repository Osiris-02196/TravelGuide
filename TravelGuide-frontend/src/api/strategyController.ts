// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /strategy/add */
export async function addStrategy(body: API.StrategyAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/strategy/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT /strategy/admin/audit/${param0} */
export async function auditStrategy(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.auditStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/admin/audit/${param0}`, {
    method: 'PUT',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/admin/list/pending */
export async function listPendingStrategies(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>('/strategy/admin/list/pending', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT /strategy/admin/official/${param0} */
export async function setOfficial(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.setOfficialParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/admin/official/${param0}`, {
    method: 'PUT',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/collect/${param0} */
export async function collectStrategy(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.collectStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/collect/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/delete/${param0} */
export async function deleteStrategy(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /strategy/detail/${param0} */
export async function getStrategyDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getStrategyDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseStrategyVO>(`/strategy/detail/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/like/${param0} */
export async function likeStrategy(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likeStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/like/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/list/by-location */
export async function listStrategiesByLocation(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>('/strategy/list/by-location', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/list/hot */
export async function listHotStrategies(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>('/strategy/list/hot', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/list/my */
export async function listMyStrategies(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>('/strategy/list/my', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/list/my-collect */
export async function listUserCollectStrategies(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyCollectVO>('/strategy/list/my-collect', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/uncollect/${param0} */
export async function uncollectStrategy(
  params: API.uncollectStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/uncollect/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 管理员删除攻略 POST /strategy/admin/delete/${param0} */
export async function adminDeleteStrategy(
  params: API.adminDeleteStrategyParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/strategy/admin/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/list/passed */
export async function listPassedStrategies(
  body: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>('/strategy/list/passed', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** GET /strategy/list/user/${param0} - 查询某个用户的审核通过攻略 */
export async function listUserStrategies(
  userId: string,
  params?: API.StrategyQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageStrategyVO>(`/strategy/list/user/${userId}`, {
    method: 'GET',
    params: { pageNum: 1, pageSize: 20, ...params },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /strategy/upload/image */
export async function uploadImage(body: {}, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/strategy/upload/image', {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}
