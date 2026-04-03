package com.userservice.user.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ErrorResponse {

    private Map<String, String> errors;
    
    public static ValidationErrorResponseBuilder validationBuilder() {
        return new ValidationErrorResponseBuilder();
    }
    
    public static class ValidationErrorResponseBuilder {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> errors;
        
        public ValidationErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public ValidationErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }
        
        public ValidationErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }
        
        public ValidationErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public ValidationErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }
        
        public ValidationErrorResponseBuilder errors(Map<String, String> errors) {
            this.errors = errors;
            return this;
        }
        
        public ValidationErrorResponse build() {
            ValidationErrorResponse response = new ValidationErrorResponse();
            response.setTimestamp(timestamp);
            response.setStatus(status);
            response.setError(error);
            response.setMessage(message);
            response.setPath(path);
            response.setErrors(errors);
            return response;
        }
    }
}
