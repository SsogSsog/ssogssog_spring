package org.project.ssogssog.application.common.response.status;

import org.project.ssogssog.application.common.response.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();
    ErrorReasonDTO getReasonHttpStatus();
}
