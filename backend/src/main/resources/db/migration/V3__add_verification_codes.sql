-- =========================================================
-- EMAIL VERIFICATION CODES (one active code per user)
-- =========================================================

CREATE TABLE verification_codes (
    user_id UUID PRIMARY KEY,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_verification_codes_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE CASCADE
);
