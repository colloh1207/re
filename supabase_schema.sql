-- =====================================================
-- Sdd Marketplace — Supabase PostgreSQL Schema
-- Project: fkeuioagahwqgpqjuwqj
-- =====================================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- for fast text search
CREATE EXTENSION IF NOT EXISTS "postgis";   -- for geolocation queries

-- =====================================================
-- USERS TABLE
-- =====================================================
CREATE TABLE public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL DEFAULT '',
    username TEXT UNIQUE,
    email TEXT UNIQUE,
    phone TEXT,
    avatar_url TEXT,
    bio TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    is_seller BOOLEAN DEFAULT TRUE,
    rating NUMERIC(3,2) DEFAULT 0.0,
    review_count INT DEFAULT 0,
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    product_count INT DEFAULT 0,
    sold_count INT DEFAULT 0,
    response_rate INT DEFAULT 100,
    location TEXT,
    latitude NUMERIC,
    longitude NUMERIC,
    is_online BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMPTZ,
    fcm_token TEXT,
    joined_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view all profiles"
    ON public.users FOR SELECT USING (true);

CREATE POLICY "Users can update own profile"
    ON public.users FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
    ON public.users FOR INSERT WITH CHECK (auth.uid() = id);

-- =====================================================
-- PRODUCTS TABLE
-- =====================================================
CREATE TABLE public.products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    price NUMERIC(12,2) NOT NULL,
    discount_price NUMERIC(12,2),
    currency TEXT DEFAULT 'INR',
    category TEXT NOT NULL,
    brand TEXT,
    condition TEXT NOT NULL DEFAULT 'Good',
    stock_quantity INT DEFAULT 1,
    images TEXT[] DEFAULT '{}',
    tags TEXT[] DEFAULT '{}',
    attributes JSONB DEFAULT '{}',
    seller_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    location TEXT,
    latitude NUMERIC,
    longitude NUMERIC,
    delivery_options TEXT[] DEFAULT '{}',
    return_policy TEXT,
    is_negotiable BOOLEAN DEFAULT FALSE,
    is_featured BOOLEAN DEFAULT FALSE,
    is_boosted BOOLEAN DEFAULT FALSE,
    is_new BOOLEAN DEFAULT TRUE,
    is_sold BOOLEAN DEFAULT FALSE,
    is_archived BOOLEAN DEFAULT FALSE,
    view_count INT DEFAULT 0,
    favorite_count INT DEFAULT 0,
    rating NUMERIC(3,2) DEFAULT 0.0,
    review_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_products_seller_id ON public.products(seller_id);
CREATE INDEX idx_products_category ON public.products(category);
CREATE INDEX idx_products_is_featured ON public.products(is_featured);
CREATE INDEX idx_products_created_at ON public.products(created_at DESC);
CREATE INDEX idx_products_price ON public.products(price);
CREATE INDEX idx_products_title_trgm ON public.products USING GIN (title gin_trgm_ops);
CREATE INDEX idx_products_description_trgm ON public.products USING GIN (description gin_trgm_ops);

ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view active products"
    ON public.products FOR SELECT USING (NOT is_archived);

CREATE POLICY "Sellers can insert their own products"
    ON public.products FOR INSERT WITH CHECK (auth.uid() = seller_id);

CREATE POLICY "Sellers can update their own products"
    ON public.products FOR UPDATE USING (auth.uid() = seller_id);

CREATE POLICY "Sellers can delete their own products"
    ON public.products FOR DELETE USING (auth.uid() = seller_id);

-- =====================================================
-- CHATS TABLE
-- =====================================================
CREATE TABLE public.chats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    participant1_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    participant2_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
    last_message_content TEXT,
    last_message_sent_at TIMESTAMPTZ,
    last_message_type TEXT DEFAULT 'TEXT',
    unread_count_p1 INT DEFAULT 0,
    unread_count_p2 INT DEFAULT 0,
    is_blocked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (participant1_id, participant2_id)
);

CREATE INDEX idx_chats_participant1 ON public.chats(participant1_id);
CREATE INDEX idx_chats_participant2 ON public.chats(participant2_id);
CREATE INDEX idx_chats_updated_at ON public.chats(updated_at DESC);

