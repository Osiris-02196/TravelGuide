# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A full-stack "Travel Guide" (旅游攻略) application with a Spring Boot backend and Vue 3 frontend. Users can create, browse, like, collect, and comment on travel strategies. Features include AI-powered travel assistant chat (via DeepSeek), real-time notifications via WebSocket, role-based admin management, and Tencent COS file storage.

## Tech Stack

- **Backend**: Spring Boot 3.5, Java 21, Maven, MyBatis-Flex ORM, MySQL, Redis (session + AI chat memory)
- **Frontend**: Vue 3 (Composition API), TypeScript 5.8, Vite 7, Ant Design Vue 4.x, Pinia, Vue Router 4
- **AI**: LangChain4j 1.12.x + DeepSeek API (OpenAI-compatible), Redis-backed chat memory via `MessageWindowChatMemory` (last 20 messages)
- **Infrastructure**: WebSocket (notifications), Tencent COS (file storage), Knife4j (API docs)

## Prerequisites

- **MySQL** and **Redis** must be running locally before starting the backend
- **application-local.yml** (gitignored) must exist with COS credentials and DeepSeek API key. Copy from `application.yml` structure and fill in your own secrets
- Node.js and npm for frontend development

## Development Commands

### Backend (requires MySQL + Redis running locally)
```sh
# Build the project (skip tests)
./mvnw package -DskipTests
# On Windows: mvnw.cmd package -DskipTests

# Run tests
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
npm run lint          # Lint and fix
npm run format        # Prettier format
npm run type-check    # TypeScript check only (vue-tsc)
npm run openapi2ts    # Regenerate API typings from Knife4j OpenAPI spec
```

## Project Structure

```
TravelGuide/
├── src/main/java/com/oxiris/travelguide/
│   ├── ai/                    # LangChain4j AI service (TravelAiService + Factory)
│   ├── annotation/            # Custom annotations (@AuthCheck, @StatusCheck)
│   ├── aop/                   # AOP interceptors (AuthInterceptor, StatusInterceptor)
│   ├── common/                # Shared: BaseResponse, ErrorCode, ResultUtils, PageRequest
│   ├── config/                # Spring configs (CORS, COS, WebSocket, ChatModel, RedisChatMemoryStore)
│   ├── constant/              # Constants (user roles, file config)
│   ├── controller/            # REST controllers (8 total — User, Strategy, Comment, Location, Notify, Follow, TravelAi, AiChatSession)
│   ├── exception/             # BusinessException + GlobalExceptionHandler + ThrowUtils
│   ├── generator/             # MyBatis-Flex code generator
│   ├── manager/               # CosManager (Tencent COS file upload abstraction)
│   ├── mapper/                # MyBatis-Flex Mapper interfaces + XML under resources/mapper/
│   ├── model/
│   │   ├── dto/               # Request DTOs (grouped by domain)
│   │   ├── entity/            # DB entity classes
│   │   ├── enums/             # Enums (UserRole, UserStatus)
│   │   └── vo/                # View Objects (response wrappers)
│   ├── service/               # Service interfaces + impl/
│   └── websocket/             # WebSocket handler (NotifyWebSocketHandler)
├── src/main/resources/
│   ├── mapper/                # MyBatis-Flex XML mappers
│   ├── prompt/                # AI system prompt (AiChat-prompt.txt)
│   ├── application.yml        # Main config — DB, Redis, session 30d, AI (DeepSeek), server port 8082
│   └── application-local.yml  # Local overrides (COS credentials, AI API key) — gitignored
├── sql/                       # Database schema (create_table.sql)
└── TravelGuide-frontend/
    └── src/
        ├── api/               # Axios API wrappers (auto-generated via openapi2ts + manual additions)
        ├── components/        # Shared components (NotifyBell, AI chat components)
        ├── layouts/           # BasicLayout (sidebar + header + content area)
        ├── pages/             # Page components organized as user/, admin/, and root-level
        ├── router/            # Vue Router — layout-based routing through BasicLayout
        ├── stores/            # Pinia stores (loginUser, aiChat, notify)
        ├── request.ts         # Axios instance with interceptors + 40100 redirect
        ├── App.vue            # Root — auto-fetches login user on mount
        └── main.ts            # App entry point
```

## Key Architecture Patterns

### Backend Layered Architecture
Controller → Service (interface) → ServiceImpl → Mapper (MyBatis-Flex)

All controllers return `BaseResponse<T>` wrapped via `ResultUtils.success()` / `ResultUtils.error()`. Business exceptions use `BusinessException` and are handled by `GlobalExceptionHandler`.

### API Response Format
```json
{ "code": 0, "data": {...}, "message": "ok" }
```
Error codes in `ErrorCode.java`. Common codes: `40000` (params), `40100` (not logged in), `40101` (no auth), `40400` (not found), `50000` (system error).

