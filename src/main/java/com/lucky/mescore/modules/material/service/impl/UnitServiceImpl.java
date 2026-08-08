package com.lucky.mescore.modules.material.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.mapper.UnitMapper;
import com.lucky.mescore.modules.material.service.UnitService;
import org.springframework.stereotype.Service;

@Service
public class UnitServiceImpl extends ServiceImpl<UnitMapper, Unit> implements UnitService {
}
