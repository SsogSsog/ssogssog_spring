package org.project.ssogssog.global.response.code.status;

import org.project.ssogssog.global.response.code.dto.ReasonDTO;

public interface BaseCode {

    ReasonDTO getReason();
    ReasonDTO getReasonHttpStatus();
}
