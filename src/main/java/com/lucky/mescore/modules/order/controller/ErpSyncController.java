package com.lucky.mescore.modules.order.controller;

import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.order.erp.ErpOrderDTO;
import com.lucky.mescore.modules.order.erp.ErpOrderMockGateway;
import com.lucky.mescore.modules.order.erp.ErpOrderSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ERP 集成接口。
 *
 * 提供手动触发同步的入口，便于演示和联调；
 * 定时同步见 {@code ErpOrderSyncJob}。
 */
@RestController
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpSyncController {

    private final ErpOrderSyncService syncService;
    private final ErpOrderMockGateway mockGateway;

    /** 手动触发一次 ERP 订单同步 */
    @PostMapping("/sync/order")
    public R<ErpOrderSyncService.SyncResult> syncOrder(
            @RequestParam(defaultValue = "3") int batchSize) {
        return R.ok(syncService.sync(batchSize));
    }

    /** 预览上游将要下发的报文，不落库，用于排查主数据匹配问题 */
    @GetMapping("/preview/order")
    public R<List<ErpOrderDTO>> preview(@RequestParam(defaultValue = "3") int batchSize) {
        return R.ok(mockGateway.pullOrders(batchSize));
    }
}
