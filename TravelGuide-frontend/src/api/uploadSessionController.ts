// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** PC端：创建上传会话 POST /upload/session */
export async function createUploadSession(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject>('/upload/session', {
    method: 'POST',
    ...(options || {}),
  })
}

/** PC端：获取会话中的图片列表及完成状态 GET /upload/session/{token}/images */
export async function getSessionImages(
  token: string,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseMapStringObject>(`/upload/session/${token}/images`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 手机端：上传单张图片到会话 POST /upload/session/{token}/images */
export async function uploadToSession(
  token: string,
  body: {},
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseString>(`/upload/session/${token}/images`, {
    method: 'POST',
    data: body,
    ...(options || {}),
  })
}

/** 手机端：标记会话上传完成 PUT /upload/session/{token}/complete */
export async function completeSession(
  token: string,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseVoid>(`/upload/session/${token}/complete`, {
    method: 'PUT',
    ...(options || {}),
  })
}

/** PC端：取消/删除上传会话 DELETE /upload/session/{token} */
export async function deleteUploadSession(
  token: string,
  options?: { [key: string]: any },
) {
  return request<API.BaseResponseVoid>(`/upload/session/${token}`, {
    method: 'DELETE',
    ...(options || {}),
  })
}
