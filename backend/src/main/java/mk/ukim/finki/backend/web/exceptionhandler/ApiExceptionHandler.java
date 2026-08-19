package mk.ukim.finki.backend.web.exceptionhandler;

import mk.ukim.finki.backend.domain.exception.InvalidEcgException;
import mk.ukim.finki.backend.domain.exception.ModelInferenceException;
import mk.ukim.finki.backend.domain.exception.UploadReadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidEcgException.class)
    ProblemDetail invalidEcg(InvalidEcgException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage()
        );
        problem.setTitle("Invalid ECG upload");
        return problem;
    }

    @ExceptionHandler(ModelInferenceException.class)
    ProblemDetail modelService(ModelInferenceException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage()
        );
        problem.setTitle("Model service error");
        return problem;
    }

    @ExceptionHandler(UploadReadException.class)
    ProblemDetail uploadRead(UploadReadException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
        problem.setTitle("Could not read upload");
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail uploadTooLarge(
            MaxUploadSizeExceededException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeds the configured size limit"
        );
        problem.setTitle("Upload too large");
        return problem;
    }
}
