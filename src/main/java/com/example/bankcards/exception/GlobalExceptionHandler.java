package com.example.bankcards.exception;

import com.example.bankcards.dto.ErrorResponseDTO;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@RestControllerAdvice(basePackages = "com.example.bankcards.controller")
public class GlobalExceptionHandler {

    // Предварительно компилируем паттерн для производительности
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\d+)\\]");

    // Внедряем ObjectMapper, который Spring уже настроил
    @Autowired
    public GlobalExceptionHandler(ObjectMapper objectMapper) {
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400",
            description = "Ошибка в параметрах",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                 HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST,
                ex,
                "Не указан обязательный параметр",
                "Bad Request",
                request.getRequestURI());
    }

//    @ExceptionHandler(ConstraintViolationException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    @ApiResponse(responseCode = "400",
//            description = "Ошибка в параметрах",
//            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
//    public ErrorResponseDTO handleConstraintViolation(ConstraintViolationException  ex,
//        HttpServletRequest request) {
//        return new ErrorResponseDTO(
//                HttpStatus.BAD_REQUEST,
//                ex,
//                "Не заполнен обязательный параметр",
//                "Bad Request");
//    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400",
            description = "Ошибка в параметрах",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.BAD_REQUEST,
                ex,
                "Указан не правильный тип данных",
                "Bad Request",
                request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponse(responseCode = "401",
            description = "Ошибка авторизации",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleBadCredentials(BadCredentialsException ex,
                                                 HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED,
                ex,
                "Неверный логин или пароль",
                "Unauthorized",
                request.getRequestURI());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ApiResponse(responseCode = "401 ",
            description = "Ошибка Токена",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleExpiredJwtException(ExpiredJwtException ex,
                                                           HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED,
                ex,
                ex.getMessage(),
                "Flow Token Expired",
                request.getRequestURI());
    }
    //

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ApiResponse(responseCode = "403",
            description = "Доступ к ресурсу запрещён",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleAccessDenied(AccessDeniedException ex,
                                               HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.FORBIDDEN,
                ex,
                "У вас недостаточно прав для выполнения этого действия",
                "Forbidden",
                request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ApiResponse(responseCode = "404",
                description = "Ресурс не найден",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleResourceNotFound(ResourceNotFoundException ex,
                                                   HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.NOT_FOUND,
                ex,
                ex.getMessage(),
                "Not Found",
                request.getRequestURI());

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    @ApiResponse(responseCode = "422",
            description = "Валидация данных",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public List<ErrorResponseDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException  exList,
                                                                        HttpServletRequest request) {
        List<ErrorResponseDTO> ErrorResponseDTOList= new ArrayList<>();
        exList.getBindingResult().getFieldErrors().forEach(error -> {
            ErrorResponseDTOList.add(
                    new ErrorResponseDTO(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        error.getField()+": "+error.getDefaultMessage(),
                        "Unprocessable Content",
                            "Unprocessable Content",
                            request.getRequestURI()
                    )
            );
        });
        return ErrorResponseDTOList;
                //new ErrorResponseDTO(HttpStatus.UNPROCESSABLE_CONTENT, ex,"Unprocessable Content @Valid");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    @ApiResponse(responseCode = "422",
            description = "Валидация данных",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public List<ErrorResponseDTO> handleConstraintViolationException(ConstraintViolationException  exList,
                                                                     HttpServletRequest request) {
        List<ErrorResponseDTO> ErrorList = exList
                .getConstraintViolations()
                .stream()
                .map(error ->{
                    String fullPath = error.getPropertyPath().toString();
                    Integer indexNode = null;

                    // 1. Ищем индекс через RegExp (берем последнее совпадение)
                    Matcher matcher = INDEX_PATTERN.matcher(fullPath);
                    while (matcher.find()) {
                        indexNode = Integer.parseInt(matcher.group(1));
                    }
                    // 2. Получаем имя поля (все, что после последней точки)
                    String fieldName = fullPath.substring(fullPath.lastIndexOf('.') + 1);

                    // теперь можно создать dto ошибки
                    return new ErrorResponseDTO(
                            HttpStatus.UNPROCESSABLE_CONTENT,
                            String.format("Элемент %d. %s: %s", indexNode, fieldName, error.getMessage()),
                            "Unprocessable Content",
                            error.getLeafBean(),
                            request.getRequestURI()); // передаём объект с ошибкой
                    }
                ).toList();

        return ErrorList;
        //new ErrorResponseDTO(HttpStatus.UNPROCESSABLE_CONTENT, ex,"Unprocessable Content @Valid");
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ApiResponse(responseCode = "409",
            description = "Конфликт",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleAlreadyExists(AlreadyExistsException ex,
                                                HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.CONFLICT,
                ex,
                ex.getMessage(),
                "Conflict",
                request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    @ApiResponse(responseCode = "422 ",
            description = "Ошибка в данных",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleIllegalArgumentException(IllegalArgumentException ex,
                                                           HttpServletRequest request) {
        return new ErrorResponseDTO(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ex,
                ex.getMessage(),
                "Unprocessable Content",
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ApiResponse(responseCode = "500",
            description = "Произошла непредвиденная ошибка",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ErrorResponseDTO handleGenericException(Exception ex,
                                                   HttpServletRequest request) {
        return new ErrorResponseDTO(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ex,
                        "Произошла непредвиденная ошибка",
                        "Internal Server Error",
                        request.getRequestURI());
    }
}