ALTER TABLE public.chats ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Participants can view their chats"
    ON public.chats FOR SELECT
    USING (auth.uid() = participant1_id OR auth.uid() = participant2_id);

CREATE POLICY "Authenticated users can create chats"
    ON public.chats FOR INSERT
    WITH CHECK (auth.uid() = participant1_id OR auth.uid() = participant2_id);

CREATE POLICY "Participants can update their chats"
    ON public.chats FOR UPDATE
    USING (auth.uid() = participant1_id OR auth.uid() = participant2_id);

-- =====================================================
-- MESSAGES TABLE
-- =====================================================
CREATE TABLE public.messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chat_id UUID NOT NULL REFERENCES public.chats(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL DEFAULT '',
    type TEXT DEFAULT 'TEXT' CHECK (type IN ('TEXT', 'IMAGE', 'LOCATION', 'OFFER', 'SYSTEM')),
    image_url TEXT,
    latitude NUMERIC,
    longitude NUMERIC,
    location_address TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    is_delivered BOOLEAN DEFAULT FALSE,
    edited_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_messages_chat_id ON public.messages(chat_id);
CREATE INDEX idx_messages_sender_id ON public.messages(sender_id);
CREATE INDEX idx_messages_sent_at ON public.messages(sent_at ASC);

ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Chat participants can view messages"
    ON public.messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.chats
            WHERE id = chat_id
            AND (participant1_id = auth.uid() OR participant2_id = auth.uid())
        )
    );

CREATE POLICY "Chat participants can send messages"
    ON public.messages FOR INSERT
    WITH CHECK (
        auth.uid() = sender_id AND
        EXISTS (
            SELECT 1 FROM public.chats
            WHERE id = chat_id
            AND (participant1_id = auth.uid() OR participant2_id = auth.uid())
        )
    );

CREATE POLICY "Senders can update their messages"
    ON public.messages FOR UPDATE
    USING (auth.uid() = sender_id);

-- =====================================================
-- REVIEWS TABLE
-- =====================================================
CREATE TABLE public.reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT NOT NULL DEFAULT '',
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    helpful_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (product_id, reviewer_id)
);

CREATE INDEX idx_reviews_product_id ON public.reviews(product_id);
CREATE INDEX idx_reviews_seller_id ON public.reviews(seller_id);

ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view reviews" ON public.reviews FOR SELECT USING (true);
CREATE POLICY "Authenticated users can write reviews"
    ON public.reviews FOR INSERT WITH CHECK (auth.uid() = reviewer_id);
CREATE POLICY "Reviewers can update their reviews"
    ON public.reviews FOR UPDATE USING (auth.uid() = reviewer_id);

-- =====================================================
-- FAVORITES TABLE
-- =====================================================
CREATE TABLE public.favorites (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_favorites_user_id ON public.favorites(user_id);
CREATE INDEX idx_favorites_product_id ON public.favorites(product_id);

ALTER TABLE public.favorites ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own favorites"
    ON public.favorites FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can add favorites"
    ON public.favorites FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can remove favorites"
    ON public.favorites FOR DELETE USING (auth.uid() = user_id);

-- =====================================================
-- FOLLOWERS TABLE
-- =====================================================
CREATE TABLE public.followers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    follower_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (follower_id, following_id),
    CHECK (follower_id != following_id)
);

ALTER TABLE public.followers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view followers" ON public.followers FOR SELECT USING (true);
CREATE POLICY "Users can follow others"
    ON public.followers FOR INSERT WITH CHECK (auth.uid() = follower_id);
CREATE POLICY "Users can unfollow"
    ON public.followers FOR DELETE USING (auth.uid() = follower_id);

-- =====================================================
-- NOTIFICATIONS TABLE
-- =====================================================
CREATE TABLE public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('MESSAGE','LIKE','FOLLOW','SALE','OFFER','ORDER_UPDATE','REVIEW','SYSTEM')),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data JSONB DEFAULT '{}',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON public.notifications(user_id);
CREATE INDEX idx_notifications_created_at ON public.notifications(created_at DESC);
CREATE INDEX idx_notifications_is_read ON public.notifications(is_read);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their notifications"
    ON public.notifications FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "System can insert notifications"
    ON public.notifications FOR INSERT WITH CHECK (true);
