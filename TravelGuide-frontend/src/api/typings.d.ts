declare namespace API {
  type auditStrategyParams = {
    id: string | number
    status: number
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  // ===== 攻略收藏相关类型 =====
  type StrategyCollectVO = {
    id?: string | number
    strategyId?: string | number
    createTime?: string
    strategyTitle?: string
    coverImage?: string
    imageUrls?: string
    strategyContent?: string
    locations?: string
    strategyTags?: string
    userName?: string
    userAvatar?: string
    collectCount?: number
    likeCount?: number
  }

  type PageStrategyCollectVO = {
    records?: StrategyCollectVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageStrategyCollectVO = {
    code?: number
    data?: PageStrategyCollectVO
    message?: string
  }

  type uncollectStrategyParams = {
    id: string | number
  }

  type NotifyQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    receiverId?: number
    type?: string
    isRead?: number
  }

  type NotifyVO = {
    id?: string | number
    receiverId?: string | number
    senderId?: string | number
    senderName?: string
    type?: string
    targetType?: number
    targetId?: string | number
    content?: string
    isRead?: number
    createTime?: string
  }

  type PageNotifyVO = {
    records?: NotifyVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageNotifyVO = {
    code?: number
    data?: PageNotifyVO
    message?: string
  }

  type BaseResponseNotifyVO = {
    code?: number
    data?: NotifyVO
    message?: string
  }

  type BaseResponseListLocationVO = {
    code?: number
    data?: LocationVO[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageStrategyVO = {
    code?: number
    data?: PageStrategyVO
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseStrategyVO = {
    code?: number
    data?: StrategyVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type collectStrategyParams = {
    id: string | number
  }

  type DeleteRequest = {
    id?: number
  }

  type deleteStrategyParams = {
    id: string | number
  }

  type adminDeleteStrategyParams = {
    id: string | number
  }

  type getStrategyDetailParams = {
    id: string | number
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type likeStrategyParams = {
    id: string | number
  }

  type likeCommentParams = {
    id: string | number
  }

  type listCitiesParams = {
    provinceId: number
  }

  type LocationVO = {
    id?: number
    locationCode?: string
    locationName?: string
    parentId?: number
    level?: number
    likeCount?: number
  }

  type LoginUserVO = {
    /** 雪花 ID 以字符串返回，避免 JS 精度丢失 */
    id?: string | number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userStatus?: number
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageStrategyVO = {
    records?: StrategyVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type searchCitiesParams = {
    keyword: string
  }

  type setOfficialParams = {
    id: string | number
    isOfficial: number
  }

  type StrategyAddRequest = {
    strategyTitle?: string
    strategyContent?: string
    imageUrls?: string[]
    strategyTags?: string[]
    locations?: string[]
  }

  type StrategyQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    strategyTitle?: string
    strategyContent?: string
    strategyStatus?: number
    userId?: number
    strategyTags?: string
    location?: string
    isOfficial?: number
    keyword?: string
  }

  type StrategyVO = {
    /** 后端雪花 ID 以字符串返回，避免 JS 精度丢失 */
    id?: string | number
    userId?: string | number
    userName?: string
    userAvatar?: string
    strategyTitle?: string
    strategyContent?: string
    imageUrls?: string
    strategyTags?: string
    strategyStatus?: number
    clickCount?: number
    likeCount?: number
    collectCount?: number
    commentCount?: number
    hotScore?: number
    locations?: string
    isOfficial?: number
    createTime?: string
    updateTime?: string
  }

  // ===== 评论相关类型 =====
  type CommentVO = {
    id?: string | number
    userId?: string | number
    userName?: string
    userAvatar?: string
    strategyId?: string | number
    content?: string
    likeCount?: number
    createTime?: string
    /** 客户端本地状态：是否已点赞 */
    _liked?: boolean
  }

  type PageCommentVO = {
    records?: CommentVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageCommentVO = {
    code?: number
    data?: PageCommentVO
    message?: string
  }

  type CommentAddRequest = {
    strategyId?: string
    content?: string
  }

  type CommentQueryRequest = {
    strategyId?: string
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
  }

  type UpdateUserStatusRequest = {
    id?: number
    userStatus?: number
  }

  type User = {
    id?: number
    userAccount?: string
    userName?: string
    userPassword?: string
    userAvatar?: string
    userRole?: string
    userStatus?: number
    createTime?: string
    updateTime?: string
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateProfileRequest = {
    userName?: string
    userAvatar?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userRole?: string
  }

  type UserVO = {
    /** 雪花 ID 以字符串返回，避免 JS 精度丢失 */
    id?: string | number
    userAccount?: string
    userName?: string
    userStatus?: number
    userAvatar?: string
    userRole?: string
    createTime?: string
    followCount?: number
    followerCount?: number
    isFollowed?: boolean
  }

  type UserFollowVO = {
    id?: number
    userId?: number
    userName?: string
    userAvatar?: string
    userAccount?: string
    createTime?: string
  }

  type FollowCountResult = {
    followCount?: number
    followerCount?: number
  }

  type BaseResponseMapStringLong = {
    code?: number
    data?: Record<string, number>
    message?: string
  }

  type BaseResponseMapStringObject = {
    code?: number
    data?: Record<string, any>
    message?: string
  }

  type BaseResponsePageUserFollowVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  // ===== 举报相关类型 =====
  type ReportAddRequest = {
    targetType?: string
    targetId?: string | number
    reportedUserId?: string | number
    reason?: string
    description?: string
  }

  type ReportQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    reporterId?: number
    targetType?: string
    status?: string
  }

  type ReportReviewRequest = {
    status?: string
    reviewRemark?: string
  }

  type ReportVO = {
    id?: string | number
    reporterId?: string | number
    reporterName?: string
    targetType?: string
    targetId?: string | number
    reportedUserId?: string | number
    reportedUserName?: string
    reason?: string
    description?: string
    status?: string
    targetContent?: string
    strategyId?: string | number
    strategyTitle?: string
    reviewRemark?: string
    reviewAdminId?: string | number
    reviewAdminName?: string
    reviewTime?: string
    createTime?: string
  }

  type PageReportVO = {
    records?: ReportVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type BaseResponsePageReportVO = {
    code?: number
    data?: PageReportVO
    message?: string
  }

  type BaseResponseReportVO = {
    code?: number
    data?: ReportVO
    message?: string
  }

  type reviewReportParams = {
    id: string | number
  }

  // ===== AI 会话相关类型 =====
  type AiChatSession = {
    id: number
    sessionId: string
    title: string
    updateTime: string
    createTime?: string
    userId?: number
  }

  type BaseResponseListAiChatSession = {
    code?: number
    data?: AiChatSession[]
    message?: string
  }

  type BaseResponseAiChatSession = {
    code?: number
    data?: AiChatSession
    message?: string
  }

  type AiChatMessage = {
    id: number
    sessionId: string
    role: string
    content: string
    createTime: string
  }

  type BaseResponseListAiChatMessage = {
    code?: number
    data?: AiChatMessage[]
    message?: string
  }
}
