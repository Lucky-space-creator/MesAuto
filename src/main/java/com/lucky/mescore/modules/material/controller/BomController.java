package com.lucky.mescore.modules.material.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Bom;
import com.lucky.mescore.modules.material.entity.BomItem;
import com.lucky.mescore.modules.material.service.BomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bom")
@RequiredArgsConstructor
public class BomController {

    private final BomService bomService;

    @PostMapping("/page")
    public R<PageResponse<Bom>> page(@RequestBody PageRequest<Bom> request) {
        Bom condition = request.getCondition();
        LambdaQueryWrapper<Bom> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getBomCode() != null, Bom::getBomCode, condition.getBomCode())
              .like(condition.getBomName() != null, Bom::getBomName, condition.getBomName())
              .eq(condition.getMaterialId() != null, Bom::getMaterialId, condition.getMaterialId());
        }
        qw.orderByDesc(Bom::getCreateTime);
        Page<Bom> page = bomService.page(new Page<>(request.getPageNum(), request.getPageSize()), qw);
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<Bom> getById(@PathVariable Long id) {
        return R.ok(bomService.getById(id));
    }

    @GetMapping("/{id}/items")
    public R<List<BomItem>> items(@PathVariable Long id) {
        return R.ok(bomService.getBomItems(id));
    }

    @PostMapping
    public R<Void> create(@RequestBody Bom bom) {
        bomService.saveBomWithItems(bom, null);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Bom bom) {
        bom.setId(id);
        bomService.updateById(bom);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        bomService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/items")
    public R<Void> saveItems(@PathVariable Long id, @RequestBody List<BomItem> items) {
        Bom bom = new Bom();
        bom.setId(id);
        bomService.updateBomWithItems(bom, items);
        return R.ok();
    }
}
