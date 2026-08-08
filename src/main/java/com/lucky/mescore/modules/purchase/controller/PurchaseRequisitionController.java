package com.lucky.mescore.modules.purchase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.material.mapper.UnitMapper;
import com.lucky.mescore.modules.purchase.entity.PurchaseRequisition;
import com.lucky.mescore.modules.purchase.service.PurchaseRequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/purchase/requisition")
@RequiredArgsConstructor
public class PurchaseRequisitionController {

    private final PurchaseRequisitionService requisitionService;
    private final MaterialMapper materialMapper;
    private final UnitMapper unitMapper;

    @PostMapping("/page")
    public R<PageResponse<PurchaseRequisition>> page(@RequestBody PageRequest<PurchaseRequisition> request) {
        Page<PurchaseRequisition> page = requisitionService.page(
                new Page<>(request.getPageNum(), request.getPageSize()),
                requisitionService.buildQuery(request.getCondition()));
        enrich(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<PurchaseRequisition> getById(@PathVariable Long id) {
        PurchaseRequisition req = requisitionService.getById(id);
        enrich(List.of(req));
        return R.ok(req);
    }

    private void enrich(List<PurchaseRequisition> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> materialIds = list.stream().map(PurchaseRequisition::getMaterialId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> unitIds = list.stream().map(PurchaseRequisition::getUnitId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() :
                materialMapper.selectBatchIds(materialIds).stream().collect(Collectors.toMap(Material::getId, Function.identity()));
        Map<Long, Unit> unitMap = unitIds.isEmpty() ? Map.of() :
                unitMapper.selectBatchIds(unitIds).stream().collect(Collectors.toMap(Unit::getId, Function.identity()));
        for (PurchaseRequisition r : list) {
            if (r.getMaterialId() != null && materialMap.containsKey(r.getMaterialId())) {
                r.setMaterialName(materialMap.get(r.getMaterialId()).getMaterialName());
            }
            if (r.getUnitId() != null && unitMap.containsKey(r.getUnitId())) {
                r.setUnitName(unitMap.get(r.getUnitId()).getUnitName());
            }
        }
    }

    @PostMapping
    public R<PurchaseRequisition> create(@RequestBody PurchaseRequisition req) {
        requisitionService.createRequisition(req);
        return R.ok(req);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody PurchaseRequisition req) {
        req.setId(id);
        requisitionService.updateById(req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        requisitionService.removeById(id);
        return R.ok();
    }

    /** 提交审批：触发审批引擎 */
    @PostMapping("/{id}/submit")
    public R<Void> submitApproval(@PathVariable Long id, @RequestParam String applicant) {
        requisitionService.submitApproval(id, applicant);
        return R.ok();
    }
}
