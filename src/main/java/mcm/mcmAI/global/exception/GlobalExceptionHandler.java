package mcm.mcmAI.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request
    ) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        ErrorCode errorCode = resolveErrorCode(e.getBindingResult().getFieldError());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request
    ) {
        ErrorCode errorCode = resolveErrorCode(e);
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpServletRequest request
    ) {
        ErrorCode errorCode = "sessionId".equals(e.getParameterName())
                ? ErrorCode.MISSING_SESSION_ID
                : ErrorCode.INVALID_RESPONSE_KEY;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, request.getRequestURI()));
    }

    private ErrorCode resolveErrorCode(FieldError fieldError) {
        if (fieldError == null) {
            return ErrorCode.INVALID_RESPONSE_KEY;
        }
        return switch (fieldError.getField()) {
            case "email" -> ErrorCode.INVALID_EMAIL;
            case "nickname" -> ErrorCode.INVALID_NICKNAME;
            case "purposeType" -> ErrorCode.INVALID_PURPOSE;
            default -> ErrorCode.INVALID_RESPONSE_KEY;
        };
    }

    private ErrorCode resolveErrorCode(ConstraintViolationException e) {
        boolean isInterestType = e.getConstraintViolations().stream()
                .anyMatch(v -> v.getPropertyPath().toString().contains("interestType"));
        if (isInterestType) {
            return ErrorCode.INVALID_INTEREST_TYPE;
        }
        return ErrorCode.INVALID_RESPONSE_KEY;
    }
}