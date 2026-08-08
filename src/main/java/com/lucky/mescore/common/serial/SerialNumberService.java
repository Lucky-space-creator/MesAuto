package com.lucky.mescore.common.serial;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 基于 sys_serial_number 表的业务流水号生成器。
 *
 * 采用 INSERT ... ON DUPLICATE KEY UPDATE 的原子自增方式，
 * 保证并发下同一 (tenant, bizType, date) 序号不重复。
 * 使用 REQUIRES_NEW 独立事务，避免外层业务回滚导致号段回退。
 */
@Service
@RequiredArgsConstructor
public class SerialNumberService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final JdbcTemplate jdbcTemplate;

    /**
     * 生成流水号，格式：前缀 + yyyyMMdd + 4位序号，例如 MO202608080001
     *
     * @param bizType 业务类型，需与 sys_serial_number.biz_type 对应
     * @param prefix  前缀，表中无记录时用于初始化
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generate(String bizType, String prefix) {
        return generate("DEFAULT", bizType, prefix);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generate(String tenantId, String bizType, String prefix) {
        String datePart = LocalDate.now().format(DATE_FMT);

        jdbcTemplate.update(
                "INSERT INTO sys_serial_number (tenant_id, prefix, biz_type, date_part, current_seq) "
                        + "VALUES (?, ?, ?, ?, 1) "
                        + "ON DUPLICATE KEY UPDATE current_seq = current_seq + 1",
                tenantId, prefix, bizType, datePart);

        Integer seq = jdbcTemplate.queryForObject(
                "SELECT current_seq FROM sys_serial_number "
                        + "WHERE tenant_id = ? AND biz_type = ? AND date_part = ?",
                Integer.class, tenantId, bizType, datePart);

        String realPrefix = jdbcTemplate.queryForObject(
                "SELECT prefix FROM sys_serial_number "
                        + "WHERE tenant_id = ? AND biz_type = ? AND date_part = ?",
                String.class, tenantId, bizType, datePart);

        return (realPrefix == null ? prefix : realPrefix) + datePart + String.format("%04d", seq == null ? 1 : seq);
    }
}
