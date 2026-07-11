import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useUserAuthStore } from '../stores/user-auth'
import { canAccessAdmin, resolveUserRole } from '../utils/permissions'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/user/login',
      name: 'userLogin',
      component: () => import('../views/user/UserLoginView.vue'),
      meta: { title: '用户登录', public: true, userRoute: true },
    },
    {
      path: '/user/register',
      name: 'userRegister',
      component: () => import('../views/user/UserRegisterView.vue'),
      meta: { title: '用户注册', public: true, userRoute: true },
    },
    {
      path: '/user',
      component: () => import('../layouts/UserLayout.vue'),
      children: [
        {
          path: '',
          name: 'userHome',
          component: () => import('../views/user/UserHomeView.vue'),
          meta: { title: '用户首页', public: true, userRoute: true },
        },
        {
          path: 'search',
          name: 'userSearch',
          component: () => import('../views/user/UserSearchView.vue'),
          meta: { title: '综合搜索', public: true, userRoute: true },
        },
        {
          path: 'products',
          name: 'userProducts',
          component: () => import('../views/user/UserProductListView.vue'),
          meta: { title: '商品列表', public: true, userRoute: true },
        },
        {
          path: 'products/:id',
          name: 'userProductDetail',
          component: () => import('../views/user/UserProductDetailView.vue'),
          meta: { title: '商品详情', public: true, userRoute: true },
        },
        {
          path: 'merchants',
          name: 'userMerchants',
          component: () => import('../views/user/UserMerchantListView.vue'),
          meta: { title: '商户列表', public: true, userRoute: true },
        },
        {
          path: 'merchants/:id',
          name: 'userMerchantDetail',
          component: () => import('../views/user/UserMerchantDetailView.vue'),
          meta: { title: '商户详情', public: true, userRoute: true },
        },
        {
          path: 'profile',
          name: 'userProfile',
          component: () => import('../views/user/UserProfileView.vue'),
          meta: { title: '个人中心', public: true, userRoute: true, userAuthRequired: true },
        },
        {
          path: 'payment-result',
          name: 'userPaymentResult',
          component: () => import('../views/user/UserPaymentResultView.vue'),
          meta: { title: '支付结果', public: true, userRoute: true, userAuthRequired: true },
        },
        {
          path: '500',
          name: 'userSystemError',
          component: () => import('../views/user/UserSystemErrorView.vue'),
          meta: { title: '服务异常', public: true, userRoute: true },
        },
        {
          path: ':pathMatch(.*)*',
          name: 'userNotFound',
          component: () => import('../views/user/UserNotFoundView.vue'),
          meta: { title: '页面不存在', public: true, userRoute: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '管理端登录' },
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('../views/ForbiddenView.vue'),
      meta: { title: '无访问权限', public: true },
    },
    {
      path: '/500',
      name: 'systemError',
      component: () => import('../views/SystemErrorView.vue'),
      meta: { title: '系统异常', public: true },
    },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '控制台' },
        },
        {
          path: 'products',
          name: 'products',
          component: () => import('../views/ProductIndexView.vue'),
          meta: { title: '商品管理' },
        },
        {
          path: 'seckill',
          name: 'seckill',
          component: () => import('../views/SeckillManagementView.vue'),
          meta: { title: '秒杀管理' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../views/OrderManagementView.vue'),
          meta: { title: '订单管理' },
        },
        {
          path: 'merchants',
          name: 'merchants',
          component: () => import('../views/MerchantManagementView.vue'),
          meta: { title: '商户管理' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/UserManagementView.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'reviews',
          name: 'reviews',
          component: () => import('../views/ReviewManagementView.vue'),
          meta: { title: '评价管理' },
        },
        {
          path: 'coupon-templates',
          name: 'couponTemplates',
          component: () => import('../views/CouponTemplateManagementView.vue'),
          meta: { title: '优惠券模板管理' },
        },
        {
          path: 'merchant-categories',
          name: 'merchantCategories',
          component: () => import('../views/MerchantCategoryManagementView.vue'),
          meta: { title: '商户分类管理' },
        },
        {
          path: 'operation-logs',
          name: 'operationLogs',
          component: () => import('../views/OperationLogView.vue'),
          meta: { title: '操作日志' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面不存在', public: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const userAuthStore = useUserAuthStore()

  if (to.path.startsWith('/user')) {
    document.title = `${to.meta.title ?? '用户端'} - HuiXiangLife`

    if (to.path === '/user/login' || to.path === '/user/register') {
      if (userAuthStore.isLoggedIn) {
        const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/user/profile'
        return redirect
      }
      return true
    }

    if (to.meta.userAuthRequired && !userAuthStore.isLoggedIn) {
      return {
        path: '/user/login',
        query: { redirect: to.fullPath },
      }
    }

    return true
  }

  if (to.meta.public) {
    document.title = `${to.meta.title ?? '页面'} - HuiXiangLife`
    return true
  }

  if (to.path === '/login') {
    if (authStore.isLoggedIn) {
      return '/dashboard'
    }
    document.title = '管理端登录'
    return true
  }

  if (!authStore.isLoggedIn) {
    return '/login'
  }

  if (!canAccessAdmin(resolveUserRole(authStore.userInfo))) {
    return '/403'
  }

  document.title = `${to.meta.title ?? '后台管理'} - HuiXiangLife`
  return true
})

export default router
