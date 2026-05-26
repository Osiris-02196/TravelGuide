# CLAUDE.md

#andrej-karpathy-skills
Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

Tradeoff: These guidelines bias toward caution over speed. For trivial tasks, use judgment.

1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.
2. Simplicity First
Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
#andrej-karpathy-skills

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A full-stack "Travel Guide" (旅游攻略) application with a Spring Boot backend and Vue 3 frontend. Users can create, browse, like, collect, and comment on travel strategies. Features include AI-powered travel assistant chat (via DeepSeek), real-time notifications via WebSocket, role-based admin management, and Tencent COS file storage.

## Tech Stack

- **Backend**: Spring Boot 3.5.14, Java 21, Maven, MyBatis-Flex ORM, MySQL, Redis (session + AI chat memory)
- **Frontend**: Vue 3 (Composition API), TypeScript 5.8, Vite 7, Ant Design Vue 4.x, Pinia, Vue Router 4
- **AI**: LangChain4j 1.12.x + DeepSeek API (OpenAI-compatible), Redis-backed chat memory via `MessageWindowChatMemory` (last 20 messages)
- **Infrastructure**: WebSocket (notifications), Tencent COS (file storage), Knife4j (API docs)

## Prerequisites

- **MySQL** and **Redis** must be running locally before starting the backend
- **application-local.yml** (gitignored) — COS credentials, DeepSeek API key, AMap security code. Copy from `application.yml` structure and fill in your own secrets
- **Frontend `.env`** (gitignored) — must contain `VITE_AMAP_KEY` for route planning maps
- Node.js and npm for frontend development

## Development Commands

### Backend (requires MySQL + Redis running locally)
```sh
# Build the project (skip tests)
./mvnw package -DskipTests        # Unix
mvnw.cmd package -DskipTests      # Windows

# Run tests (only one test class exists — context-load check)
./mvnw test

# Run a single test class
./mvnw test -Dtest=TravelGuideApplicationTests

# Run the application (activates application-local.yml profile)
./mvnw spring-boot:run
```

### Frontend
```sh
# In TravelGuide-frontend/ directory
npm install           # Install dependencies
npm run dev           # Start dev server with hot-reload (port 5173)
npm run build         # Type-check + build for production
npm run preview       # Preview production build locally
npm run lint          # ESLint fix
npm run format        # Prettier format
npm run type-check    # TypeScript check only (vue-tsc --build)
npm run openapi2ts    # Regenerate API typings from Knife4j OpenAPI spec (requires backend running on :8082)
```

## Project Structure (high-level)

```
TravelGuide/                          # Spring Boot backend (port 8082, /api/*)
├── src/main/java/com/oxiris/travelguide/
│   ├── ai/                           # LangChain4j AI service
│   ├── annotation/ + aop/            # @AuthCheck, @StatusCheck + interceptors
│   ├── common/                       # BaseResponse, ErrorCode, ResultUtils, PageRequest, DeleteRequest
│   ├── config/                       # CORS, COS, WebSocket, ChatModel, RedisChatMemory, RestTemplate
│   ├── controller/ → service/ → mapper/   # REST controllers (User, Strategy, Comment, Location, Follow, Notify, TravelAi, AiChatSession, Report, AmapProxy)
│   ├── model/{dto,entity,enums,vo}   # Request DTOs, entities, enums, view objects
│   ├── manager/                      # CosManager (Tencent COS upload)
│   ├── websocket/                    # NotifyWebSocketHandler
│   └── generator/                    # MyBatis-Flex code generator (run-and-forget)
├── src/main/resources/
│   ├── mapper/ + prompt/             # XML mappers + AI system prompt
│   ├── application.yml               # Base config (DB, Redis, session 30d, DeepSeek, server port 8082, /api)
│   └── application-local.yml         # Local secrets (COS, API key, AMap) — gitignored
├── sql/                              # Database schema (create_table.sql)
└── TravelGuide-frontend/             # Vue 3 + Vite (port 5173, /api/* proxied)
    └── src/
        ├── api/                      # Axios wrappers (openapi2ts + manual)
        ├── components/ + layouts/    # NotifyBell, AI chat, BasicLayout
        ├── pages/                    # HomePage, StrategyDetail, admin/, user/
        ├── stores/                   # Pinia: loginUser, aiChat, notify
        ├── utils/                    # amap.ts (AMap loader singleton)
        ├── types/                    # amap.d.ts, global.d.ts
        └── request.ts + router/      # Axios instance + Vue Router
```

