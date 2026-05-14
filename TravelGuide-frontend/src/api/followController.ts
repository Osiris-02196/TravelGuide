import request from '@/request'

/** POST /follow/${param0} - 关注/取消关注 */
export async function toggleFollow(
  followedUserId: string,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>(`/follow/${followedUserId}`, {
    method: 'POST',
    ...(options || {}),
  })
}

/** GET /follow/check/${param0} - 检查是否已关注 */
export async function checkFollowed(
  followedUserId: string,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>(`/follow/check/${followedUserId}`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** GET /follow/following/${param0} - 获取关注列表 */
export async function listFollowing(
  userId: string,
  params?: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO>(`/follow/following/${userId}`, {
    method: 'GET',
    params: { pageNum: 1, pageSize: 20, ...params },
    ...(options || {}),
  })
}

/** GET /follow/followers/${param0} - 获取粉丝列表 */
export async function listFollowers(
  userId: string,
  params?: { pageNum?: number; pageSize?: number },
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO>(`/follow/followers/${userId}`, {
    method: 'GET',
    params: { pageNum: 1, pageSize: 20, ...params },
    ...(options || {}),
  })
}

/** GET /follow/count/${param0} - 获取关注数和粉丝数 */
export async function getFollowCounts(
  userId: string,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringLong>(`/follow/count/${userId}`, {
    method: 'GET',
    ...(options || {}),
  })
}
