-- Initial schema for ZestWear
-- Generated to match JPA entity fields (safe, minimal)

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255),
  password VARCHAR(255),
  full_name VARCHAR(255),
  role VARCHAR(50) DEFAULT 'User',
  active BOOLEAN DEFAULT true,
  access_token TEXT,
  refresh_token TEXT,
  avatar_url VARCHAR(1024),
  reset_token VARCHAR(255),
  reset_token_expiry TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON users(email);

CREATE TABLE categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  slug VARCHAR(255)
);

CREATE TABLE products (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255),
  description TEXT,
  price NUMERIC(12,2),
  image_url VARCHAR(1024),
  stock INTEGER,
  slug VARCHAR(255),
  featured BOOLEAN DEFAULT false,
  category_id BIGINT
);

CREATE TABLE product_images (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT,
  image_url VARCHAR(1024),
  position INTEGER DEFAULT 0
);

CREATE TABLE product_variants (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT,
  name VARCHAR(255),
  sku VARCHAR(255),
  price NUMERIC(12,2),
  stock INTEGER
);

CREATE TABLE order_status_history (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT,
  status VARCHAR(255),
  note TEXT,
  changed_by BIGINT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cart_items (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  product_id BIGINT,
  product_name VARCHAR(1024),
  quantity INTEGER,
  unit_price NUMERIC(12,2)
);

CREATE TABLE coupons (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(255),
  type VARCHAR(50),
  amount NUMERIC(12,2),
  active BOOLEAN DEFAULT true,
  min_order_amount NUMERIC(12,2) DEFAULT 0,
  expires_at TIMESTAMP,
  usage_limit INTEGER,
  used_count INTEGER DEFAULT 0
);

CREATE TABLE banners (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255),
  image_url VARCHAR(1024),
  link VARCHAR(1024),
  active BOOLEAN DEFAULT true,
  order_index INTEGER DEFAULT 0
);

CREATE TABLE addresses (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  full_name VARCHAR(255),
  phone VARCHAR(100),
  address_line VARCHAR(2000),
  is_default BOOLEAN DEFAULT false
);

CREATE TABLE reviews (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT,
  user_id BIGINT,
  rating INTEGER,
  comment TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT,
  status VARCHAR(100),
  total_amount NUMERIC(12,2),
  order_code VARCHAR(255),
  created_at TIMESTAMP DEFAULT NOW(),
  payment_status VARCHAR(50) DEFAULT 'Pending',
  payment_method VARCHAR(50) DEFAULT 'COD',
  paid_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT,
  product_id BIGINT,
  product_name VARCHAR(1024),
  quantity INTEGER,
  unit_price NUMERIC(12,2),
  total_price NUMERIC(12,2)
);

-- Indexes for quick lookups
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_orders_order_code ON orders(order_code);
