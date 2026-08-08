package com.lucky.mescore.modules.material.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.material.entity.Bom;
import com.lucky.mescore.modules.material.entity.BomItem;

import java.util.List;

public interface BomService extends IService<Bom> {

    List<BomItem> getBomItems(Long bomId);

    void saveBomWithItems(Bom bom, List<BomItem> items);

    void updateBomWithItems(Bom bom, List<BomItem> items);
}