## Key Architecture Patterns

### Backend Layered Architecture
Controller → Service (interface) → ServiceImpl → Mapper (MyBatis-Flex)

All controllers return `BaseResponse<T>` wrapped via `ResultUtils.success()` / `ResultUtils.error()`. Business exceptions use `BusinessException` and are handled by `GlobalExceptionHandler`.

### Exception Handling Pattern
- `ThrowUtils.throwIf(condition, ErrorCode, message)` — used in every service method for fail-fast validation
- `GlobalExceptionHandler` catches only two exception types: `BusinessException` (preserves code+message) and `RuntimeException` (maps to 50000/"系统错误")
- No Spring-specific exception handling (no `MethodArgumentNotValidException`, etc.)

### Query Building Pattern (MyBatis-Flex)
`StrategyServiceImpl.getQueryWrapper(StrategyQueryRequest)` builds dynamic WHERE clauses by chaining `.eq()` / `.like()` / `.orderBy()` from DTO fields. Keyword search is multi-table aware: it resolves location names to IDs, then OR-combines title-match and location-ID-match conditions. All query wrapper methods follow this same conditional-chain pattern.

### Controller Pattern
- Parameter validation at top via `ThrowUtils.throwIf`
- List queries use `@PostMapping` + `@RequestBody` (pagination params in body)
- Admin endpoints annotated with `@AuthCheck(mustRole = {UserRoleEnum.ADMIN, UserRoleEnum.SUPERADMIN})`
- State-sensitive endpoints annotated with `@StatusCheck(allowedStatus = {UserConstant.NORMAL})`

### API Response Format
```json
{ "code": 0, "data": {...}, "message": "ok" }
```
Error codes in `ErrorCode.java`. Common codes: `40000` (params), `40100` (not logged in), `40101` (no auth), `40400` (not found), `50000` (system error).

### Authentication & Authorization
- Session-based auth stored in Redis (`spring-session-data-redis`), 30-day timeout
- `@AuthCheck(mustRole = {...})` annotation on admin endpoints, enforced by `AuthInterceptor` AOP
- `@StatusCheck(allowedStatus = {...})` annotation checks user status (normal/banned/suspended), enforced by `StatusInterceptor` AOP. Accepts an array of allowed statuses — e.g., `allowedStatus = {UserConstant.NORMAL, UserConstant.MUTED}` allows both normal and muted users
- Roles: `user` (default), `admin`, `superadmin`
- User statuses: `1` (正常), `2` (禁言), `3` (封号)
- Current user retrieved via `UserService.getLoginUser(request)` from session

### ⚠ Snowflake ID + JavaScript Precision
`user.id` and `strategy.id` use MyBatis-Flex Snowflake ID generator (`@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)`). These 19-digit integers exceed `Number.MAX_SAFE_INTEGER`. When serialized to JSON, **JavaScript loses precision on the last digits**. The frontend `typings.d.ts` declares `id` as `string | number` and all route params are read as strings, but new code must never parse strategy/user IDs as `Number()` or rely on exact numeric equality. Backend communication should always pass IDs as strings in query params.

### Enum Convention
Business enums use `getValue()` / `getEnumByValue()` pattern — each has a `value` field, `getValue()` instance method, and a static `getEnumByValue()` lookup map:
- `StrategyStatusEnum` (PENDING=0, APPROVED=1, REJECTED=2), `OfficialStatusEnum` (NORMAL=0, OFFICIAL=1)
- `ReportStatusEnum`, `ReportReasonEnum`, `ReportTargetTypeEnum` (strategy=1 / comment=2)
- `NotifyTypeEnum` (like/comment/collect/system/pending/violation/report_result/warning) — stored as String in DB
- `UserRoleEnum`, `UserStatusEnum`

