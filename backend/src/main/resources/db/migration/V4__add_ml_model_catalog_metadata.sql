-- =========================================================
-- DISPLAY METADATA FOR THE MODEL CATALOG (GET /api/models)
-- =========================================================

ALTER TABLE ml_models
    ADD COLUMN description VARCHAR(1000),
    ADD COLUMN device VARCHAR(255);

UPDATE ml_models
SET description = 'General-purpose ECG foundation model (fairseq-signals), fine-tuned for multi-label rhythm and conduction classification.',
    device = 'cuda:0'
WHERE model_key = 'ecg-fm';

UPDATE ml_models
SET description = 'ECGFounder foundation model: 150-label 12-lead classifier trained on large-scale clinical ECG data.',
    device = 'cuda:0'
WHERE model_key = 'ecg-founder';
