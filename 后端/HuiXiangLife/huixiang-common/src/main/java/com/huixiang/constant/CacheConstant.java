package com.huixiang.constant;

public final class CacheConstant {

    public static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:"; // 商品详情缓存 key 前缀
    public static final String PRODUCT_DETAIL_NULL_VALUE = "NULL"; // 缓存空值占位标记
    public static final long PRODUCT_DETAIL_TTL_MINUTES = 30L; // 商品详情缓存基础过期时间，单位：分钟
    public static final long PRODUCT_DETAIL_NULL_TTL_MINUTES = 3L; // 商品详情空值缓存过期时间，单位：分钟
    public static final int PRODUCT_DETAIL_TTL_RANDOM_BOUND_MINUTES = 10; // 商品详情随机过期时间上界，单位：分钟

    public static final String MERCHANT_DETAIL_KEY_PREFIX = "merchant:detail:"; // 商户详情缓存 key 前缀
    public static final long MERCHANT_DETAIL_TTL_MINUTES = 30L; // 商户详情缓存基础过期时间，单位：分钟
    public static final long MERCHANT_DETAIL_NULL_TTL_MINUTES = 3L; // 商户详情空值缓存过期时间，单位：分钟
    public static final int MERCHANT_DETAIL_TTL_RANDOM_BOUND_MINUTES = 10; // 商户详情随机过期时间上界，单位：分钟

    public static final String COUPON_DETAIL_KEY_PREFIX = "coupon:detail:"; // 优惠券模板详情缓存 key 前缀
    public static final long COUPON_DETAIL_TTL_MINUTES = 30L; // 优惠券模板详情缓存基础过期时间，单位：分钟
    public static final int COUPON_DETAIL_TTL_RANDOM_BOUND_MINUTES = 10; // 优惠券模板详情随机过期时间上界，单位：分钟

    public static final String PRODUCT_RECOMMEND_KEY_PREFIX = "product:recommend:"; // 商品推荐列表缓存 key 前缀
    public static final long PRODUCT_RECOMMEND_TTL_MINUTES = 10L; // 商品推荐列表缓存基础过期时间，单位：分钟
    public static final int PRODUCT_RECOMMEND_TTL_RANDOM_BOUND_MINUTES = 5; // 商品推荐列表随机过期时间上界，单位：分钟

    public static final String HOME_PRODUCT_PAGE_KEY_PREFIX = "home:product:page:"; // 用户端商品首屏分页缓存 key 前缀
    public static final long HOME_PRODUCT_PAGE_TTL_MINUTES = 5L; // 用户端商品首屏分页缓存基础过期时间，单位：分钟
    public static final int HOME_PRODUCT_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端商品首屏分页随机过期时间上界，单位：分钟

    public static final String HOME_AGGREGATE_KEY = "home:aggregate"; // 用户端首页聚合缓存 key
    public static final long HOME_AGGREGATE_TTL_MINUTES = 5L; // 用户端首页聚合缓存基础过期时间，单位：分钟
    public static final int HOME_AGGREGATE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端首页聚合缓存随机过期时间上界，单位：分钟
    public static final int HOME_AGGREGATE_RECOMMEND_LIMIT = 10; // 用户端首页推荐商品数量
    public static final int HOME_AGGREGATE_MERCHANT_PAGE_SIZE = 10; // 用户端首页商户首屏数量

    public static final String SEARCH_HOT_KEY = "search:hot"; // 热门搜索词缓存 key
    public static final long SEARCH_HOT_TTL_MINUTES = 10L; // 热门搜索词缓存过期时间，单位：分钟

    public static final String HOME_MERCHANT_PAGE_KEY_PREFIX = "home:merchant:page:"; // 用户端商户首屏分页缓存 key 前缀
    public static final long HOME_MERCHANT_PAGE_TTL_MINUTES = 5L; // 用户端商户首屏分页缓存基础过期时间，单位：分钟
    public static final int HOME_MERCHANT_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端商户首屏分页随机过期时间上界，单位：分钟

    public static final String MERCHANT_CATEGORY_LIST_KEY = "merchant:category:list"; // 商户分类列表缓存 key
    public static final long MERCHANT_CATEGORY_LIST_TTL_MINUTES = 30L; // 商户分类列表缓存基础过期时间，单位：分钟
    public static final int MERCHANT_CATEGORY_LIST_TTL_RANDOM_BOUND_MINUTES = 10; // 商户分类列表随机过期时间上界，单位：分钟

    public static final String USER_COUPON_PAGE_KEY_PREFIX = "user:coupon:page:"; // 用户端优惠券分页缓存 key 前缀
    public static final long USER_COUPON_PAGE_TTL_MINUTES = 5L; // 用户端优惠券分页缓存基础过期时间，单位：分钟
    public static final int USER_COUPON_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端优惠券分页随机过期时间上界，单位：分钟

    public static final String REVIEW_PAGE_KEY_PREFIX = "review:page:"; // 用户端评价分页缓存 key 前缀
    public static final long REVIEW_PAGE_TTL_MINUTES = 5L; // 用户端评价分页缓存基础过期时间，单位：分钟
    public static final int REVIEW_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端评价分页随机过期时间上界，单位：分钟

