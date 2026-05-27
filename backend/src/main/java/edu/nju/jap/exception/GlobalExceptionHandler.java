package edu.nju.jap.exception;

import edu.nju.jap.common.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
        return ApiResponse.error(500, ex.getMessage() == null ? "服务器异常" : ex.getMessage());
    }
}
