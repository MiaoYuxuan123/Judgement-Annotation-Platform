package edu.nju.jap.exception;

import edu.nju.jap.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ApiResponse<Void> responseStatus(ResponseStatusException ex) {
        return ApiResponse.error(ex.getStatusCode().value(), ex.getReason());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ApiResponse<Void> badRequest(Exception ex) {
        return ApiResponse.error(400, "参数错误");
    }

    @ExceptionHandler(Exception.class)
    ApiResponse<Void> serverError(Exception ex) {
        log.error("Unhandled exception", ex);
        return ApiResponse.error(500, rootMessage(ex));
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            return root.getMessage();
        }
        return root.getClass().getSimpleName();
    }
}
