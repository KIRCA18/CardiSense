INSERT INTO ml_models (id, model_key, name, checkpoint, version, input_adapter, active)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'ecg-founder',
    'ECGFounder',
    '12_lead_ECGFounder.pth',
    'prototype',
    'ecg-founder',
    TRUE
)
ON CONFLICT (model_key) DO NOTHING;
