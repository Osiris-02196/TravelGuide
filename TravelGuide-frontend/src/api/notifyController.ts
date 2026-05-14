// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 用户分页查询自己的消息列表 POST /notify/list/my */
export async function listMyNotifies(
  body: API.NotifyQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageNotifyVO>('/notify/list/my', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取当前用户未读消息数量 GET /notify/unread/count */
export async function getUnreadCount(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/notify/unread/count', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 用户一键全部已读 POST /notify/read/all */
export async function readAllNotifies(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/notify/read/all', {
    method: 'POST',
    ...(options || {}),
  })
}

/** 管理员分页查询待审核消息列表 POST /notify/admin/list */
export async function listAdminNotifies(
  body: API.NotifyQueryRequest,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponsePageNotifyVO>('/notify/admin/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 获取管理员未读消息数量 GET /notify/admin/unread/count */
export async function getAdminUnreadCount(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/notify/admin/unread/count', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 管理员一键全部已读 POST /notify/admin/read/all */
export async function readAllAdminNotifies(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/notify/admin/read/all', {
    method: 'POST',
    ...(options || {}),
  })
}