### Strategy Lifecycle
- **Status flow**: `0` (待审核) → admin approves → `1` (通过) / `2` (拒绝)
- Only status-1 strategies appear on the homepage and location pages
- Users see their own strategies across all statuses in "我的攻略"
- `isOfficial`: `0` (普通) / `1` (官方推荐) — admin can toggle. Official strategies get a badge and appear in the "官方推荐" section on location pages
- Strategies have `hotScore` for the "推荐" (hot) sorting tab

### Frontend Routing & Auth
- Layout-based routing: all app pages are children of `BasicLayout.vue` at path `/`; login/register sit outside at `/user/login`, `/user/register`
- `App.vue` calls `loginUserStore.fetchLoginUser()` on mount — user data is always fetched at app init
- `request.ts` Axios interceptor catches code `40100` and redirects to `/user/login?redirect=...`
- **No Vue Router navigation guards** — auth enforcement is entirely through the Axios response interceptor. Protected routes are NOT guarded by `router.beforeEach`

**Route reference** (all children of `BasicLayout` at path `/`):
| Route | Page | Description |
|-------|------|-------------|
| `/` | HomePage | Latest/hot strategy feed with tag sidebar |
| `/strategy/:id` | StrategyDetail | Strategy detail with comments |
| `/create-strategy` | CreateStrategy | Create new strategy |
| `/my-strategies` | MyStrategies | User's own strategies (table with status column) |
| `/location/:locationId` | LocationStrategies | Strategies by location, with official section |
| `/profile` | UserProfile | Own profile page (strategies + collects tabs) |
| `/user/:userId/profile` | UserProfile | Other user's profile page |
| `/profile/edit` | UserEdit | Edit profile |
| `/my-follows` | MyFollow | Follows/followers list |
| `/admin/pending-strategies` | PendingStrategies | Admin: review pending strategies (table view) |
| `/admin/users` | UserManage | Admin: manage users |
| `/admin/all-strategies` | AllStrategies | Admin: all strategies with set-official actions |
| `/my-reports` | MyReports | User's own report submissions |
| `/admin/reports` | ReportReview | Admin: review pending reports (accept/reject) |

### Frontend State Management (Pinia — Composition API style)
- `loginUserStore` — stores `LoginUserVO`, provides `isLoggedIn`/`isAdmin` computed refs, auto-fetches on mount
- `aiChatStore` — manages session list, current chat messages, send/switch/delete. Messages persisted to DB and re-fetched after each AI response. Saves user+AI messages after each turn
- `notifyStore` — manages notification list, unread count, WebSocket connection lifecycle. Connects on login, auto-increments unread count on push

### Frontend Proxy & API Layer
- Vite dev server proxies `/api/*` → `http://localhost:8082` (see `vite.config.ts`)
- API stubs in `src/api/` are auto-generated from Knife4j OpenAPI spec via `@umijs/openapi` (`npm run openapi2ts`), then **manually augmented** (e.g., `followController.ts`, `ai.ts`, `commentController.ts`, `notifyController.ts`)
- All API functions use the shared `request.ts` Axios instance with `withCredentials: true` for session cookie
- **openapi2ts requires backend running**: config points to `http://localhost:8082/api/v3/api-docs` — backend must be started first. Generated output covers strategy, user, location, report controllers; the remaining API files (follow, ai, comment, notify) are handwritten
- `request.ts` Axios interceptor: on `code === 40100` redirects to `/user/login?redirect=` — this is the **only auth enforcement** in the frontend (no Vue Router navigation guards)