CREATE POLICY "Users can update their notifications"
    ON public.notifications FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their notifications"
    ON public.notifications FOR DELETE USING (auth.uid() = user_id);

-- =====================================================
-- BLOCKED USERS TABLE
-- =====================================================
CREATE TABLE public.blocked_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blocker_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (blocker_id, blocked_id),
    CHECK (blocker_id != blocked_id)
);

ALTER TABLE public.blocked_users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own blocks"
    ON public.blocked_users USING (auth.uid() = blocker_id);

-- =====================================================
-- REPORTS TABLE
-- =====================================================
CREATE TABLE public.reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reported_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    product_id UUID REFERENCES public.products(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'reviewed', 'resolved', 'dismissed')),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can submit reports"
    ON public.reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);

-- =====================================================
-- VERIFICATION REQUESTS TABLE
-- =====================================================
CREATE TABLE public.verification_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
    documents JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ
);

ALTER TABLE public.verification_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can view their verification requests"
    ON public.verification_requests FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can submit verification requests"
    ON public.verification_requests FOR INSERT WITH CHECK (auth.uid() = user_id);

-- =====================================================
-- FUNCTIONS & TRIGGERS
-- =====================================================

-- Auto-create user profile on auth sign up
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    v_email_prefix TEXT;
    v_base_username TEXT;
    v_username TEXT;
    v_full_name TEXT;
    v_suffix INT;
    v_exists BOOLEAN;
