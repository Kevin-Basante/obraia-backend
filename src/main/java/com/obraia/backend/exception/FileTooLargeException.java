package com.obraia.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;

/** The uploaded file is bigger than the limit. */
public class FileTooLargeException extends AppException {

    public FileTooLargeException(DataSize limit) {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "The file is too large. The limit is " + limit.toMegabytes() + " MB");
    }
}
