package com.lucky.mescore.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.system.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
