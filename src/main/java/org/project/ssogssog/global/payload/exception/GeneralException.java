package org.project.ssogssog.global.payload.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.project.ssogssog.global.payload.code.dto.ErrorReasonDTO;
import org.project.ssogssog.global.payload.code.BaseErrorCode;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private BaseErrorCode code;

    public ErrorReasonDTO getErrorReason() {
        return this.code.getReason();
    }

    public ErrorReasonDTO getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }
}