BEGIN
    -- Derive email prefix (strip domain)
    v_email_prefix := LOWER(SPLIT_PART(COALESCE(NEW.email, 'user'), '@', 1));
    -- Strip non-alphanumeric chars for username base
    v_base_username := REGEXP_REPLACE(v_email_prefix, '[^a-z0-9]', '', 'g');
    IF LENGTH(v_base_username) < 3 THEN v_base_username := 'user'; END IF;

    -- Capitalise for full_name if not provided via metadata
    v_full_name := COALESCE(
        NULLIF(NEW.raw_user_meta_data->>'full_name', ''),
        INITCAP(REPLACE(v_email_prefix, '.', ' '))
    );

    -- Generate unique username: base + random 4-digit suffix
    LOOP
        v_suffix := FLOOR(RANDOM() * 9000 + 1000)::INT;
        v_username := v_base_username || v_suffix::TEXT;
        SELECT EXISTS(SELECT 1 FROM public.users WHERE username = v_username) INTO v_exists;
        EXIT WHEN NOT v_exists;
    END LOOP;

    INSERT INTO public.users (id, full_name, username, email, phone)
    VALUES (
        NEW.id,
        v_full_name,
        v_username,
        NEW.email,
        NEW.phone
    )
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Increment product views
CREATE OR REPLACE FUNCTION public.increment_product_views(product_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE public.products SET view_count = view_count + 1 WHERE id = product_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Increment/decrement follower count
CREATE OR REPLACE FUNCTION public.increment_follower_count(user_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE public.users SET follower_count = follower_count + 1 WHERE id = user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.decrement_follower_count(user_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE public.users SET follower_count = GREATEST(0, follower_count - 1) WHERE id = user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Increment review helpful count
CREATE OR REPLACE FUNCTION public.increment_review_helpful(review_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE public.reviews SET helpful_count = helpful_count + 1 WHERE id = review_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Update product rating when review is added
CREATE OR REPLACE FUNCTION public.update_product_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.products
    SET
        rating = (SELECT AVG(rating) FROM public.reviews WHERE product_id = NEW.product_id),
        review_count = (SELECT COUNT(*) FROM public.reviews WHERE product_id = NEW.product_id)
    WHERE id = NEW.product_id;

    UPDATE public.users
    SET
        rating = (SELECT AVG(rating) FROM public.reviews WHERE seller_id = NEW.seller_id),
        review_count = (SELECT COUNT(*) FROM public.reviews WHERE seller_id = NEW.seller_id)
    WHERE id = NEW.seller_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_review_created
    AFTER INSERT OR UPDATE ON public.reviews
    FOR EACH ROW EXECUTE FUNCTION public.update_product_rating();

-- Update product count when product is added/removed
CREATE OR REPLACE FUNCTION public.update_seller_product_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.users SET product_count = product_count + 1 WHERE id = NEW.seller_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.users SET product_count = GREATEST(0, product_count - 1) WHERE id = OLD.seller_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_product_changed
    AFTER INSERT OR DELETE ON public.products
    FOR EACH ROW EXECUTE FUNCTION public.update_seller_product_count();

-- Update favorite count
CREATE OR REPLACE FUNCTION public.update_favorite_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.products SET favorite_count = favorite_count + 1 WHERE id = NEW.product_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.products SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = OLD.product_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_favorite_changed
    AFTER INSERT OR DELETE ON public.favorites
    FOR EACH ROW EXECUTE FUNCTION public.update_favorite_count();

-- =====================================================
-- REALTIME SUBSCRIPTIONS
-- Enable realtime for relevant tables
-- =====================================================
/*
ALTER PUBLICATION supabase_realtime ADD TABLE public.messages;
ALTER PUBLICATION supabase_realtime ADD TABLE public.chats;
ALTER PUBLICATION supabase_realtime ADD TABLE public.notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE public.products;
*/

-- =====================================================
-- STORAGE BUCKETS
-- Run these in the Supabase Dashboard → Storage
-- =====================================================
-- INSERT INTO storage.buckets (id, name, public) VALUES ('product-images', 'product-images', true);
-- INSERT INTO storage.buckets (id, name, public) VALUES ('avatars', 'avatars', true);
-- INSERT INTO storage.buckets (id, name, public) VALUES ('chat-images', 'chat-images', false);

-- Storage policies
-- CREATE POLICY "Anyone can view product images" ON storage.objects FOR SELECT USING (bucket_id = 'product-images');
-- CREATE POLICY "Sellers can upload product images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'product-images' AND auth.role() = 'authenticated');
-- CREATE POLICY "Anyone can view avatars" ON storage.objects FOR SELECT USING (bucket_id = 'avatars');
-- CREATE POLICY "Users can upload their avatar" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'avatars' AND auth.uid()::text = (storage.foldername(name))[1]);
-- CREATE POLICY "Chat participants can view images" ON storage.objects FOR SELECT USING (bucket_id = 'chat-images' AND auth.role() = 'authenticated');
-- CREATE POLICY "Chat participants can upload images" ON storage.objects FOR INSERT WITH CHECK (bucket_id = 'chat-images' AND auth.role() = 'authenticated');

-- =====================================================
-- ORDERS TABLE
-- =====================================================
CREATE TABLE public.orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES public.products(id) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    currency TEXT DEFAULT 'INR',
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending','confirmed','processing','shipped','delivered','cancelled','returned','disputed')),
    payment_status TEXT NOT NULL DEFAULT 'pending'
        CHECK (payment_status IN ('pending','paid','failed','refunded')),
    payment_method_id UUID,
    payment_gateway TEXT,
    payment_transaction_id TEXT,
    shipping_address JSONB,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    tracking_number TEXT,
    courier_name TEXT,
    buyer_terms_accepted BOOLEAN DEFAULT FALSE,
    buyer_terms_accepted_at TIMESTAMPTZ,
    notes TEXT,
    cancellation_reason TEXT,
    return_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_orders_buyer ON public.orders(buyer_id);
CREATE INDEX idx_orders_seller ON public.orders(seller_id);
CREATE INDEX idx_orders_product ON public.orders(product_id);
CREATE INDEX idx_orders_status ON public.orders(status);
CREATE INDEX idx_orders_created_at ON public.orders(created_at DESC);

ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Buyers and sellers can view their orders" ON public.orders FOR SELECT USING (auth.uid() = buyer_id OR auth.uid() = seller_id);
CREATE POLICY "Buyers can create orders" ON public.orders FOR INSERT WITH CHECK (auth.uid() = buyer_id);
CREATE POLICY "Buyers and sellers can update their orders" ON public.orders FOR UPDATE USING (auth.uid() = buyer_id OR auth.uid() = seller_id);

-- =====================================================
-- ORDER TRACKING TABLE
-- =====================================================
CREATE TABLE public.order_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES public.orders(id) ON DELETE CASCADE,
    status TEXT NOT NULL,
    description TEXT,
    location TEXT,
    latitude NUMERIC,
    longitude NUMERIC,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES public.users(id)
);

CREATE INDEX idx_order_tracking_order ON public.order_tracking(order_id);
ALTER TABLE public.order_tracking ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Order participants can view tracking" ON public.order_tracking FOR SELECT USING (
    EXISTS (SELECT 1 FROM public.orders WHERE id = order_id AND (buyer_id = auth.uid() OR seller_id = auth.uid()))
);

-- =====================================================
-- BLOCKS TABLE
-- =====================================================
CREATE TABLE public.blocks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    blocker_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (blocker_id, blocked_id)
);

