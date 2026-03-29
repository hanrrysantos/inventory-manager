package br.com.hanrry.inventory.exception.handler;

import br.com.hanrry.inventory.exception.StandardError;
import br.com.hanrry.inventory.exception.batch.InsufficientStockException;
import br.com.hanrry.inventory.exception.batch.InvalidQuantityException;
import br.com.hanrry.inventory.exception.category.CascadeCategoryException;
import br.com.hanrry.inventory.exception.category.CategoryAlreadyExistsException;
import br.com.hanrry.inventory.exception.category.CategoryNotFoundException;
import br.com.hanrry.inventory.exception.pdf.WritePdfException;
import br.com.hanrry.inventory.exception.product.ProductAlreadyExistsException;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.exception.security.InvalidTokenException;
import br.com.hanrry.inventory.exception.user.EmailAlreadyExistsException;
import br.com.hanrry.inventory.exception.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

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
        HttpStatus status = HttpStatus.CONFLICT;
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

    @ExceptionHandler(CascadeCategoryException.class)
    public ResponseEntity<StandardError> handleCascadeCategoryException(CascadeCategoryException ex, HttpServletRequest request){
        String error = "CascadeCategoryException";
        HttpStatus status = HttpStatus.CONFLICT;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleAllExceptions(Exception ex, HttpServletRequest request){
        String error = "Exception";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(Instant.now(), status.value(), error, ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}
