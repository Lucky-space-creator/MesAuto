package com.lucky.mescore.modules.material.controller;

import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unit")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping("/all")
    public R<List<Unit>> all() {
        return R.ok(unitService.list());
    }

    @PostMapping
    public R<Void> create(@RequestBody Unit unit) {
        unitService.save(unit);
        return R.ok();
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Unit unit) {
        unit.setId(id);
        unitService.updateById(unit);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        unitService.removeById(id);
        return R.ok();
    }
}
