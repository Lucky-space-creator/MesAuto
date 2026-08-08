package com.lucky.mescore.modules.process.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.process.entity.ProcessRoute;
import com.lucky.mescore.modules.process.entity.ProcessStep;

import java.util.List;

public interface ProcessRouteService extends IService<ProcessRoute> {

    List<ProcessStep> getSteps(Long routeId);

    void saveRouteWithSteps(ProcessRoute route, List<ProcessStep> steps);

    void updateRouteWithSteps(ProcessRoute route, List<ProcessStep> steps);

    void publish(Long routeId);

    ProcessRoute copy(Long sourceId);
}
