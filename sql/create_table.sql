create database if not exists tg;

use tg;
CREATE TABLE user (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
                      userAccount VARCHAR(50) UNIQUE NOT NULL COMMENT '用户账号',
                      userName VARCHAR(50) DEFAULT NULL COMMENT '用户姓名',
                      userPassword VARCHAR(255) NOT NULL COMMENT '用户密码',
                      userAvatar VARCHAR(255) COMMENT '头像',
                      userRole     varchar(256) default 'user' NOT NULL comment '用户角色：user/admin/superadmin',
                      userStatus TINYINT DEFAULT 1 COMMENT '状态（1-正常 2-禁言 3-封号）',
                      createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
                      updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      isDelete tinyint default 0 not null comment '是否删除'
) comment '用户' collate = utf8mb4_unicode_ci;

CREATE TABLE location (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地点ID',
                          locationCode VARCHAR(20) COMMENT '地点编码',
                          locationName VARCHAR(100) NOT NULL COMMENT '地点名称',
                          parentId BIGINT DEFAULT 0 COMMENT '父级地点ID',
                          level TINYINT NOT NULL COMMENT '层级（1国家 2省 3市）',
                          likeCount INT DEFAULT 0 COMMENT '点赞量',
                          createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
                          updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          INDEX idx_level (level),
                          INDEX idx_parent_id (parentId),
                          INDEX idx_location_name (locationName(20)),
                          isDelete TINYINT DEFAULT 0 NOT NULL
)COMMENT='地点'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE strategy (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '攻略ID',
                          userId BIGINT NOT NULL COMMENT '发布用户ID',
                          strategyTitle VARCHAR(200) NOT NULL COMMENT '攻略标题',
                          strategyContent TEXT COMMENT '攻略内容',
                          imageUrls TEXT COMMENT '攻略图片(JSON格式)',
                          locations VARCHAR(1024) DEFAULT NULL COMMENT '地点标签(JSON数组)' AFTER hotScore,
                          strategyTags VARCHAR(255) COMMENT '攻略标签（逗号分隔）',
                          strategyStatus TINYINT DEFAULT 0 COMMENT '审核状态（0-待审核，1-通过，2-拒绝）',
                          clickCount INT DEFAULT 0 COMMENT '点击量',
                          likeCount INT DEFAULT 0 COMMENT '点赞量',
                          collectCount INT DEFAULT 0 COMMENT '收藏量',
                          commentCount INT DEFAULT 0 COMMENT '评论数' AFTER collectCount,
                          hotScore DOUBLE DEFAULT 0 COMMENT '热度分数',
                          createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          isOfficial INT DEFAULT 0 COMMENT '官方推荐(0-否 1-是)' AFTER locations,
                          isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除（0-否 1-是）'
) COMMENT='攻略'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE strategy_location (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
                                   strategyId BIGINT NOT NULL COMMENT '攻略ID',
                                   locationId BIGINT NOT NULL COMMENT '地点ID',
                                   sortOrder INT DEFAULT 0 COMMENT '地点排序',
                                   createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除（0-否 1-是）'
) COMMENT='攻略地点关联表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE comment (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
                         userId BIGINT NOT NULL COMMENT '用户ID',
                         strategyId BIGINT NOT NULL COMMENT '攻略ID',
                         parentId BIGINT NULL COMMENT '所属一级评论ID（NULL=顶级）',
                         replyToUserId BIGINT NULL COMMENT '被回复用户ID',

                         content TEXT COMMENT '评论内容',
                         likeCount INT DEFAULT 0,

                         commentStatus TINYINT DEFAULT 1 COMMENT '状态（1正常，0违规）',

                         createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

                         isDelete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除（0-否 1-是）',

                         INDEX idx_strategyId (strategyId),
                         INDEX idx_strategy_time (strategyId, createTime),
                         INDEX idx_parentId (parentId)
) COMMENT='评论表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE comment_like (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',

                              userId BIGINT NOT NULL COMMENT '用户ID',
                              commentId BIGINT NOT NULL COMMENT '评论ID',

                              createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',

                              UNIQUE KEY uk_user_comment (userId, commentId)
) COMMENT='评论点赞表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE strategy_collect (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',

                                  userId BIGINT NOT NULL COMMENT '用户ID',
                                  strategyId BIGINT NOT NULL COMMENT '攻略ID',

                                  createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',

                                  UNIQUE KEY uk_user_strategy (userId, strategyId),

                                  INDEX idx_userId (userId),
                                  INDEX idx_strategyId (strategyId)
) COMMENT='攻略收藏表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notify (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',

                        receiverId BIGINT NOT NULL COMMENT '接收用户ID',
                        senderId BIGINT DEFAULT NULL COMMENT '发送者ID（系统通知可为空）',

                        type VARCHAR(20) NOT NULL COMMENT '消息类型（like/comment/collect/system）',

                        targetType TINYINT NOT NULL COMMENT '目标类型（1攻略 2评论 3用户）',
                        targetId BIGINT NOT NULL COMMENT '目标ID',

                        content VARCHAR(255) COMMENT '消息内容',

                        isRead TINYINT DEFAULT 0 NOT NULL COMMENT '是否已读（0未读 1已读）',

                        createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                        INDEX idx_receiverId (receiverId),
                        INDEX idx_receiver_read (receiverId, isRead),
                        INDEX idx_createTime (createTime)
) COMMENT='消息表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE ai_chat_session (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 userId BIGINT NOT NULL COMMENT '用户ID',
                                 title VARCHAR(100) COMMENT '会话标题',
                                 sessionId VARCHAR(100) NOT NULL COMMENT 'session标识（对应Redis key）',
                                 createTime DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 isDelete TINYINT DEFAULT 0
);

