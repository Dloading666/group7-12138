package com.spider.exc.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spider.exc.domain.entity.TeachInvoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学用发票信息表Mapper
 */
@Mapper
public interface TeachInvoiceMapper extends BaseMapper<TeachInvoice> {
}
