package com.lucky.mescore.modules.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.enums.ProcessRouteStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.process.entity.ProcessRoute;
import com.lucky.mescore.modules.process.entity.ProcessStep;
import com.lucky.mescore.modules.process.mapper.ProcessRouteMapper;
import com.lucky.mescore.modules.process.mapper.ProcessStepMapper;
import com.lucky.mescore.modules.process.service.ProcessRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessRouteServiceImpl extends ServiceImpl<ProcessRouteMapper, ProcessRoute> implements ProcessRouteService {

    private final ProcessStepMapper stepMapper;

    @Override
    public List<ProcessStep> getSteps(Long routeId) {
        return stepMapper.selectByRouteId(routeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRouteWithSteps(ProcessRoute route, List<ProcessStep> steps) {
        save(route);
        if (steps != null && !steps.isEmpty()) {
            steps.forEach(step -> {
                step.setRouteId(route.getId());
                stepMapper.insert(step);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRouteWithSteps(ProcessRoute route, List<ProcessStep> steps) {
        updateById(route);
        if (steps != null) {
            stepMapper.deleteByRouteId(route.getId());
            steps.forEach(step -> {
                step.setId(null);
                step.setRouteId(route.getId());
                stepMapper.insert(step);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long routeId) {
        ProcessRoute route = getById(routeId);
        if (route == null) {
            throw new BusinessException("工艺路线不存在");
        }
        // 旧版本失效
        lambdaUpdate()
                .eq(ProcessRoute::getMaterialId, route.getMaterialId())
                .eq(ProcessRoute::getStatus, ProcessRouteStatusEnum.PUBLISHED.getCode())
                .set(ProcessRoute::getStatus, ProcessRouteStatusEnum.EXPIRED.getCode())
                .update();
        // 发布新版本
        route.setStatus(ProcessRouteStatusEnum.PUBLISHED.getCode());
        route.setEffectiveDate(LocalDate.now());
        updateById(route);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessRoute copy(Long sourceId) {
        ProcessRoute source = getById(sourceId);
        if (source == null) {
            throw new BusinessException("源工艺路线不存在");
        }
        ProcessRoute copy = new ProcessRoute();
        copy.setRouteCode(source.getRouteCode() + "_" + UUID.randomUUID().toString().substring(0, 6));
        copy.setRouteName(source.getRouteName() + "(副本)");
        copy.setMaterialId(source.getMaterialId());
        copy.setVersion("V1.0");
        copy.setStatus(ProcessRouteStatusEnum.DRAFT.getCode());
        copy.setRemark(source.getRemark());
        save(copy);

        List<ProcessStep> sourceSteps = stepMapper.selectByRouteId(sourceId);
        if (sourceSteps != null) {
            for (ProcessStep step : sourceSteps) {
                step.setId(null);
                step.setRouteId(copy.getId());
                stepMapper.insert(step);
            }
        }
        return copy;
    }
}