### Frontend Environment Variables & Build
- `VITE_AMAP_KEY` in `.env` — AMap JSAPI key for route planning maps
- Notification WebSocket in DEV mode connects to `localhost:8082`; production uses `window.location.host` — switched via `import.meta.env.DEV`
- `npm run build` uses `npm-run-all2` to **concurrently** run type-check and build (`run-p type-check "build-only {@}" --`)

### Frontend Type Declarations
Types are split across four locations:
- `api/typings.d.ts` — auto-generated API response types (under `API` namespace). Parts of this file are **manually augmented** (e.g., `routeData` on `StrategyAddRequest`, manual API files like `followController.ts` / `ai.ts`)
- `types/amap.d.ts` — manually maintained AMap JS API types (`AMap.Map`, `Marker`, `LngLat`, etc.)
- `types/global.d.ts` — `Window._AMapSecurityConfig` declaration for AMap serviceHost proxy
- `env.d.ts` — references Vite client types and `@amap/amap-jsapi-types`

### UI Pattern: Strategy Card List
Strategy lists are rendered as `.strategy-card-horizontal` cards. The card HTML/CSS pattern is **duplicated independently** across pages (HomePage, LocationStrategiesPage, UserProfilePage, AllStrategiesPage) rather than extracted into a shared component. Each page has its own scoped CSS. The homepage is the reference design — card structure: user row → title → summary → images → tags → location + stats row. Notable exception: **MyStrategiesPage** uses an `a-table` instead of cards.

### AI Chat
- `TravelAiService` interface annotated with LangChain4j `@SystemMessage` / `@MemoryId`
- `TravelAiServiceFactory` creates AI service instances per user using `AiServices.builder()` with `MessageWindowChatMemory` (max 20 messages)
- Chat memory stored in Redis via `RedisChatMemoryStore` (keyed by `"user:{userId}"`)
- Sessions are persisted in `ai_chat_session` + `ai_chat_message` tables
- System prompt in `resources/prompt/AiChat-prompt.txt` — focuses on travel recommendations, itinerary planning, food, and tips
- **Synchronous flow** (not streaming): `TravelAiController.chat()` saves user message → calls DeepSeek → saves AI response → returns string
- Chat model: `deepseek-v4-flash` via OpenAI-compatible API at `https://api.deepseek.com`

### Notifications
- Types: `like` (strategy liked), `comment` (strategy commented), `collect` (strategy collected), `system`
- Created server-side in service methods (like/comment/collect) and pushed via WebSocket
- Frontend `notifyStore` receives WebSocket push with `{ type: "unreadCount", count: N }` — unread count is refreshed by re-fetching the list
- `NotifyBell` component in the layout header shows unread badge
- `ReportDialog` component provides a modal for users to submit reports (select reason + description) on strategies or comments

### WebSocket
- Spring WebSocket config at `/ws/notify/{userId}` via `NotifyWebSocketHandler`
- Frontend `notifyStore` auto-connects when user logs in; uses `localhost:8082` in DEV mode, `window.location.host` in production
- Connection lifecycle: connect on login, disconnect on logout

### Comment System (Nested Replies)
Comments use a self-referencing `parentId` + `replyToUserId` pattern:
- **一级评论**: `parentId IS NULL` — queried with a `replyCount` subquery via `paginateAs(Page.of(...), queryWrapper, CommentVO.class)`
- **回复**: `parentId` points to the root-level comment, `replyToUserId` tracks which user is being replied to
- `CommentVO` joins `user` and `replyUser` tables via MyBatis-Flex left joins to populate `userName`, `userAvatar`, `replyToUserName`
- Admin deletion of a root comment cascades to all its replies (`WHERE parentId = ?`)
- Like/comment actions on comments trigger notifications to the comment author

