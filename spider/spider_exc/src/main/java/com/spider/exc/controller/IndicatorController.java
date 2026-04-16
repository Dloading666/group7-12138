package com.spider.exc.controller;

import com.spider.exc.domain.entity.IndicatorCollection;
import com.spider.exc.domain.entity.IndicatorParsing;
import com.spider.exc.domain.entity.IndicatorProcessing;
import com.spider.exc.domain.entity.IndicatorQuery;
import com.spider.exc.domain.mapper.IndicatorCollectionMapper;
import com.spider.exc.domain.mapper.IndicatorParsingMapper;
import com.spider.exc.domain.mapper.IndicatorProcessingMapper;
import com.spider.exc.domain.mapper.IndicatorQueryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 指标数据查询接口
 * 用于测试类调用，读取各步骤的数据
 */
@RestController
@RequestMapping("/api/indicator")
public class IndicatorController {
    
    @Autowired
    private IndicatorCollectionMapper collectionMapper;
    
    @Autowired
    private IndicatorParsingMapper parsingMapper;
    
    @Autowired
    private IndicatorProcessingMapper processingMapper;
    
    @Autowired
    private IndicatorQueryMapper queryMapper;
    
    /**
     * 获取最新的采集记录
     */
    @GetMapping("/collection/latest")
    public IndicatorCollection getLatestCollection() {
        List<IndicatorCollection> list = collectionMapper.selectList(null);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }
    
    /**
     * 根据ID获取采集记录
     */
    @GetMapping("/collection/{id}")
    public IndicatorCollection getCollectionById(@PathVariable Long id) {
        return collectionMapper.selectById(id);
    }
    
    /**
     * 获取最新的解析记录
     */
    @GetMapping("/parsing/latest")
    public IndicatorParsing getLatestParsing() {
        List<IndicatorParsing> list = parsingMapper.selectList(null);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }
    
    /**
     * 获取最新的处理记录
     */
    @GetMapping("/processing/latest")
    public IndicatorProcessing getLatestProcessing() {
        List<IndicatorProcessing> list = processingMapper.selectList(null);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }
    
    /**
     * 获取最新的查询记录
     */
    @GetMapping("/query/latest")
    public IndicatorQuery getLatestQuery() {
        List<IndicatorQuery> list = queryMapper.selectList(null);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }
}