### Authentication & Authorization
- Session-based auth stored in Redis (`spring-session-data-redis`), 30-day timeout
- `@AuthCheck(mustRole = {...})` annotation on admin endpoints, enforced by `AuthInterceptor` AOP
- `@StatusCheck` annotation checks user status (normal/banned/suspended), enforced by `StatusInterceptor` AOP
- Roles: `user` (default), `admin`, `superadmin`
- User statuses: `1` (正常), `2` (禁言), `3` (封号)
- Current user retrieved via `UserService.getLoginUser(request)` from session

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

**Route reference** (all children of `BasicLayout` at path `/`):
| Route | Page | Description |
|-------|------|-------------|
| `/` | HomePage | Latest/hot strategy feed with tag sidebar |
| `/strategy/:id` | StrategyDetail | Strategy detail with comments |
| `/create-strategy` | CreateStrategy | Create new strategy |
| `/my-strategies` | MyStrategies | User's own strategies (passed/pending/rejected/collect) |
| `/location/:locationId` | LocationStrategies | Strategies by location, with official section |
| `/profile` | UserProfile | Own profile page |
| `/user/:userId/profile` | UserProfile | Other user's profile page |
| `/profile/edit` | UserEdit | Edit profile |
| `/my-follows` | MyFollow | Follows/followers list |
| `/admin/pending-strategies` | PendingStrategies | Admin: review pending strategies (table view) |
| `/admin/users` | UserManage | Admin: manage users |
| `/admin/all-strategies` | AllStrategies | Admin: all strategies with set-official actions |

### Frontend State Management (Pinia — Composition API style)
- `loginUserStore` — stores `LoginUserVO`, provides `isLoggedIn`/`isAdmin` computed refs, auto-fetches on mount
- `aiChatStore` — manages session list, current chat messages, send/switch/delete. Messages persisted to DB and re-fetched after each AI response. Saves user+AI messages after each turn
- `notifyStore` — manages notification list, unread count, WebSocket connection lifecycle. Connects on login, auto-increments unread count on push

### Frontend Proxy & API Layer
- Vite dev server proxies `/api/*` → `http://localhost:8082` (see `vite.config.ts`)
- API stubs in `src/api/` are auto-generated from Knife4j OpenAPI spec via `@umijs/openapi` (`npm run openapi2ts`), then manually augmented (e.g., `followController.ts`, `ai.ts`)
- All API functions use the shared `request.ts` Axios instance with `withCredentials: true` for session cookie

### UI Pattern: Strategy Card List
Strategy lists are rendered as `.strategy-card-horizontal` cards. The card HTML/CSS pattern is **duplicated independently** across 5 pages (HomePage, LocationStrategiesPage, MyStrategiesPage, UserProfilePage, AllStrategiesPage) rather than extracted into a shared component. Each page has its own scoped CSS. The homepage is the reference design with:
- Card width: 680px (homepage) / responsive max-width 720px (other pages), border-radius 12px
- Thumbnails: 200×200px, 3 per row, gap 24px
- Hover: box-shadow lift + translateY(-2px) animation
- Structure: user row → title → summary → images → tags row → location + stats row

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

### WebSocket
- Spring WebSocket config at `/ws/notify/{userId}` via `NotifyWebSocketHandler`
- Frontend `notifyStore` auto-connects when user logs in; uses `localhost:8082` in DEV mode, `window.location.host` in production
- Connection lifecycle: connect on login, disconnect on logout

### User Follow System
- Unidirectional: user A follows user B (no mutual-friend concept)
- Table `user_follow`: followerId → followedUserId with unique constraint
- Only supports user-to-user following (not strategy/bookmark following)
- Follow counts stored by counting rows in `user_follow`

### Image Handling
- Images uploaded to Tencent COS via `CosManager` (abstracted behind `CosClientConfig`)
- Strategy images stored as JSON array string in `imageUrls` column (e.g., `["url1","url2","url3"]`)
- Frontend parses with `getFirstThreeImages()` helper, shows max 3 thumbnails per card
- Locations stored similarly as JSON array string in `locations` column

### Database
Tables: user, strategy, location, comment, strategy_like, strategy_collect, comment_like, strategy_location, notify, ai_chat_session, ai_chat_message, user_follow

### Application Configuration
- `application.yml` — base config (DB, Redis, session 30d, AI model, server port 8082, context-path `/api`)
- `application-local.yml` — local overrides (COS credentials, AI API key), gitignored
- Active profile: `local` (set in `application.yml` via `spring.profiles.active`)

### Git Convention
Commit messages use the format `TG-{number}: {description}` (e.g., `TG-9: bug修复`), with Chinese descriptions.