CREATE INDEX idx_blocks_blocker ON public.blocks(blocker_id);
CREATE INDEX idx_blocks_blocked ON public.blocks(blocked_id);
ALTER TABLE public.blocks ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own blocks" ON public.blocks FOR ALL USING (auth.uid() = blocker_id);

/*
-- =====================================================
-- REPORTS TABLE
-- =====================================================
CREATE TABLE public.reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    reported_user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    reported_product_id UUID REFERENCES public.products(id) ON DELETE CASCADE,
    category TEXT NOT NULL CHECK (category IN ('spam','fraud','inappropriate','counterfeit','harassment','other')),
    description TEXT,
    evidence_urls TEXT[] DEFAULT '{}',
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending','investigating','resolved','dismissed')),
    resolution TEXT,
    resolved_by UUID REFERENCES public.users(id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_reports_reporter ON public.reports(reporter_id);
CREATE INDEX idx_reports_reported_user ON public.reports(reported_user_id);
CREATE INDEX idx_reports_reported_product ON public.reports(reported_product_id);
CREATE INDEX idx_reports_status ON public.reports(status);
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can create reports" ON public.reports FOR INSERT WITH CHECK (auth.uid() = reporter_id);
CREATE POLICY "Users can view their own reports" ON public.reports FOR SELECT USING (auth.uid() = reporter_id);
*/

-- =====================================================
-- KYC DOCUMENTS TABLE
-- =====================================================
CREATE TABLE public.kyc_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    document_type TEXT NOT NULL CHECK (document_type IN ('aadhar','pan','passport','driving_license','voter_id')),
    document_number TEXT,
    front_url TEXT NOT NULL,
    back_url TEXT,
    selfie_url TEXT,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending','under_review','approved','rejected')),
    rejection_reason TEXT,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by UUID REFERENCES public.users(id),
    UNIQUE (user_id, document_type)
);

CREATE INDEX idx_kyc_user ON public.kyc_documents(user_id);
CREATE INDEX idx_kyc_status ON public.kyc_documents(status);
ALTER TABLE public.kyc_documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own KYC" ON public.kyc_documents FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- REVIEW REPLIES TABLE
-- =====================================================
CREATE TABLE public.review_replies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id UUID NOT NULL REFERENCES public.reviews(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_seller BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_review_replies_review ON public.review_replies(review_id);
ALTER TABLE public.review_replies ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can view review replies" ON public.review_replies FOR SELECT USING (true);
CREATE POLICY "Authenticated users can reply" ON public.review_replies FOR INSERT WITH CHECK (auth.uid() = author_id);
CREATE POLICY "Authors can update their replies" ON public.review_replies FOR UPDATE USING (auth.uid() = author_id);

-- =====================================================
-- REVIEW HELPFUL TABLE
-- =====================================================
CREATE TABLE public.review_helpful (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    review_id UUID NOT NULL REFERENCES public.reviews(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (review_id, user_id)
);

CREATE INDEX idx_review_helpful_review ON public.review_helpful(review_id);
ALTER TABLE public.review_helpful ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can view helpful votes" ON public.review_helpful FOR SELECT USING (true);
CREATE POLICY "Authenticated users can mark helpful" ON public.review_helpful FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- PAYMENT METHODS TABLE
-- =====================================================
CREATE TABLE public.payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('upi','card','netbanking','wallet','cod')),
    display_name TEXT NOT NULL,
    upi_id TEXT,
    card_last4 TEXT,
    card_brand TEXT,
    card_expiry TEXT,
    bank_name TEXT,
    wallet_name TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    gateway_token TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_user ON public.payment_methods(user_id);
ALTER TABLE public.payment_methods ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own payment methods" ON public.payment_methods FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- DEVICE ACCOUNTS TABLE
-- =====================================================
CREATE TABLE public.device_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (device_id, user_id)
);

