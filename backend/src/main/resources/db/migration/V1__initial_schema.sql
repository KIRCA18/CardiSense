-- =========================================================
-- USERS
-- =========================================================

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(255),
                       last_name VARCHAR(255),
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       enabled BOOLEAN NOT NULL DEFAULT FALSE,
                       role VARCHAR(255) NOT NULL DEFAULT 'ROLE_RESEARCHER',

                       CONSTRAINT uk_users_email UNIQUE (email)
);


-- =========================================================
-- AVAILABLE MACHINE-LEARNING MODELS
-- =========================================================

CREATE TABLE ml_models (
                           id UUID PRIMARY KEY,
                           model_key VARCHAR(255) NOT NULL,
                           name VARCHAR(255) NOT NULL,
                           checkpoint VARCHAR(255) NOT NULL,
                           version VARCHAR(255) NOT NULL,
                           input_adapter VARCHAR(255) NOT NULL,
                           active BOOLEAN NOT NULL DEFAULT TRUE,

                           CONSTRAINT uk_ml_models_model_key UNIQUE (model_key)
);


-- =========================================================
-- ORIGINAL USER UPLOADS
-- =========================================================

CREATE TABLE ecg_uploads (
                             id UUID PRIMARY KEY,
                             original_filename VARCHAR(255) NOT NULL,
                             archive_format VARCHAR(255) NOT NULL,
                             archive_size BIGINT NOT NULL,
                             archive_content BYTEA NOT NULL,
                             uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL
);


-- =========================================================
-- INDIVIDUAL ECG RECORDS FOUND INSIDE AN UPLOAD
-- =========================================================

CREATE TABLE ecg_records (
                             id UUID PRIMARY KEY,
                             upload_id UUID NOT NULL,
                             record_name VARCHAR(255) NOT NULL,
                             original_format VARCHAR(255) NOT NULL,
                             sample_count INTEGER NOT NULL,
                             sampling_rate_hz DOUBLE PRECISION NOT NULL,
                             duration_seconds DOUBLE PRECISION NOT NULL,
                             uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,

                             CONSTRAINT fk_ecg_records_upload
                                 FOREIGN KEY (upload_id)
                                     REFERENCES ecg_uploads(id)
                                     ON DELETE CASCADE
);

CREATE INDEX idx_ecg_records_upload_id
    ON ecg_records(upload_id);


-- =========================================================
-- ECG LEAD NAMES
-- =========================================================

CREATE TABLE ecg_record_lead_names (
                                       ecg_record_id UUID NOT NULL,
                                       lead_index INTEGER NOT NULL,
                                       lead_name VARCHAR(255) NOT NULL,

                                       CONSTRAINT pk_ecg_record_lead_names
                                           PRIMARY KEY (ecg_record_id, lead_index),

                                       CONSTRAINT fk_lead_names_ecg_record
                                           FOREIGN KEY (ecg_record_id)
                                               REFERENCES ecg_records(id)
                                               ON DELETE CASCADE
);


-- =========================================================
-- ECG LEAD UNITS
-- =========================================================

CREATE TABLE ecg_record_units (
                                  ecg_record_id UUID NOT NULL,
                                  unit_index INTEGER NOT NULL,
                                  unit VARCHAR(255) NOT NULL,

                                  CONSTRAINT pk_ecg_record_units
                                      PRIMARY KEY (ecg_record_id, unit_index),

                                  CONSTRAINT fk_units_ecg_record
                                      FOREIGN KEY (ecg_record_id)
                                          REFERENCES ecg_records(id)
                                          ON DELETE CASCADE
);


-- =========================================================
-- ECG RECORD OWNERSHIP
-- =========================================================

CREATE TABLE ecg_record_ownership (
                                      ecg_record_id UUID PRIMARY KEY,
                                      user_id UUID NOT NULL,
                                      linked_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT fk_ownership_ecg_record
                                          FOREIGN KEY (ecg_record_id)
                                              REFERENCES ecg_records(id)
                                              ON DELETE CASCADE,

                                      CONSTRAINT fk_ownership_user
                                          FOREIGN KEY (user_id)
                                              REFERENCES users(id)
);

