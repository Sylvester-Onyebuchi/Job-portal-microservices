package com.sylvester.springauthkeycloak.exception;

import com.sylvester.springauthkeycloak.dto.CreateUserRequest;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationErrorsReturnBadRequestWithFieldMessages() throws Exception {
        MethodArgumentNotValidException exception = validationException("email", "Email is required");

        ResponseEntity<?> response = handler.handleMethodArgumentNotValidException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("email", "Email is required"), response.getBody());
    }

    @Test
    void notFoundExceptionReturnsNotFoundErrorBody() {
        ResponseEntity<?> response = handler.handleNotFoundException(new NotFoundException("User not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Map.of("error: ", "User not found"), response.getBody());
    }

    @Test
    void alreadyExistExceptionReturnsConflictErrorBody() {
        ResponseEntity<?> response = handler.handleAlreadyExistException(new AlreadyExistException("User already exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(Map.of("error: ", "User already exists"), response.getBody());
    }

    private MethodArgumentNotValidException validationException(String field, String message) throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", CreateUserRequest.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", field, message));
        return new MethodArgumentNotValidException(methodParameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(@Valid CreateUserRequest request) {
    }
}