CREATE TABLE strategy_like (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
                               userId BIGINT NOT NULL COMMENT '用户ID',
                               strategyId BIGINT NOT NULL COMMENT '攻略ID',
                               createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                               UNIQUE KEY uk_user_strategy (userId, strategyId),
                               INDEX idx_userId (userId),
                               INDEX idx_strategyId (strategyId)
) COMMENT='攻略点赞表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    followerId BIGINT NOT NULL COMMENT '关注者用户ID',
    followedUserId BIGINT NOT NULL COMMENT '被关注者用户ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY uk_follower_followed (followerId, followedUserId),
    INDEX idx_followerId (followerId),
    INDEX idx_followedUserId (followedUserId)
) COMMENT='用户关注表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    sessionId VARCHAR(100) NOT NULL COMMENT '会话ID',
    role VARCHAR(10) NOT NULL COMMENT '角色(user/ai)',
    content TEXT NOT NULL COMMENT '消息内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sessionId (sessionId)
) COMMENT='AI聊天消息表'
    COLLATE = utf8mb4_unicode_ci;

CREATE TABLE report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
    reporterId BIGINT NOT NULL COMMENT '举报人ID',
    targetType VARCHAR(20) NOT NULL COMMENT '举报类型（strategy/comment）',
    targetId BIGINT NOT NULL COMMENT '被举报对象ID',
    reportedUserId BIGINT NOT NULL COMMENT '被举报用户ID',
    reason VARCHAR(50) NOT NULL COMMENT '举报原因',
    description VARCHAR(500) DEFAULT NULL COMMENT '举报详细说明',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '举报状态（pending/approved/rejected）',
    reviewRemark VARCHAR(500) DEFAULT NULL COMMENT '管理员审核说明',
    reviewAdminId BIGINT DEFAULT NULL COMMENT '审核管理员ID',
    reviewTime DATETIME DEFAULT NULL COMMENT '审核时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_reporterId (reporterId),
    INDEX idx_reportedUserId (reportedUserId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT='举报表'
    COLLATE = utf8mb4_unicode_ci;

ALTER TABLE comment
  ADD COLUMN parentId BIGINT NULL COMMENT '所属一级评论ID（NULL=顶级）' AFTER strategyId,
  ADD COLUMN replyToUserId BIGINT NULL COMMENT '被回复用户ID' AFTER parentId;

ALTER TABLE comment ADD INDEX idx_parentId (parentId);

ALTER TABLE strategy ADD COLUMN routeData TEXT NULL COMMENT '路线规划数据（JSON）';

































