package org.project.ssogssog.global.payload.code;

import org.project.ssogssog.global.payload.code.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();
    ErrorReasonDTO getReasonHttpStatus();
}
