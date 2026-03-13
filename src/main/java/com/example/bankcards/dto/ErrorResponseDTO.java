package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
     private LocalDateTime timestamp;
     private String path;
     private Integer status;
     private String error;
     private String message;
     private String details;


     public ErrorResponseDTO(HttpStatus httpStatus, Exception exception, String errorMessage, String error, String url){
          this.timestamp = LocalDateTime.now();
          this.status = httpStatus.value();
          this.error = error;
          this.message = errorMessage;
          this.details = exception.getMessage();
          this.path = url;
     }

     public ErrorResponseDTO(HttpStatus httpStatus, Exception exception, String errorMessage, String error){
          this.timestamp = LocalDateTime.now();
          this.status = httpStatus.value();
          this.error = error;
          this.message = errorMessage;
          this.details = exception.getMessage();
          this.path = "/api";
     }

     public ErrorResponseDTO(HttpStatus httpStatus, Exception exception, String error){
          this.timestamp = LocalDateTime.now();
          this.status = httpStatus.value();
          this.error = error;
          this.message = exception.getMessage();
          this.details = exception.getMessage();
          this.path = "/api";
     }
}