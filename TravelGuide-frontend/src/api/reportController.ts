// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 用户提交举报 POST /report/add */
export async function addReport(
  body: API.ReportAddRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseLong>('/report/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 用户分页查询自己的举报列表 POST /report/list/my */
export async function listMyReports(
  body: API.ReportQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageReportVO>('/report/list/my', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取举报详情 GET /report/detail/{id} */
export async function getReportDetail(
  params: { id: string | number },
  options?: { [key: string]: any },
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseReportVO>(`/report/detail/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 管理员分页查询待审核举报列表 POST /report/admin/list/pending */
export async function listPendingReports(
  body: API.ReportQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageReportVO>('/report/admin/list/pending', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员审核举报 PUT /report/admin/review/{id} */
export async function reviewReport(
  params: API.reviewReportParams,
  body: API.ReportReviewRequest,
  options?: { [key: string]: any },
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/report/admin/review/${param0}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}
