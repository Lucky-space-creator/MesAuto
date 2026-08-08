package com.lucky.mescore.modules.order.erp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟上游 ERP 的订单下发网关。
 *
 * 生产环境应替换为真实的 ERP 对接实现（REST 调用 / MQ 消费 / 中间表轮询），
 * 只要保持 {@link #pullOrders(int)} 的语义不变，上层同步逻辑无需改动。
 */
@Slf4j
@Component
public class ErpOrderMockGateway {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** ERP 侧可下单的成品编码，需与 MES 物料主数据一致 */
    private static final String[] PRODUCT_CODES = {
            "FG-CHAIR-001", "FG-DESK-001", "FG-CABINET-001"
    };

    private static final String[] CUSTOMERS = {
            "华东制造集团", "南方家居有限公司", "北方办公科技", "西部家具连锁"
    };

    private static final String[] PRIORITIES = {"NORMAL", "NORMAL", "NORMAL", "URGENT"};

    /** 单号序列，保证同一次运行内不重复 */
    private final AtomicInteger sequence = new AtomicInteger(1000);

    /**
     * 从上游拉取待同步订单。
     *
     * @param batchSize 期望拉取数量
     */
    public List<ErpOrderDTO> pullOrders(int batchSize) {
        int size = Math.max(1, Math.min(batchSize, 20));
        List<ErpOrderDTO> list = new ArrayList<>(size);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String today = LocalDate.now().format(DATE_FMT);

        for (int i = 0; i < size; i++) {
            ErpOrderDTO dto = new ErpOrderDTO();
            dto.setErpOrderNo("ERP" + today + sequence.incrementAndGet());
            dto.setMaterialCode(PRODUCT_CODES[rnd.nextInt(PRODUCT_CODES.length)]);
            dto.setQuantity(BigDecimal.valueOf(rnd.nextInt(10, 200)));
            dto.setUnitCode("PCS");
            dto.setCustomerName(CUSTOMERS[rnd.nextInt(CUSTOMERS.length)]);

            LocalDate start = LocalDate.now().plusDays(rnd.nextInt(0, 5));
            dto.setPlanStartDate(start);
            dto.setPlanEndDate(start.plusDays(rnd.nextInt(5, 25)));
            dto.setPriority(PRIORITIES[rnd.nextInt(PRIORITIES.length)]);
            dto.setRemark("ERP自动下发");
            list.add(dto);
        }
        log.debug("模拟ERP下发订单 {} 条", list.size());
        return list;
    }
}
