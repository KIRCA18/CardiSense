from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

# De-identified, format-independent ECG
class CommonEcgInput(BaseModel):
    

    model_config = ConfigDict(extra="forbid")

    record_id: str = Field(min_length=1, max_length=128)
    waveform: list[list[float]] = Field(
        min_length=1,
        description="ECG waveform in leads-by-samples orientation.",
    )
    lead_names: list[str] = Field(min_length=1)
    sampling_rate_hz: float = Field(gt=0, le=10_000)
    units: list[str] = Field(
        min_length=1,
        description="One unit for all leads, or one unit per lead.",
    )

    @model_validator(mode="after")
    def validate_dimensions(self) -> "CommonEcgInput":
        lead_count = len(self.waveform)
        if len(self.lead_names) != lead_count:
            raise ValueError("lead_names length must match the waveform lead count")

        normalized_names = [name.strip().lower() for name in self.lead_names]
        if any(not name for name in normalized_names):
            raise ValueError("lead names cannot be blank")
        if len(set(normalized_names)) != len(normalized_names):
            raise ValueError("lead names must be unique")

        sample_counts = {len(lead) for lead in self.waveform}
        if len(sample_counts) != 1:
            raise ValueError("all waveform leads must contain the same number of samples")

        sample_count = next(iter(sample_counts))
        if sample_count == 0:
            raise ValueError("waveform leads cannot be empty")
        if sample_count > 300_000:
            raise ValueError("recordings above 300,000 samples per lead are not supported")

        if len(self.units) not in (1, lead_count):
            raise ValueError("units must contain one value or one value per lead")

        return self


class InferenceRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    model_id: str = Field(default="ecg-fm", min_length=1, max_length=128)
    ecg: CommonEcgInput


class Prediction(BaseModel):
    label: str
    score: float = Field(ge=0, le=1)
    aggregation: Literal["mean", "max", "none"]


class ModelInfo(BaseModel):
    id: str
    checkpoint: str
    device: str


class InputSummary(BaseModel):
    lead_count: int
    sample_count: int
    sampling_rate_hz: float
    duration_seconds: float
    resampled_sample_count: int
    segments_processed: int


class InferenceResponse(BaseModel):
    model: ModelInfo
    record_id: str
    input: InputSummary
    predictions: list[Prediction]
    warnings: list[str]


class HealthResponse(BaseModel):
    status: Literal["ready", "degraded", "not_ready"]
    models: list["AvailableModel"]


class AvailableModel(BaseModel):
    id: str
    name: str
    status: Literal["available", "unavailable"]
    required_leads: list[str]
    sampling_rate_hz: int
    segment_seconds: int
    required_sample_count: int | None = None
    accepts_other_sampling_rates: bool = False
    detail: str | None = None