    public static final String FAVORITE_PAGE_KEY_PREFIX = "user:favorite:page:"; // 用户端收藏分页缓存 key 前缀
    public static final long FAVORITE_PAGE_TTL_MINUTES = 5L; // 用户端收藏分页缓存基础过期时间，单位：分钟
    public static final int FAVORITE_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端收藏分页随机过期时间上界，单位：分钟

    public static final String ORDER_DETAIL_KEY_PREFIX = "user:order:detail:"; // 用户端订单详情缓存 key 前缀
    public static final long ORDER_DETAIL_TTL_MINUTES = 5L; // 用户端订单详情缓存基础过期时间，单位：分钟
    public static final int ORDER_DETAIL_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端订单详情缓存随机过期时间上界，单位：分钟

    public static final String ORDER_PAGE_KEY_PREFIX = "user:order:page:"; // 用户端订单分页缓存 key 前缀
    public static final long ORDER_PAGE_TTL_MINUTES = 5L; // 用户端订单分页缓存基础过期时间，单位：分钟
    public static final int ORDER_PAGE_TTL_RANDOM_BOUND_MINUTES = 3; // 用户端订单分页缓存随机过期时间上界，单位：分钟

    public static final String ORDER_SUBMIT_KEY_PREFIX = "user:order:submit:"; // 用户端下单防重复提交 key 前缀
    public static final long ORDER_SUBMIT_TTL_SECONDS = 3L; // 用户端下单防重复提交锁过期时间，单位：秒

    public static final String ORDER_PAY_SUBMIT_KEY_PREFIX = "user:order:pay:submit:"; // 用户端支付提交防重复操作 key 前缀
    public static final long ORDER_PAY_SUBMIT_TTL_SECONDS = 3L; // 用户端支付提交防重复操作锁过期时间，单位：秒

    public static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:"; // 秒杀库存 key 前缀
    public static final String SECKILL_USER_ORDER_KEY_PREFIX = "seckill:user:order:"; // 秒杀用户商品占位 key 前缀
    public static final String SECKILL_ORDER_MAPPING_KEY_PREFIX = "seckill:order:mapping:"; // 秒杀订单映射 key 前缀
    public static final String SECKILL_RESULT_KEY_PREFIX = "seckill:result:"; // 秒杀结果 key 前缀
    public static final long SECKILL_USER_ORDER_TTL_HOURS = 24L; // 秒杀用户商品占位 key 过期时间，单位：小时
    public static final long SECKILL_ORDER_MAPPING_TTL_HOURS = 24L; // 秒杀订单映射 key 过期时间，单位：小时
    public static final long SECKILL_RESULT_TTL_HOURS = 24L; // 秒杀结果 key 过期时间，单位：小时

    public static final String USER_ME_KEY_PREFIX = "user:me:"; // 用户端当前登录信息缓存 key 前缀
    public static final String ADMIN_ME_KEY_PREFIX = "admin:me:"; // 管理端当前登录信息缓存 key 前缀
    public static final long AUTH_ME_TTL_MINUTES = 5L; // 当前登录信息缓存基础过期时间，单位：分钟
    public static final int AUTH_ME_TTL_RANDOM_BOUND_MINUTES = 3; // 当前登录信息缓存随机过期时间上界，单位：分钟

    public static final String ADMIN_USER_DETAIL_KEY_PREFIX = "admin:user:detail:"; // 管理端用户详情缓存 key 前缀
    public static final long ADMIN_USER_DETAIL_TTL_MINUTES = 10L; // 管理端用户详情缓存基础过期时间，单位：分钟
    public static final int ADMIN_USER_DETAIL_TTL_RANDOM_BOUND_MINUTES = 5; // 管理端用户详情缓存随机过期时间上界，单位：分钟

    public static final String ADMIN_MERCHANT_DETAIL_KEY_PREFIX = "admin:merchant:detail:"; // 管理端商户详情缓存 key 前缀
    public static final long ADMIN_MERCHANT_DETAIL_TTL_MINUTES = 10L; // 管理端商户详情缓存基础过期时间，单位：分钟
    public static final int ADMIN_MERCHANT_DETAIL_TTL_RANDOM_BOUND_MINUTES = 5; // 管理端商户详情缓存随机过期时间上界，单位：分钟

    public static final String ADMIN_ORDER_DETAIL_KEY_PREFIX = "admin:order:detail:"; // 管理端订单详情缓存 key 前缀
    public static final long ADMIN_ORDER_DETAIL_TTL_MINUTES = 10L; // 管理端订单详情缓存基础过期时间，单位：分钟
    public static final int ADMIN_ORDER_DETAIL_TTL_RANDOM_BOUND_MINUTES = 5; // 管理端订单详情缓存随机过期时间上界，单位：分钟

    public static final String ADMIN_REVIEW_DETAIL_KEY_PREFIX = "admin:review:detail:"; // 管理端评价详情缓存 key 前缀
    public static final long ADMIN_REVIEW_DETAIL_TTL_MINUTES = 10L; // 管理端评价详情缓存基础过期时间，单位：分钟
    public static final int ADMIN_REVIEW_DETAIL_TTL_RANDOM_BOUND_MINUTES = 5; // 管理端评价详情缓存随机过期时间上界，单位：分钟

    private CacheConstant() {
    }
}
