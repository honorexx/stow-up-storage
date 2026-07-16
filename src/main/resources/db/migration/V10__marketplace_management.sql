ALTER TABLE users ADD COLUMN owner_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN latitude DOUBLE PRECISION;
ALTER TABLE users ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE spaces ADD COLUMN lifecycle_status VARCHAR(30) NOT NULL DEFAULT 'IN_REVIEW';
ALTER TABLE spaces ADD COLUMN capacity_total INTEGER;
ALTER TABLE spaces ADD COLUMN capacity_used INTEGER NOT NULL DEFAULT 0;

UPDATE spaces
SET lifecycle_status = CASE
    WHEN moderation_status = 'REJECTED' THEN 'REJECTED'
    WHEN moderation_status = 'WITHDRAWN' THEN 'SUSPENDED'
    WHEN verified AND available THEN 'ACTIVE'
    WHEN verified AND NOT available THEN 'TEMP_UNAVAILABLE'
    ELSE 'IN_REVIEW'
END,
capacity_total = size
WHERE capacity_total IS NULL;

ALTER TABLE spaces ALTER COLUMN capacity_total SET NOT NULL;
ALTER TABLE spaces ADD CONSTRAINT ck_space_capacity_total CHECK (capacity_total > 0);
ALTER TABLE spaces ADD CONSTRAINT ck_space_capacity_used CHECK (capacity_used >= 0 AND capacity_used <= capacity_total);
CREATE INDEX idx_spaces_lifecycle_status ON spaces(lifecycle_status, city, district);

ALTER TABLE bookings ADD COLUMN end_date DATE;
ALTER TABLE bookings ADD COLUMN customer_checkout_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN owner_checkout_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE bookings ADD COLUMN reserved_capacity INTEGER;

UPDATE bookings
SET end_date = (start_date + (months || ' months')::interval)::date,
    reserved_capacity = (SELECT capacity_total FROM spaces WHERE spaces.id = bookings.space_id)
WHERE end_date IS NULL OR reserved_capacity IS NULL;

ALTER TABLE bookings ALTER COLUMN end_date SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN reserved_capacity SET NOT NULL;
CREATE INDEX idx_bookings_availability ON bookings(space_id, start_date, end_date, status);

CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_favorite_user_space UNIQUE(user_id, space_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id, created_at DESC);

CREATE TABLE rental_reviews (
    id BIGSERIAL PRIMARY KEY,
    booking_id VARCHAR(20) NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(id),
    reviewed_user_id BIGINT NOT NULL REFERENCES users(id),
    space_id BIGINT NOT NULL REFERENCES spaces(id),
    direction VARCHAR(30) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_review_booking_reviewer UNIQUE(booking_id, reviewer_id)
);
CREATE INDEX idx_reviews_space ON rental_reviews(space_id, created_at DESC);
CREATE INDEX idx_reviews_reviewed_user ON rental_reviews(reviewed_user_id, created_at DESC);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(140) NOT NULL,
    message VARCHAR(600) NOT NULL,
    reference_type VARCHAR(40),
    reference_id VARCHAR(60),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(60) NOT NULL,
    details VARCHAR(1200),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

CREATE TABLE space_documents (
    id VARCHAR(36) PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES spaces(id) ON DELETE CASCADE,
    document_type VARCHAR(40) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    file_path VARCHAR(600) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'ADMIN_ONLY',
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_space_documents_space ON space_documents(space_id, uploaded_at DESC);

UPDATE users u SET owner_verified = TRUE
WHERE EXISTS (
    SELECT 1 FROM owner_listing_documents d
    JOIN spaces s ON s.id = d.space_id
    WHERE s.owner_id = u.id AND d.status = 'APPROVED'
);
