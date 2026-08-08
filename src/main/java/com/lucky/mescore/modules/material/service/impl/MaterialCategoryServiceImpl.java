package com.lucky.mescore.modules.material.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.modules.material.entity.MaterialCategory;
import com.lucky.mescore.modules.material.mapper.MaterialCategoryMapper;
import com.lucky.mescore.modules.material.service.MaterialCategoryService;
import org.springframework.stereotype.Service;

@Service
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory> implements MaterialCategoryService {
}