CREATE INDEX idx_ecg_record_ownership_user_id
    ON ecg_record_ownership(user_id);


-- =========================================================
-- MODEL ANALYSES
-- =========================================================

CREATE TABLE analyses (
                          id UUID PRIMARY KEY,
                          ecg_record_id UUID NOT NULL,
                          model_id UUID NOT NULL,
                          requested_by_user_id UUID,

                          device_used VARCHAR(255) NOT NULL,
                          checkpoint_used VARCHAR(255) NOT NULL,

                          input_lead_count INTEGER NOT NULL,
                          input_sample_count INTEGER NOT NULL,
                          input_sampling_rate_hz DOUBLE PRECISION NOT NULL,
                          input_duration_seconds DOUBLE PRECISION NOT NULL,

                          resampled_sample_count INTEGER NOT NULL,
                          segments_processed INTEGER NOT NULL,

                          status VARCHAR(255) NOT NULL DEFAULT 'PENDING',
                          started_at TIMESTAMP WITH TIME ZONE,
                          completed_at TIMESTAMP WITH TIME ZONE,
                          error_message VARCHAR(255),

                          CONSTRAINT fk_analyses_ecg_record
                              FOREIGN KEY (ecg_record_id)
                                  REFERENCES ecg_records(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_analyses_model
                              FOREIGN KEY (model_id)
                                  REFERENCES ml_models(id),

                          CONSTRAINT fk_analyses_requested_by
                              FOREIGN KEY (requested_by_user_id)
                                  REFERENCES users(id),

                          CONSTRAINT chk_analyses_status
                              CHECK (
                                  status IN (
                                             'PENDING',
                                             'RUNNING',
                                             'COMPLETED',
                                             'FAILED'
                                      )
                                  )
);

CREATE INDEX idx_analyses_ecg_record_id
    ON analyses(ecg_record_id);

CREATE INDEX idx_analyses_model_id
    ON analyses(model_id);

CREATE INDEX idx_analyses_requested_by_user_id
    ON analyses(requested_by_user_id);

CREATE INDEX idx_analyses_record_started_at
    ON analyses(ecg_record_id, started_at DESC);


-- =========================================================
-- MODEL PREDICTIONS
-- =========================================================

CREATE TABLE predictions (
                             id UUID PRIMARY KEY,
                             analysis_id UUID NOT NULL,
                             label VARCHAR(255) NOT NULL,
                             score DOUBLE PRECISION NOT NULL,
                             aggregation VARCHAR(255) NOT NULL,

                             CONSTRAINT fk_predictions_analysis
                                 FOREIGN KEY (analysis_id)
                                     REFERENCES analyses(id)
                                     ON DELETE CASCADE
);

CREATE INDEX idx_predictions_analysis_id
    ON predictions(analysis_id);


-- =========================================================
-- ANALYSIS WARNINGS
-- =========================================================

CREATE TABLE analysis_warnings (
                                   id UUID PRIMARY KEY,
                                   analysis_id UUID NOT NULL,
                                   message VARCHAR(255) NOT NULL,

                                   CONSTRAINT fk_analysis_warnings_analysis
                                       FOREIGN KEY (analysis_id)
                                           REFERENCES analyses(id)
                                           ON DELETE CASCADE
);

CREATE INDEX idx_analysis_warnings_analysis_id
    ON analysis_warnings(analysis_id);


-- =========================================================
-- INITIAL MODEL CATALOG
-- =========================================================

INSERT INTO ml_models (
    id,
    model_key,
    name,
    checkpoint,
    version,
    input_adapter,
    active
)
VALUES (
           '11111111-1111-1111-1111-111111111111',
           'ecg-fm',
           'ECG-FM',
           'configured-in-model-service',
           'prototype',
           'ecg-fm',
           TRUE
       );