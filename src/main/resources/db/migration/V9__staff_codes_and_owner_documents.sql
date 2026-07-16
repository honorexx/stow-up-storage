ALTER TABLE users ADD COLUMN staff_code VARCHAR(8);

UPDATE users
SET staff_code = 'SU' || LPAD((id % 1000000)::text, 6, '0')
WHERE role IN ('SUPPORT', 'MODERATOR', 'LISTING_ANALYST', 'DOCUMENT_ANALYST', 'CONTRACT_ANALYST', 'ADMIN');

CREATE UNIQUE INDEX uk_users_staff_code ON users (staff_code) WHERE staff_code IS NOT NULL;

CREATE TABLE owner_listing_documents (
    id VARCHAR(36) PRIMARY KEY,
    space_id BIGINT NOT NULL UNIQUE REFERENCES spaces(id) ON DELETE CASCADE,
    document_code VARCHAR(30) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL,
    generated_pdf_path VARCHAR(500) NOT NULL,
    signed_pdf_path VARCHAR(500),
    original_filename VARCHAR(255),
    review_notes VARCHAR(1000),
    validator_reference VARCHAR(500),
    signer_name_matches BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_owner_listing_documents_status ON owner_listing_documents(status, created_at);
