package br.com.hanrry.inventory.shared.exception.handler;

import br.com.hanrry.inventory.shared.exception.StandardError;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InsufficientStockException;
import br.com.hanrry.inventory.shared.exception.inventory.batch.InvalidQuantityException;
import br.com.hanrry.inventory.shared.exception.product.category.CascadeCategoryException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.product.category.CategoryNotFoundException;
import br.com.hanrry.inventory.shared.exception.notification.pdf.WritePdfException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.product.product.ProductNotFoundException;
import br.com.hanrry.inventory.shared.exception.auth.InvalidTokenException;
import br.com.hanrry.inventory.shared.exception.auth.GoogleAccountLinkRequiredException;
import br.com.hanrry.inventory.shared.exception.auth.GoogleAccountAlreadyLinkedException;
import br.com.hanrry.inventory.shared.exception.user.EmailAlreadyExistsException;
import br.com.hanrry.inventory.shared.exception.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import br.com.hanrry.inventory.shared.exception.pagination.InvalidPaginationException;
import br.com.hanrry.inventory.shared.exception.security.OwnerNotAuthenticatedException;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request) {
        StandardError error = new StandardError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "InvalidCredentials",
                "Invalid email or password",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        StandardError error = new StandardError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "ValidationError",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<StandardError> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request){
        String error = "EmailAlreadyExistsException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<StandardError> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request){
        String error = "UserNotFoundException";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ProductAlreadyExistsException.class)
    public ResponseEntity<StandardError> handleProductAlreadyExistsException(ProductAlreadyExistsException ex, HttpServletRequest request){
        String error = "ProductAlreadyExistsException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<StandardError> handleProductNotFoundException(ProductNotFoundException ex, HttpServletRequest request){
        String error = "ProductNotFoundException";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<StandardError> handleCategoryNotFoundException(CategoryNotFoundException ex, HttpServletRequest request){
        String error = "CategoryNotFoundException";
        HttpStatus status = HttpStatus.NOT_FOUND;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<StandardError> handleCategoryAlreadyExistsException(CategoryAlreadyExistsException ex, HttpServletRequest request){
        String error = "CategoryAlreadyExistsException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> handleDataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request){
        StandardError err = new StandardError(Instant.now(), HttpStatus.CONFLICT.value(), "DataIntegrityViolation", "A record with the same owner and identifier already exists", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(OwnerNotAuthenticatedException.class)
    public ResponseEntity<StandardError> handleOwnerNotAuthenticatedException(OwnerNotAuthenticatedException ex, HttpServletRequest request){
        StandardError err = new StandardError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), "OwnerNotAuthenticated", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(CascadeCategoryException.class)
    public ResponseEntity<StandardError> handleCascadeCategoryException(CascadeCategoryException ex, HttpServletRequest request){
        String error = "CascadeCategoryException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<StandardError> handleInvalidPaginationException(
            InvalidPaginationException ex,
            HttpServletRequest request) {
        StandardError err = new StandardError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "InvalidPagination",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<StandardError> handleInsufficientStockException(InsufficientStockException ex, HttpServletRequest request){
        String error = "InsufficientStock";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidQuantityException.class)
    public ResponseEntity<StandardError> handleInvalidQuantityException(InvalidQuantityException ex, HttpServletRequest request){
        String error = "InvalidQuantity";
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(WritePdfException.class)
    public ResponseEntity<StandardError> handleWritePdfException(WritePdfException ex, HttpServletRequest request){
        String error = "WritePdfException";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<StandardError> handleInvalidTokenException(InvalidTokenException ex, HttpServletRequest request){
        String error = "InvalidTokenException";
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(GoogleAccountLinkRequiredException.class)
    public ResponseEntity<StandardError> handleGoogleAccountLinkRequiredException(
            GoogleAccountLinkRequiredException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "GoogleAccountLinkRequired",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(GoogleAccountAlreadyLinkedException.class)
    public ResponseEntity<StandardError> handleGoogleAccountAlreadyLinkedException(
            GoogleAccountAlreadyLinkedException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "GoogleAccountAlreadyLinked",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleAllExceptions(Exception ex, HttpServletRequest request){
        String error = "Exception";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}
