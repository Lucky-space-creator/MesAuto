package com.lucky.mescore.modules.order.erp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ERP 订单定时同步任务。
 *
 * 通过 mes.erp-sync.enabled 开关控制，默认关闭，
 * 避免开发环境后台不断造数据干扰调试；需要演示时在配置里打开即可。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpOrderSyncJob {

    private final ErpOrderSyncService syncService;

    @Value("${mes.erp-sync.enabled:false}")
    private boolean enabled;

    @Value("${mes.erp-sync.batch-size:3}")
    private int batchSize;

    /** 默认每 5 分钟拉取一次 */
    @Scheduled(cron = "${mes.erp-sync.cron:0 */5 * * * ?}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            syncService.sync(batchSize);
        } catch (Exception e) {
            log.error("ERP订单定时同步异常", e);
        }
    }
}
