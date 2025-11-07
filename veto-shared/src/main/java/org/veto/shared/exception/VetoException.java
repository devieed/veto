package org.veto.shared.exception;

import lombok.Getter;

public class VetoException extends RuntimeException {

    @Getter
    private String code;

    private String message;

    public VetoException() {
    }

    public VetoException(String code) {
        this.code = code;
    }

}
