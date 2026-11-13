package com.pzhgp.backend.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Komunikaty błędów są zawsze zwykłym tekstem, także gdy klient prosi o JSON (Accept: application/json),
    // tak samo jak odpowiedzi 401 i 403 z RestAuthenticationEntryPoint i RestAccessDeniedHandler.
    private static final MediaType PLAIN_TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private static ResponseEntity<String> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(PLAIN_TEXT).body(message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Walidator zwraca błędy w przypadkowej kolejności. Wybieramy błąd pola, które w żądaniu
        // (a więc i w formularzu) występuje najwcześniej, żeby komunikat był zawsze ten sam.
        List<String> fieldOrder = fieldNamesInDeclarationOrder(ex.getBindingResult().getTarget());

        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .min(Comparator.comparingInt((ObjectError error) -> fieldPosition(error, fieldOrder))
                        .thenComparing(error -> Objects.requireNonNullElse(error.getDefaultMessage(), "")))
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Błąd walidacji formularza.")
                .orElse("Błąd walidacji formularza.");

        return error(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private static List<String> fieldNamesInDeclarationOrder(Object target) {
        if (target == null) {
            return List.of();
        }

        Class<?> type = target.getClass();
        if (type.isRecord()) {
            return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
        }

        return Arrays.stream(type.getDeclaredFields()).map(Field::getName).toList();
    }

    // Błędy pól spoza listy (np. z metod @AssertTrue) i błędy całego obiektu trafiają na koniec.
    private static int fieldPosition(ObjectError error, List<String> fieldOrder) {
        if (error instanceof FieldError fieldError) {
            int index = fieldOrder.indexOf(fieldError.getField());
            if (index >= 0) {
                return index;
            }
        }

        return fieldOrder.size();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<String> handleForbiddenOperation(ForbiddenOperationException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<String> handleResourceConflict(ResourceConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(GpxParsingException.class)
    public ResponseEntity<String> handleGpxParsing(GpxParsingException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingRequestPart(MissingServletRequestPartException ex) {
        return error(HttpStatus.BAD_REQUEST, "Żądanie jest niekompletne — brakuje części \"" + ex.getRequestPartName() + "\".");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "Przesłany plik przekracza maksymalny rozmiar dozwolony przez serwer.");
    }

    @ExceptionHandler(SubmissionRateLimitException.class)
    public ResponseEntity<String> handleSubmissionRateLimit(SubmissionRateLimitException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<String> handleAuthRateLimit(AuthRateLimitException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(CaptchaVerificationException.class)
    public ResponseEntity<String> handleCaptchaVerification(CaptchaVerificationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(CaptchaUnavailableException.class)
    public ResponseEntity<String> handleCaptchaUnavailable(CaptchaUnavailableException ex) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<String> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllUnhandledExceptions(Exception ex) {
        log.error("Nieoczekiwany błąd serwera: ", ex);

        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd serwera. Spróbuj ponownie później lub skontaktuj się z administratorem.");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        // Nieznana wartość pola wyliczeniowego (np. roli) to błąd danych, a nie struktury JSON.
        if (ex.getCause() instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()
                && !invalidFormat.getPath().isEmpty()) {
            String field = invalidFormat.getPath().getLast().getPropertyName();
            return error(HttpStatus.BAD_REQUEST, "Nieprawidłowa wartość pola \"" + field + "\".");
        }

        return error(HttpStatus.BAD_REQUEST, "Nieprawidłowy format żądania (błąd struktury JSON).");
    }

    // Błędy samego żądania HTTP (zły adres, metoda lub format) nie są błędami serwera,
    // dlatego nie trafiają do ogólnej obsługi z kodem 500 i nie są logowane jako ERROR.

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFound(NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "Nie znaleziono zasobu pod podanym adresem.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();

        if (supportedMethods != null) {
            response.allow(supportedMethods.toArray(new HttpMethod[0]));
        }

        return response.contentType(PLAIN_TEXT).body("Metoda " + ex.getMethod() + " nie jest obsługiwana pod tym adresem.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Nieobsługiwany format danych żądania.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Żądanie jest niekompletne — brakuje parametru \"" + ex.getParameterName() + "\".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();

        if (requiredType != null && requiredType.isEnum()) {
            return error(HttpStatus.BAD_REQUEST, "Nieprawidłowa wartość parametru \"" + ex.getName() + "\".");
        }

        return error(HttpStatus.BAD_REQUEST, "Nieprawidłowy typ parametru \"" + ex.getName() + "\".");
    }
}
