from abc import ABC, abstractmethod

from app.schemas import AvailableModel, CommonEcgInput, InferenceResponse


class ModelAdapter(ABC):
    model_id: str
    name: str
    required_leads: list[str]
    sampling_rate_hz: int
    segment_seconds: int
    required_sample_count: int | None = None
    accepts_other_sampling_rates: bool = False
    load_error: str | None = None

    @property
    @abstractmethod
    def ready(self) -> bool: ...

    @abstractmethod
    def load(self) -> None: ...

    @abstractmethod
    def predict(self, ecg: CommonEcgInput) -> InferenceResponse: ...

    def info(self) -> AvailableModel:
        return AvailableModel(
            id=self.model_id, name=self.name,
            status="available" if self.ready else "unavailable",
            required_leads=self.required_leads,
            sampling_rate_hz=self.sampling_rate_hz,
            segment_seconds=self.segment_seconds,
            required_sample_count=self.required_sample_count,
            accepts_other_sampling_rates=self.accepts_other_sampling_rates,
            detail=self.load_error,
        )