### Report System
- Users can report strategies or comments; frontend `ReportDialog.vue` provides predefined Chinese reason labels (色情低俗/广告营销/人身攻击/违法违规/虚假信息/抄袭搬运/其他), stored as plain strings in `report.reason`
- Report status flow: `pending` → admin reviews → `approved` (举报成立) / `rejected` (举报驳回)
- Endpoints under `/api/report`: user-facing `POST /add`, `POST /list/my`, `GET /detail/{id}`; admin `POST /admin/list/pending`, `PUT /admin/review/{id}`
- Admin review sends notifications to reporter (`report_result`) and reported user (`violation`), but does not automatically apply mute/ban actions
- Frontend: `ReportDialog.vue` (report submission modal), `MyReportsPage.vue` (user's report history), `ReportReviewPage.vue` (admin review queue)

### User Follow System
- Unidirectional: user A follows user B (no mutual-friend concept)
- Table `user_follow`: followerId → followedUserId with unique constraint
- Only supports user-to-user following (not strategy/bookmark following)
- Follow counts stored by counting rows in `user_follow`
- REST endpoints under `/api/follow`: `POST /{followedUserId}` (toggle), `GET /check/{followedUserId}`, `GET /following/{userId}`, `GET /followers/{userId}`, `GET /count/{userId}`

### Image Handling
- Images uploaded to Tencent COS via `CosManager` (abstracted behind `CosClientConfig`)
- Strategy images stored as JSON array string in `imageUrls` column (e.g., `["url1","url2","url3"]`)
- Frontend parses with `getFirstThreeImages()` helper, shows max 3 thumbnails per card
- Locations stored similarly as JSON array string in `locations` column
- **Upload fallback**: `StrategyServiceImpl.uploadImage()` writes MultipartFile to temp file, tries COS upload; on COS failure, falls back to local filesystem (`static/uploads/`). Temp file always cleaned up in `finally` block

### Dual VO Conversion Pattern
Two private methods in StrategyServiceImpl:
- `getStrategyVO()` — full BeanUtil copy for detail pages
- `getStrategyVOForList()` — truncates `strategyContent` to 20 chars, limits images to 3, manually injects user/location names. List queries always use the `*ForList` variant for performance

### Route Planning (高德地图)
Users can plan routes when creating a strategy and view them on the detail page:
- **Frontend**: `RouteMapEditor.vue` / `RouteMapViewer.vue` in `src/components/` — uses `@amap/amap-jsapi-loader` singleton via `utils/amap.ts` with security proxy `serviceHost: '/api/amap-proxy'`
- **Backend**: `AmapProxyController` proxies `restapi.amap.com` requests, injects `securityJsCode` (keeps key server-side)
- **Data Flow**: `RouteResultData` JSON (transportType, origin, destination, waypoints, polyline, distance, duration, originName, destinationName, waypointNames) stored in strategy's `routeData` TEXT column
- **Multi-leg routing**: Route is calculated segment-by-segment (origin→wp₁, wp₁→wp₂, ..., wpₙ→destination) for all transport types, ensuring waypoints are always respected (Walking/Riding APIs don't natively support waypoints)
- **Transport types**: driving, walking, riding
- **Credentials**: `VITE_AMAP_KEY` in frontend `.env`; `amap.security-js-code` in `application-local.yml`
- **Type declarations**: `types/amap.d.ts` (Map, Marker, Polyline, LngLat, Geocoder, PlaceSearch, etc.)

### Database
Tables: user, strategy, location, comment, strategy_like, strategy_collect, comment_like, strategy_location, notify, ai_chat_session, ai_chat_message, user_follow, report

### Application Configuration
- `application.yml` — base config (DB, Redis, session 30d, AI model, server port 8082, context-path `/api`)
- `application-local.yml` — local overrides (COS credentials, AI API key), gitignored
- Active profile: `local` (set in `application.yml` via `spring.profiles.active`)

### Code Generator
`MyBatisCodeGenerator` is a standalone `main()` method (not a Maven plugin). It parses `application.yml` via Hutool YAML to get DB credentials, generates to `com.oxiris.travelguide.genresult` package. Only generates for `TABLE_NAMES` explicitly listed in the array. Output must be manually moved into the project structure.

### Git Convention
Commit messages use the format `TG-{number}: {description}` (e.g., `TG-9: bug修复`), with Chinese descriptions.
