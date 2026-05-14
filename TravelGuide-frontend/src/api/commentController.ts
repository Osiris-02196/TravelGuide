// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 添加评论 POST /comment/add */
export async function addComment(body: API.CommentAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/comment/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 分页查询评论列表 POST /comment/list */
export async function listComments(body: API.CommentQueryRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponsePageCommentVO>('/comment/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 点赞评论 POST /comment/like/${param0} */
export async function likeComment(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likeCommentParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean>(`/comment/like/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}