CREATE INDEX idx_device_accounts_device ON public.device_accounts(device_id);
ALTER TABLE public.device_accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Device account insert by auth" ON public.device_accounts FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can view their own devices" ON public.device_accounts FOR SELECT USING (auth.uid() = user_id);

-- =====================================================
-- APP RATINGS TABLE
-- =====================================================
CREATE TABLE public.app_ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    feedback TEXT,
    app_version TEXT,
    platform TEXT DEFAULT 'android',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id)
);

ALTER TABLE public.app_ratings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own app ratings" ON public.app_ratings FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- BUG REPORTS TABLE
-- =====================================================
CREATE TABLE public.bug_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    steps_to_reproduce TEXT,
    expected_behavior TEXT,
    actual_behavior TEXT,
    severity TEXT DEFAULT 'medium' CHECK (severity IN ('low','medium','high','critical')),
    status TEXT DEFAULT 'open' CHECK (status IN ('open','triaged','in_progress','fixed','wont_fix','duplicate')),
    app_version TEXT,
    device_model TEXT,
    os_version TEXT,
    screenshots TEXT[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_bug_reports_user ON public.bug_reports(user_id);
CREATE INDEX idx_bug_reports_status ON public.bug_reports(status);
ALTER TABLE public.bug_reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users can create and view their bug reports" ON public.bug_reports FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- SUPPORT TICKETS TABLE
-- =====================================================
CREATE TABLE public.support_tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    order_id UUID REFERENCES public.orders(id) ON DELETE SET NULL,
    subject TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('payment','order','product','account','other')),
    description TEXT NOT NULL,
    status TEXT DEFAULT 'open' CHECK (status IN ('open','assigned','in_progress','resolved','closed')),
    priority TEXT DEFAULT 'normal' CHECK (priority IN ('low','normal','high','urgent')),
    messages JSONB DEFAULT '[]',
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_support_tickets_user ON public.support_tickets(user_id);
CREATE INDEX idx_support_tickets_status ON public.support_tickets(status);
ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users manage their own support tickets" ON public.support_tickets FOR ALL USING (auth.uid() = user_id);

-- =====================================================
-- Add KYC fields to users
-- =====================================================
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS kyc_status TEXT DEFAULT 'none' CHECK (kyc_status IN ('none','pending','under_review','approved','rejected'));
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_suspended BOOLEAN DEFAULT FALSE;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS language TEXT DEFAULT 'en';
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS blocked_users UUID[] DEFAULT '{}';

-- =====================================================
-- FUNCTIONS & TRIGGERS
-- =====================================================

-- Increment product view count
CREATE OR REPLACE FUNCTION increment_view_count(product_uuid UUID)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.products SET view_count = view_count + 1 WHERE id = product_uuid;
END;
$$;

-- Update review helpful count
CREATE OR REPLACE FUNCTION update_review_helpful_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.reviews SET helpful_count = helpful_count + 1 WHERE id = NEW.review_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.reviews SET helpful_count = GREATEST(0, helpful_count - 1) WHERE id = OLD.review_id;
    END IF;
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_review_helpful_count
AFTER INSERT OR DELETE ON public.review_helpful
FOR EACH ROW EXECUTE FUNCTION update_review_helpful_count();

-- Update product rating on review insert/update/delete
CREATE OR REPLACE FUNCTION update_product_rating()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.products SET
        rating = (SELECT COALESCE(AVG(rating::NUMERIC), 0) FROM public.reviews WHERE product_id = COALESCE(NEW.product_id, OLD.product_id)),
        review_count = (SELECT COUNT(*) FROM public.reviews WHERE product_id = COALESCE(NEW.product_id, OLD.product_id))
    WHERE id = COALESCE(NEW.product_id, OLD.product_id);
    RETURN COALESCE(NEW, OLD);
END;
$$;

CREATE TRIGGER trg_update_product_rating
AFTER INSERT OR UPDATE OR DELETE ON public.reviews
FOR EACH ROW EXECUTE FUNCTION update_product_rating();

