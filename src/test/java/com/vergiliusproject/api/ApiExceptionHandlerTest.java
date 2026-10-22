package com.vergiliusproject.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFoundProducesProblemDetail404() {
        ProblemDetail pd = handler.notFound(new ApiExceptionHandler.NotFoundException("no such kernel"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Not found");
        assertThat(pd.getDetail()).isEqualTo("no such kernel");
    }

    @Test
    void badRequestProducesProblemDetail400() {
        ProblemDetail pd = handler.badRequest(new ApiExceptionHandler.BadRequestException("size must be <= 200"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("size must be <= 200");
    }

    @Test
    void typeMismatchProducesProblemDetail400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", null, new RuntimeException("cause"));
        ProblemDetail pd = handler.typeMismatch(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).contains("page").contains("abc");
    }
}
