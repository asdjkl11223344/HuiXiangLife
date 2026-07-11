CREATE DATABASE IF NOT EXISTS huixiang_life DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE huixiang_life;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL COMMENT '主键ID',
  phone VARCHAR(20) NOT NULL COMMENT '手机号',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  nickname VARCHAR(64) NOT NULL COMMENT '昵称',
  avatar VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  role VARCHAR(20) NOT NULL COMMENT '角色: ADMIN/USER',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_phone (phone),
  KEY idx_sys_user_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS merchant_category (
  id BIGINT NOT NULL COMMENT '主键ID',
  name VARCHAR(64) NOT NULL COMMENT '分类名称',
  sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_merchant_category_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户分类表';

CREATE TABLE IF NOT EXISTS merchant (
  id BIGINT NOT NULL COMMENT '主键ID',
  name VARCHAR(128) NOT NULL COMMENT '商户名称',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  address VARCHAR(255) DEFAULT NULL COMMENT '商户地址',
  phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  description TEXT COMMENT '商户简介',
  score DECIMAL(3,1) NOT NULL DEFAULT 5.0 COMMENT '评分',
  avg_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '人均价格',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_merchant_category_status (category_id, status),
  KEY idx_merchant_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

CREATE TABLE IF NOT EXISTS product (
  id BIGINT NOT NULL COMMENT '主键ID',
  merchant_id BIGINT NOT NULL COMMENT '商户ID',
  name VARCHAR(128) NOT NULL COMMENT '商品名称',
  sub_title VARCHAR(255) DEFAULT NULL COMMENT '商品副标题',
  content TEXT COMMENT '商品详情',
  cover_url VARCHAR(255) DEFAULT NULL COMMENT '封面图地址',
  origin_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '原价',
  sale_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '售价',
  stock INT NOT NULL DEFAULT 0 COMMENT '库存',
  sold_count INT NOT NULL DEFAULT 0 COMMENT '销量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0下架 1上架',
  start_time DATETIME DEFAULT NULL COMMENT '上架开始时间',
  end_time DATETIME DEFAULT NULL COMMENT '上架结束时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_product_merchant_status (merchant_id, status),
  KEY idx_product_name (name),
  KEY idx_product_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT NOT NULL COMMENT '主键ID',
  name VARCHAR(128) NOT NULL COMMENT '优惠券名称',
  type TINYINT NOT NULL DEFAULT 1 COMMENT '优惠券类型',
  discount_type TINYINT NOT NULL DEFAULT 1 COMMENT '优惠方式',
  discount_value DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠值',
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛金额',
  stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  limit_per_user INT NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  merchant_id BIGINT DEFAULT NULL COMMENT '商户ID',
  product_id BIGINT DEFAULT NULL COMMENT '商品ID',
  start_time DATETIME NOT NULL COMMENT '生效时间',
  end_time DATETIME NOT NULL COMMENT '失效时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1启用',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_coupon_template_status_time (status, start_time, end_time),
  KEY idx_coupon_template_merchant_product (merchant_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券模板表';

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT NOT NULL COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  coupon_template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未使用 1已使用 2已过期',
  receive_time DATETIME NOT NULL COMMENT '领取时间',
  use_time DATETIME DEFAULT NULL COMMENT '使用时间',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  order_id BIGINT DEFAULT NULL COMMENT '关联订单ID',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_user_coupon_user_status (user_id, status),
  KEY idx_user_coupon_template (coupon_template_id),
  KEY idx_user_coupon_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户优惠券表';

CREATE TABLE IF NOT EXISTS order_info (
  id BIGINT NOT NULL COMMENT '主键ID',
  order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  merchant_id BIGINT NOT NULL COMMENT '商户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  coupon_id BIGINT DEFAULT NULL COMMENT '使用的优惠券ID',
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待支付 1已支付 2已取消 3已完成 4已退款',
  remark VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
  finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_info_order_no (order_no),
  KEY idx_order_info_user_status (user_id, status),
  KEY idx_order_info_merchant_status (merchant_id, status),
  KEY idx_order_info_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT NOT NULL COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
  product_cover VARCHAR(255) DEFAULT NULL COMMENT '商品封面图',
  sale_price DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '成交单价',
  quantity INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '小计金额',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

CREATE TABLE IF NOT EXISTS payment_record (
  id BIGINT NOT NULL COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道',
  pay_status TINYINT NOT NULL DEFAULT 0 COMMENT '支付状态: 0待处理 1成功 2失败',
  transaction_no VARCHAR(100) DEFAULT NULL COMMENT '支付流水号',
  callback_content TEXT COMMENT '支付回调内容',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_payment_record_order (order_id),
  KEY idx_payment_record_transaction (transaction_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE IF NOT EXISTS refund_record (
  id BIGINT NOT NULL COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  refund_no VARCHAR(64) NOT NULL COMMENT '退款编号',
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '退款金额',
  refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '退款状态: 0已申请 1成功 2失败',
  reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
  apply_time DATETIME NOT NULL COMMENT '申请时间',
  refund_time DATETIME DEFAULT NULL COMMENT '退款时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_refund_record_refund_no (refund_no),
  KEY idx_refund_record_order (order_id),
  KEY idx_refund_record_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

CREATE TABLE IF NOT EXISTS review (
  id BIGINT NOT NULL COMMENT '主键ID',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  merchant_id BIGINT NOT NULL COMMENT '商户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  score TINYINT NOT NULL COMMENT '评分',
  content VARCHAR(1000) DEFAULT NULL COMMENT '评价内容',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0隐藏 1可见',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_review_product_status (product_id, status),
  KEY idx_review_merchant_status (merchant_id, status),
  KEY idx_review_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT NOT NULL COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  target_id BIGINT NOT NULL COMMENT '收藏目标ID',
  target_type TINYINT NOT NULL COMMENT '收藏目标类型: 1商户 2商品',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_user_target (user_id, target_id, target_type, deleted),
  KEY idx_favorite_target (target_id, target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS search_log (
  id BIGINT NOT NULL COMMENT '主键ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  keyword VARCHAR(100) NOT NULL COMMENT '搜索关键词',
  search_time DATETIME NOT NULL COMMENT '搜索时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_search_log_keyword_time (keyword, search_time),
  KEY idx_search_log_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索日志表';

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL COMMENT '主键ID',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  module VARCHAR(64) NOT NULL COMMENT '操作模块',
  action VARCHAR(32) NOT NULL COMMENT '操作动作',
  biz_id BIGINT DEFAULT NULL COMMENT '业务ID',
  detail VARCHAR(1000) DEFAULT NULL COMMENT '操作详情',
  ip VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_operation_log_operator_time (operator_id, create_time),
  KEY idx_operation_log_module_action (module, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端操作日志表';

CREATE TABLE IF NOT EXISTS mq_consume_log (
  id BIGINT NOT NULL COMMENT '主键ID',
  msg_id VARCHAR(100) NOT NULL COMMENT '消息ID',
  biz_type VARCHAR(64) NOT NULL COMMENT '业务类型',
  biz_key VARCHAR(100) NOT NULL COMMENT '业务唯一键',
  consume_status TINYINT NOT NULL DEFAULT 1 COMMENT '消费状态',
  consume_time DATETIME DEFAULT NULL COMMENT '消费时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_mq_consume_log_msg_id (msg_id),
  KEY idx_mq_consume_log_biz (biz_type, biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ消费日志表';

CREATE TABLE IF NOT EXISTS idempotent_record (
  id BIGINT NOT NULL COMMENT '主键ID',
  idempotent_key VARCHAR(128) NOT NULL COMMENT '幂等键',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  api_uri VARCHAR(255) NOT NULL COMMENT '接口地址',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态',
  expire_time DATETIME NOT NULL COMMENT '过期时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotent_record_key (idempotent_key),
  KEY idx_idempotent_record_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口幂等记录表';

INSERT INTO sys_user (id, phone, password, nickname, avatar, role, status, create_time, update_time, deleted) VALUES
(1000000000000000001, '13800000000', '$2b$10$36Xj2dirQ7JBhbNmI.psYuJRqazfCiezMt1wXcjEdyoactoFny/uC', '系统管理员', '', 'ADMIN', 1, NOW(), NOW(), 0),
(1000000000000000002, '13900000000', '$2b$10$36Xj2dirQ7JBhbNmI.psYuJRqazfCiezMt1wXcjEdyoactoFny/uC', '演示用户', '', 'USER', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role), status = VALUES(status), deleted = 0;

INSERT INTO merchant_category (id, name, sort, status, create_time, update_time, deleted) VALUES
(2000000000000000001, '美食', 10, 1, NOW(), NOW(), 0),
(2000000000000000002, '休闲娱乐', 20, 1, NOW(), NOW(), 0),
(2000000000000000003, '丽人', 30, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort = VALUES(sort), status = VALUES(status), deleted = 0;

INSERT INTO merchant (id, name, category_id, cover_url, address, phone, description, score, avg_price, status, create_time, update_time, deleted) VALUES
(3000000000000000001, '惠享川味小馆', 2000000000000000001, '', '成都市高新区天府三街 88 号', '028-88888888', '主打川味套餐和本地特色小吃，适合工作餐和朋友聚餐。', 4.8, 48.00, 1, NOW(), NOW(), 0),
(3000000000000000002, '惠享轻食咖啡', 2000000000000000001, '', '成都市锦江区春熙路 66 号', '028-66666666', '提供咖啡、轻食、甜品和下午茶套餐。', 4.7, 36.00, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), category_id = VALUES(category_id), status = VALUES(status), deleted = 0;

INSERT INTO product (id, merchant_id, name, sub_title, content, cover_url, origin_price, sale_price, stock, sold_count, status, start_time, end_time, create_time, update_time, deleted) VALUES
(4000000000000000001, 3000000000000000001, '双人川味套餐', '招牌菜组合，适合双人到店消费', '包含招牌麻婆豆腐、宫保鸡丁、时蔬、米饭和饮品。', '', 128.00, 88.00, 200, 35, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
(4000000000000000002, 3000000000000000002, '咖啡轻食单人餐', '工作日下午茶优选', '包含拿铁一杯、鸡肉沙拉一份和甜品一份。', '', 68.00, 49.90, 150, 22, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW(), 0),
(4000000000000000003, 3000000000000000001, '限时秒杀酸菜鱼套餐', '限量秒杀，库存预热后可参与抢购', '酸菜鱼双人套餐，适合作为秒杀链路演示商品。', '', 168.00, 59.90, 50, 0, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), merchant_id = VALUES(merchant_id), stock = VALUES(stock), status = VALUES(status), deleted = 0;

INSERT INTO coupon_template (id, name, type, discount_type, discount_value, threshold_amount, stock, limit_per_user, merchant_id, product_id, start_time, end_time, status, create_time, update_time, deleted) VALUES
(5000000000000000001, '满80减20通用券', 1, 1, 20.00, 80.00, 1000, 1, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW(), NOW(), 0),
(5000000000000000002, '川味小馆满100减30', 1, 1, 30.00, 100.00, 300, 1, 3000000000000000001, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), stock = VALUES(stock), status = VALUES(status), deleted = 0;

SET FOREIGN_KEY_CHECKS = 1;

