package org.project.ssogssog.application.response.status;

import org.project.ssogssog.application.response.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();
    ErrorReasonDTO getReasonHttpStatus();
}
