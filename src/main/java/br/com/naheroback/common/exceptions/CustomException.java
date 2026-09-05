package br.com.naheroback.common.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CustomException {
    private Instant timestamp;
    private HttpStatus status;
    private String error;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    private String path;
}
