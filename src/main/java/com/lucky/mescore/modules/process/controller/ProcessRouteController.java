package com.lucky.mescore.modules.process.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lucky.mescore.common.page.PageRequest;
import com.lucky.mescore.common.page.PageResponse;
import com.lucky.mescore.common.result.R;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.process.entity.ProcessRoute;
import com.lucky.mescore.modules.process.entity.ProcessStep;
import com.lucky.mescore.modules.process.service.ProcessRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/process-route")
@RequiredArgsConstructor
public class ProcessRouteController {

    private final ProcessRouteService routeService;
    private final MaterialMapper materialMapper;

    @PostMapping("/page")
    public R<PageResponse<ProcessRoute>> page(@RequestBody PageRequest<ProcessRoute> request) {
        ProcessRoute condition = request.getCondition();
        LambdaQueryWrapper<ProcessRoute> qw = new LambdaQueryWrapper<>();
        if (condition != null) {
            qw.like(condition.getRouteCode() != null, ProcessRoute::getRouteCode, condition.getRouteCode())
              .like(condition.getRouteName() != null, ProcessRoute::getRouteName, condition.getRouteName())
              .eq(condition.getMaterialId() != null, ProcessRoute::getMaterialId, condition.getMaterialId())
              .eq(condition.getStatus() != null, ProcessRoute::getStatus, condition.getStatus());
        }
        qw.orderByDesc(ProcessRoute::getCreateTime);
        Page<ProcessRoute> page = routeService.page(new Page<>(request.getPageNum(), request.getPageSize()), qw);
        enrich(page.getRecords());
        return R.ok(PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @GetMapping("/{id}")
    public R<ProcessRoute> getById(@PathVariable Long id) {
        ProcessRoute route = routeService.getById(id);
        enrich(List.of(route));
        return R.ok(route);
    }

    @GetMapping("/{id}/steps")
    public R<List<ProcessStep>> steps(@PathVariable Long id) {
        return R.ok(routeService.getSteps(id));
    }

    @GetMapping("/{id}/flow")
    public R<java.util.Map<String, Object>> flow(@PathVariable Long id) {
        ProcessRoute route = routeService.getById(id);
        List<ProcessStep> steps = routeService.getSteps(id);
        return R.ok(java.util.Map.of("route", route, "steps", steps));
    }

    @PostMapping
    public R<ProcessRoute> create(@RequestBody ProcessRoute route) {
        routeService.saveRouteWithSteps(route, route.getSteps());
        return R.ok(route);
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ProcessRoute route) {
        route.setId(id);
        routeService.updateById(route);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        routeService.removeById(id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        routeService.publish(id);
        return R.ok();
    }

    @PostMapping("/{id}/copy")
    public R<ProcessRoute> copy(@PathVariable Long id) {
        return R.ok(routeService.copy(id));
    }

    @PostMapping("/{id}/steps")
    public R<Void> saveSteps(@PathVariable Long id, @RequestBody List<ProcessStep> steps) {
        ProcessRoute route = new ProcessRoute();
        route.setId(id);
        routeService.updateRouteWithSteps(route, steps);
        return R.ok();
    }

    /** 补充前端展示用的关联物料名称，不影响持久化 */
    private void enrich(List<ProcessRoute> routes) {
        if (routes == null || routes.isEmpty()) return;
        Set<Long> materialIds = routes.stream().map(ProcessRoute::getMaterialId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Material> materialMap = materialIds.isEmpty() ? Map.of() :
                materialMapper.selectBatchIds(materialIds).stream()
                        .collect(Collectors.toMap(Material::getId, Function.identity()));
        for (ProcessRoute r : routes) {
            if (r.getMaterialId() != null && materialMap.containsKey(r.getMaterialId())) {
                r.setMaterialName(materialMap.get(r.getMaterialId()).getMaterialName());
            }
        }
    }
}
