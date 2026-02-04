package com.star.share.common.exception;

import lombok.Getter;
import com.star.share.common.exception.ErrorCode;

@Getter
public class BusinessException extends RuntimeException{
    /**
     * Error code of internal service
     */
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     *  Using custom message
     * @param errorCode error code must have
     * @param message custom message
     */
    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

}
