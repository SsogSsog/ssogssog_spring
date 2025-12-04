package org.project.ssogssog.application.response.status;

import org.project.ssogssog.application.response.dto.ReasonDTO;

public interface BaseCode {

    ReasonDTO getReason();
    ReasonDTO getReasonHttpStatus();
}
