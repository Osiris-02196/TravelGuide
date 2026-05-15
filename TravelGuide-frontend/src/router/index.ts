import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/pages/HomePage.vue'),
      },
      {
        path: 'my-strategies',
        name: 'myStrategies',
        component: () => import('@/pages/MyStrategiesPage.vue'),
      },
      {
        path: 'create-strategy',
        name: 'createStrategy',
        component: () => import('@/pages/CreateStrategyPage.vue'),
      },
      {
        path: 'strategy/:id',
        name: 'strategyDetail',
        component: () => import('@/pages/StrategyDetailPage.vue'),
      },
      {
        path: 'location/:locationId',
        name: 'locationStrategies',
        component: () => import('@/pages/LocationStrategiesPage.vue'),
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/pages/user/UserProfilePage.vue'),
      },
      {
        path: 'user/:userId/profile',
        name: 'userProfile',
        component: () => import('@/pages/user/UserProfilePage.vue'),
      },
      {
        path: 'profile/edit',
        name: 'profileEdit',
        component: () => import('@/pages/user/UserEditPage.vue'),
      },
      {
        path: 'my-follows',
        name: 'myFollows',
        component: () => import('@/pages/MyFollowPage.vue'),
      },
      {
        path: 'admin/pending-strategies',
        name: 'pendingStrategies',
        component: () => import('@/pages/admin/PendingStrategiesPage.vue'),
      },
      {
        path: 'admin/users',
        name: 'userManage',
        component: () => import('@/pages/admin/UserManagePage.vue'),
      },
      {
        path: 'admin/all-strategies',
        name: 'allStrategies',
        component: () => import('@/pages/admin/AllStrategiesPage.vue'),
      },
    ],
  },
  {
    path: '/user/login',
    name: 'login',
    component: () => import('@/pages/user/UserLoginPage.vue'),
  },
  {
    path: '/user/register',
    name: 'register',
    component: () => import('@/pages/user/UserRegisterPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

// 禁止浏览器原生滚动恢复，交由 Vue Router scrollBehavior 接管
if ('scrollRestoration' in history) {
  history.scrollRestoration = 'manual'
}

export default router
