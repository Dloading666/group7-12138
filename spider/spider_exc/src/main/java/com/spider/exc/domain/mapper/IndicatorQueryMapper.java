package com.spider.exc.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spider.exc.domain.entity.IndicatorQuery;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标查询表Mapper
 */
@Mapper
public interface IndicatorQueryMapper extends BaseMapper<IndicatorQuery> {
}
