import { writeFileSync, mkdirSync } from 'fs'
import { join } from 'path'

const srcDir = 'src'

function write(relativePath, content) {
  const parts = relativePath.split('/')
  if (parts.length > 1) {
    mkdirSync(join(srcDir, ...parts.slice(0, -1)), { recursive: true })
  }
  writeFileSync(join(srcDir, relativePath), content, 'utf8')
  console.log('Wrote:', relativePath)
}

// Router
write('router/index.ts', `import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      redirect: '/home',
      children: [
        { path: 'home', name: 'Home', component: () => import('@/pages/HomePage.vue') },
        { path: 'my-strategies', name: 'MyStrategies', component: () => import('@/pages/MyStrategiesPage.vue') },
        { path: 'create-strategy', name: 'CreateStrategy', component: () => import('@/pages/CreateStrategyPage.vue') },
        { path: 'strategy/:id', name: 'StrategyDetail', component: () => import('@/pages/StrategyDetailPage.vue') },
        { path: 'location-strategy', name: 'LocationStrategy', component: () => import('@/pages/LocationStrategyPage.vue') },
        { path: 'profile', name: 'Profile', component: () => import('@/pages/user/UserProfilePage.vue') },
        { path: 'profile/edit', name: 'ProfileEdit', component: () => import('@/pages/user/UserEditPage.vue') },
        { path: 'admin/audit', name: 'AdminAudit', component: () => import('@/pages/admin/AuditStrategyPage.vue') },
        { path: 'admin/users', name: 'AdminUsers', component: () => import('@/pages/admin/UserManagePage.vue') },
        { path: 'admin/strategies', name: 'AdminStrategies', component: () => import('@/pages/admin/AllStrategiesPage.vue') },
      ],
    },
    { path: '/user/login', name: 'UserLogin', component: () => import('@/pages/user/UserLoginPage.vue') },
    { path: '/user/register', name: 'UserRegister', component: () => import('@/pages/user/UserRegisterPage.vue') },
  ],
})

export default router
`)
