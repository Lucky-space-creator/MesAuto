package com.lucky.mescore.modules.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.material.entity.Bom;
import com.lucky.mescore.modules.material.entity.BomItem;
import com.lucky.mescore.modules.material.mapper.BomItemMapper;
import com.lucky.mescore.modules.material.mapper.BomMapper;
import com.lucky.mescore.modules.material.service.BomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BomServiceImpl extends ServiceImpl<BomMapper, Bom> implements BomService {

    private final BomItemMapper bomItemMapper;

    @Override
    public List<BomItem> getBomItems(Long bomId) {
        return bomItemMapper.selectList(
                new LambdaQueryWrapper<BomItem>().eq(BomItem::getBomId, bomId).orderByAsc(BomItem::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBomWithItems(Bom bom, List<BomItem> items) {
        if (bom.getMaterialId() == null) {
            throw new BusinessException("BOM父物料不能为空");
        }
        save(bom);
        if (items != null && !items.isEmpty()) {
            items.forEach(item -> {
                item.setBomId(bom.getId());
                bomItemMapper.insert(item);
            });
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBomWithItems(Bom bom, List<BomItem> items) {
        updateById(bom);
        if (items != null) {
            bomItemMapper.deleteByBomId(bom.getId());
            items.forEach(item -> {
                item.setId(null);
                item.setBomId(bom.getId());
                bomItemMapper.insert(item);
            });
        }
    }
}
