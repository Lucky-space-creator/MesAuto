package com.lucky.mescore.modules.approval.event;

import lombok.Getter;

/**
 * 审批流程终结事件。
 *
 * 审批引擎不直接依赖各业务 Service（否则 order -> approval -> order 会形成循环依赖），
 * 而是发布事件，由各业务模块自行监听并推进自身状态机。
 */
@Getter
public class ApprovalFinishedEvent {

    /** 业务类型，如 ORDER */
    private final String bizType;

    /** 业务主键 */
    private final Long bizId;

    /** 是否审批通过 */
    private final boolean approved;

    public ApprovalFinishedEvent(String bizType, Long bizId, boolean approved) {
        this.bizType = bizType;
        this.bizId = bizId;
        this.approved = approved;
    }
}
