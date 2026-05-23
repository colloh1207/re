-- =====================================================
-- SDD Marketplace: Schema Additions
-- Run in Supabase SQL Editor AFTER supabase_schema.sql
-- =====================================================

-- -------------------------------------------------------
-- 1. Coupons Table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    discount_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE', -- PERCENTAGE | FIXED
    discount_value NUMERIC(10,2) NOT NULL,
    max_discount_amount NUMERIC(10,2),
    min_order_amount NUMERIC(10,2) DEFAULT 0,
    is_used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMPTZ,
    used_on_order_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    source_order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coupons_user_id ON coupons(user_id);
CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons(code);

ALTER TABLE coupons ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own coupons" ON coupons
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role can manage coupons" ON coupons
    FOR ALL USING (auth.role() = 'service_role');

-- -------------------------------------------------------
-- 2. Moderation Warnings Table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS moderation_warnings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_by UUID REFERENCES users(id) ON DELETE SET NULL, -- admin user id, null = auto
    reason VARCHAR(255) NOT NULL,
    details TEXT,
    warning_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_moderation_warnings_user_id ON moderation_warnings(user_id);

ALTER TABLE moderation_warnings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own warnings" ON moderation_warnings
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Admins can manage warnings" ON moderation_warnings
    FOR ALL USING (auth.role() = 'service_role');

-- -------------------------------------------------------
-- 3. User Suspensions Table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_suspensions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    suspension_type VARCHAR(20) NOT NULL DEFAULT 'TEMPORARY', -- TEMPORARY | PERMANENT
    reason TEXT NOT NULL,
    issued_by UUID REFERENCES users(id) ON DELETE SET NULL,
    suspended_until TIMESTAMPTZ, -- null means permanent
    is_permanent BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    appeal_status VARCHAR(20) NOT NULL DEFAULT 'NONE', -- NONE | PENDING | APPROVED | REJECTED
    appeal_note TEXT,
    appeal_submitted_at TIMESTAMPTZ,
    appeal_resolved_at TIMESTAMPTZ,
    admin_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_suspensions_user_id ON user_suspensions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_suspensions_active ON user_suspensions(is_active, user_id);

ALTER TABLE user_suspensions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own suspensions" ON user_suspensions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Service role can manage suspensions" ON user_suspensions
    FOR ALL USING (auth.role() = 'service_role');

-- -------------------------------------------------------
-- 4. User Privacy Settings Table
-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    show_location BOOLEAN NOT NULL DEFAULT true,
    show_bio BOOLEAN NOT NULL DEFAULT true,
    show_country BOOLEAN NOT NULL DEFAULT true,
    allow_direct_messages BOOLEAN NOT NULL DEFAULT true,
    show_online_status BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE user_privacy_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own privacy settings" ON user_privacy_settings
    FOR ALL USING (auth.uid() = user_id);

-- -------------------------------------------------------
-- 5. Add columns to existing tables
-- -------------------------------------------------------

-- Add suspension fields to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS warning_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_permanently_banned BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS suspension_reason TEXT,
    ADD COLUMN IF NOT EXISTS device_fingerprint TEXT,
    ADD COLUMN IF NOT EXISTS registration_country VARCHAR(10),
    ADD COLUMN IF NOT EXISTS kyc_required_after_appeal BOOLEAN NOT NULL DEFAULT false;

-- Add edit/unsend/delete fields to messages table
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_unsent BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS edited_at TIMESTAMPTZ;

-- Add device location fields to products table
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS device_country VARCHAR(10);

-- -------------------------------------------------------
-- 6. Function: Auto-suspend after 2 warnings
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION auto_suspend_on_warnings()
RETURNS TRIGGER AS $$
DECLARE
    warning_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO warning_count
    FROM moderation_warnings
    WHERE user_id = NEW.user_id;

    IF warning_count >= 2 THEN
        INSERT INTO user_suspensions (user_id, suspension_type, reason, suspended_until, is_permanent)
        VALUES (
            NEW.user_id,
            'TEMPORARY',
            'Automatic suspension: 2 or more warnings issued',
            NOW() + INTERVAL '2 days',
            false
        )
        ON CONFLICT DO NOTHING;

        UPDATE users
        SET suspended_until = NOW() + INTERVAL '2 days',
            warning_count = warning_count
        WHERE id = NEW.user_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_auto_suspend ON moderation_warnings;
CREATE TRIGGER trigger_auto_suspend
    AFTER INSERT ON moderation_warnings
    FOR EACH ROW
    EXECUTE FUNCTION auto_suspend_on_warnings();

-- -------------------------------------------------------
-- 7. Function: Increment product views (already exists, ensure)
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION increment_product_views(product_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE products SET view_count = COALESCE(view_count, 0) + 1 WHERE id = product_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- -------------------------------------------------------
-- 8. PostGIS extension for NearMe queries (if not enabled)
-- -------------------------------------------------------
-- CREATE EXTENSION IF NOT EXISTS postgis;

-- Function to find nearby products
CREATE OR REPLACE FUNCTION nearby_products(user_lat DOUBLE PRECISION, user_lng DOUBLE PRECISION, radius_km DOUBLE PRECISION)
RETURNS SETOF products AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM products
    WHERE latitude IS NOT NULL
      AND longitude IS NOT NULL
      AND (
          6371 * acos(
              cos(radians(user_lat)) * cos(radians(latitude)) *
              cos(radians(longitude) - radians(user_lng)) +
              sin(radians(user_lat)) * sin(radians(latitude))
          )
      ) <= radius_km
    ORDER BY (
        6371 * acos(
            cos(radians(user_lat)) * cos(radians(latitude)) *
            cos(radians(longitude) - radians(user_lng)) +
            sin(radians(user_lat)) * sin(radians(latitude))
        )
    ) ASC
    LIMIT 50;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
