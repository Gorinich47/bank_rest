package com.example.bankcards.exception;

import com.example.bankcards.dto.ErrorResponseDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice(basePackages = "com.example.bankcards.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400",
            description = "Ошибка в параметрах",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST,
                ex,
                "Не указан обязательный параметр",
                "Bad Request");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400",
            description = "Ошибка в параметрах",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleConstraintViolation(ConstraintViolationException  ex) {
        return new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST,
                ex,
                "Не заполнен обязательный параметр",
                "Bad Request");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400",
            description = "Ошибка в параметрах",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST,
                ex,
                "Указан не правильный тип данных",
                "Bad Request");
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponse(responseCode = "401",
            description = "Ошибка авторизации",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleBadCredentials(BadCredentialsException ex) {
        return new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED,
                ex,
                "Неверный логин или пароль",
                "Unauthorized",
                "/api/auth/login");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(responseCode = "404",
                description = "Ресурс не найден",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleResourceNotFound(ResourceNotFoundException ex) {
        return new ErrorResponseDTO(HttpStatus.NOT_FOUND, ex,"Not Found");

    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ApiResponse(responseCode = "409",
            description = "Конфликт",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleAlreadyExists(AlreadyExistsException ex) {
        return new ErrorResponseDTO(HttpStatus.CONFLICT, ex,"Conflict");
    }



    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponse(responseCode = "403",
            description = "Доступ к ресурсу запрещён",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponseDTO(
                        HttpStatus.FORBIDDEN,
                        ex,
                        "У вас недостаточно прав для выполнения этого действия",
                        "Forbidden");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(responseCode = "500",
            description = "Произошла непредвиденная ошибка",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleGenericException(Exception ex) {
        return new ErrorResponseDTO(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ex,
                        "Произошла непредвиденная ошибка",
                        "Internal Server Error");
    }
}