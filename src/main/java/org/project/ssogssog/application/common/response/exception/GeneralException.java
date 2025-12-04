package org.project.ssogssog.application.common.response.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.project.ssogssog.application.common.response.dto.ErrorReasonDTO;
import org.project.ssogssog.application.common.response.status.BaseErrorCode;

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