-- Auto-update updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON public.users FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_products_updated_at BEFORE UPDATE ON public.products FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON public.orders FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Realtime subscriptions
ALTER publication supabase_realtime ADD TABLE public.messages;
ALTER publication supabase_realtime ADD TABLE public.chats;
ALTER publication supabase_realtime ADD TABLE public.notifications;
ALTER publication supabase_realtime ADD TABLE public.products;
ALTER publication supabase_realtime ADD TABLE public.orders;
ALTER publication supabase_realtime ADD TABLE public.order_tracking;

-- =====================================================================
-- BOOST SALES SYSTEM
-- =====================================================================

-- Boost tiers (static reference, seeded below)
CREATE TABLE IF NOT EXISTS public.boost_tiers (
    id            text PRIMARY KEY,
    name          text NOT NULL,
    impressions   int  NOT NULL,
    duration_days int  NOT NULL,
    price_usd     decimal(10,2) NOT NULL,
    features      jsonb  DEFAULT '[]',
    is_popular    boolean DEFAULT false,
    badge_label   text    DEFAULT '',
    sort_order    int     NOT NULL,
    created_at    timestamptz DEFAULT now()
);

INSERT INTO public.boost_tiers
    (id, name, impressions, duration_days, price_usd, features, is_popular, badge_label, sort_order) VALUES
  ('starter',  'Starter',  500,    3,  1.99,  '["500 guaranteed impressions","3 days active","Category placement","Basic view analytics"]',                                                                                        false, '',             1),
  ('basic',    'Basic',    2500,   7,  4.99,  '["2,500 guaranteed impressions","7 days active","Priority category placement","Click & view tracking","2× search ranking boost"]',                                                   false, '',             2),
  ('standard', 'Standard', 7500,  14,  9.99,  '["7,500 guaranteed impressions","14 days active","Homepage featured row","Full analytics dashboard","3× search ranking boost","Boosted badge on listing"]',                           true,  'Most Popular', 3),
  ('premium',  'Premium',  20000, 30, 24.99,  '["20,000 guaranteed impressions","30 days active","Top-of-feed placement","Real-time analytics","5× search ranking boost","Boost + Verified badges","Push to followers"]',            false, 'Best Value',   4),
  ('business', 'Business', 75000, 60, 49.99,  '["75,000 guaranteed impressions","60 days active","Sticky homepage banner","Priority support","8× search ranking boost","All premium badges","Push blast to nearby users"]',           false, '',             5),
  ('elite',    'Elite',   250000, 90, 99.99,  '["250,000 guaranteed impressions","90 days active","App splash placement","Dedicated account manager","10× search ranking boost","All badges + Gold frame","SMS + push blast"]',       false, 'Elite',        6)
ON CONFLICT (id) DO NOTHING;

-- Active boosts
CREATE TABLE IF NOT EXISTS public.boosts (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               uuid NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    product_ids           uuid[] NOT NULL DEFAULT '{}',
    tier_id               text  NOT NULL REFERENCES public.boost_tiers(id),
    tier_name             text  NOT NULL DEFAULT '',
    status                text  NOT NULL DEFAULT 'pending'
                          CHECK (status IN ('pending','active','expired','cancelled')),
    payment_status        text  NOT NULL DEFAULT 'pending'
                          CHECK (payment_status IN ('pending','paid','failed','refunded')),
    payment_reference     text,
    paystack_auth_url     text,
    currency              text  NOT NULL DEFAULT 'USD',
    amount_paid           decimal(12,2) DEFAULT 0,
    views_count           int DEFAULT 0,
    impressions_guaranteed int DEFAULT 0,
    started_at            timestamptz,
    expires_at            timestamptz,
    created_at            timestamptz DEFAULT now(),
    updated_at            timestamptz DEFAULT now()
);

-- Add boost columns to products
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS is_boosted    boolean     DEFAULT false;
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS boosted_until timestamptz;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_boosts_user_id     ON public.boosts(user_id);
CREATE INDEX IF NOT EXISTS idx_boosts_status       ON public.boosts(status);
CREATE INDEX IF NOT EXISTS idx_boosts_payment_ref  ON public.boosts(payment_reference);
CREATE INDEX IF NOT EXISTS idx_products_boosted    ON public.products(is_boosted);

