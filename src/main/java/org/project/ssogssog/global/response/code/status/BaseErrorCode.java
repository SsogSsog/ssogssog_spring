package org.project.ssogssog.global.response.code.status;

import org.project.ssogssog.global.response.code.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();
    ErrorReasonDTO getReasonHttpStatus();
}
