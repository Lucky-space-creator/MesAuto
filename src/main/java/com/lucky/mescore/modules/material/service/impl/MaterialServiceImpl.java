package com.lucky.mescore.modules.material.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.material.service.MaterialService;
import org.springframework.stereotype.Service;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {
}