-- RLS
ALTER TABLE public.boosts ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users view own boosts"        ON public.boosts FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users insert own boosts"      ON public.boosts FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own boosts"      ON public.boosts FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Service role full boosts"     ON public.boosts USING (auth.role() = 'service_role');

-- Auto-expire trigger (catches stale boosts on any update)
CREATE OR REPLACE FUNCTION expire_boost_if_due()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.expires_at IS NOT NULL AND NEW.expires_at < NOW() AND NEW.status = 'active' THEN
        NEW.status := 'expired';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_expire_boosts
BEFORE UPDATE ON public.boosts
FOR EACH ROW EXECUTE FUNCTION expire_boost_if_due();

CREATE TRIGGER trg_boosts_updated_at
BEFORE UPDATE ON public.boosts
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

ALTER publication supabase_realtime ADD TABLE public.boosts;

-- =====================================================
-- TELEGRAM CONNECTIONS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS public.telegram_connections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    encrypted_token TEXT,
    bot_username TEXT,
    bot_id TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    connected_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

ALTER TABLE public.telegram_connections ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own telegram connection"
    ON public.telegram_connections FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Service role can manage telegram connections"
    ON public.telegram_connections FOR ALL
    USING (true);

CREATE TRIGGER trg_telegram_connections_updated_at
BEFORE UPDATE ON public.telegram_connections
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =====================================================
-- AI MODERATION: MESSAGE WARNINGS
-- =====================================================
ALTER TABLE public.messages
    ADD COLUMN IF NOT EXISTS is_flagged BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS flag_reason TEXT;

CREATE TABLE IF NOT EXISTS public.message_warnings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    message_id UUID REFERENCES public.messages(id) ON DELETE SET NULL,
    chat_id UUID REFERENCES public.chats(id) ON DELETE SET NULL,
    sender_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    threat_type TEXT,
    confidence FLOAT DEFAULT 0,
    reason TEXT,
    is_scam BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.message_warnings ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Service role manages warnings" ON public.message_warnings FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Users view own warnings" ON public.message_warnings FOR SELECT USING (auth.uid() = sender_id);
CREATE INDEX IF NOT EXISTS idx_message_warnings_sender ON public.message_warnings(sender_id);
CREATE INDEX IF NOT EXISTS idx_message_warnings_scam ON public.message_warnings(is_scam, created_at);

-- =====================================================
-- AI MODERATION: PRODUCT MODERATION LOGS
-- =====================================================
ALTER TABLE public.products
    ADD COLUMN IF NOT EXISTS moderation_status TEXT DEFAULT 'pending' CHECK (moderation_status IN ('pending','approved','under_review','removed')),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS public.product_moderation_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID REFERENCES public.products(id) ON DELETE SET NULL,
    seller_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    is_illegal BOOLEAN DEFAULT FALSE,
    is_suspicious BOOLEAN DEFAULT FALSE,
    violation_type TEXT,
    confidence FLOAT DEFAULT 0,
    reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.product_moderation_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Service role manages product logs" ON public.product_moderation_logs FOR ALL USING (auth.role() = 'service_role');
CREATE POLICY "Sellers can view own logs" ON public.product_moderation_logs FOR SELECT USING (auth.uid() = seller_id AND auth.role() = 'authenticated');
CREATE INDEX IF NOT EXISTS idx_product_mod_logs_seller ON public.product_moderation_logs(seller_id);
CREATE INDEX IF NOT EXISTS idx_product_mod_logs_illegal ON public.product_moderation_logs(is_illegal, created_at);

-- =====================================================
-- USERS: KYC FLAG FOR RE-VERIFICATION
-- =====================================================
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS kyc_flagged BOOLEAN DEFAULT FALSE;

-- =====================================================
-- WEBHOOK NOTES (configure in Supabase Dashboard)
-- moderate-message: Table=messages, Event=INSERT, URL={SUPABASE_URL}/functions/v1/moderate-message
-- moderate-product: Table=products, Event=INSERT, URL={SUPABASE_URL}/functions/v1/moderate-product
-- =====================================================
