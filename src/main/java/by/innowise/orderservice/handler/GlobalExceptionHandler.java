package by.innowise.orderservice.handler;

import by.innowise.orderservice.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleResourceNotFound(
      ResourceNotFoundException exception,
      HttpServletRequest request
  ) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        exception.getMessage()
    );

    problemDetail.setTitle("Resource not found");
    addCommonProperties(problemDetail, request);

    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    Map<String, String> errors = new LinkedHashMap<>();

    exception.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(
            error.getField(),
            error.getDefaultMessage()
        ));

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Request validation failed"
    );

    problemDetail.setTitle("Validation failed");
    problemDetail.setProperty("errors", errors);
    addCommonProperties(problemDetail, request);

    return problemDetail;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      ConstraintViolationException exception,
      HttpServletRequest request
  ) {
    Map<String, String> errors = new LinkedHashMap<>();

    exception.getConstraintViolations()
        .forEach(violation -> errors.put(
            violation.getPropertyPath().toString(),
            violation.getMessage()
        ));

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Request parameter validation failed"
    );

    problemDetail.setTitle("Validation failed");
    problemDetail.setProperty("errors", errors);
    addCommonProperties(problemDetail, request);

    return problemDetail;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleHttpMessageNotReadable(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Request body is malformed"
    );
    problemDetail.setTitle("Malformed request");
    addCommonProperties(problemDetail, request);
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedException(
      Exception exception,
      HttpServletRequest request
  ) {
    log.error(
        "Unexpected error while processing request",
        exception
    );

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected error occurred"
    );

    problemDetail.setTitle("Internal server error");
    addCommonProperties(problemDetail, request);

    return problemDetail;
  }

  private void addCommonProperties(
      ProblemDetail problemDetail,
      HttpServletRequest request
  ) {
    problemDetail.setProperty(
        "timestamp",
        Instant.now()
    );

    problemDetail.setProperty(
        "path",
        request.getRequestURI()
    );
  }
}
