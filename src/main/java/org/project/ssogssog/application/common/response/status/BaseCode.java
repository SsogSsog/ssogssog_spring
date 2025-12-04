package org.project.ssogssog.application.common.response.status;

import org.project.ssogssog.application.common.response.dto.ReasonDTO;

public interface BaseCode {

    ReasonDTO getReason();
    ReasonDTO getReasonHttpStatus();
}
