package org.project.ssogssog.global.response.code.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.project.ssogssog.global.response.code.dto.ErrorReasonDTO;
import org.project.ssogssog.global.response.code.status.BaseErrorCode;

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


