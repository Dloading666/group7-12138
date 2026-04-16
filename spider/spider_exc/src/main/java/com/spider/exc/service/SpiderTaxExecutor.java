package com.spider.exc.service;

import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.spider.exc.domain.entity.*;
import com.spider.exc.domain.mapper.*;
import com.spider.exc.dto.*;
import com.spider.exc.util.IndicatorDateUtil;
import com.spider.exc.util.IndicatorStatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 税务爬虫四步执行器
 * Step1: Collect — Playwright 爬取 spider_web 页面
 * Step2: Parse  — 解析数据，添加月份字段
 * Step3: Process — 筛选发票，计算统计指标
 * Step4: Persist — 组装业务JSON，写入 rpa_indicator_query
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpiderTaxExecutor {

    private static final String SPIDER_WEB_URL = "http://localhost:3005";

    private final IndicatorCollectionMapper collectionMapper;
    private final IndicatorParsingMapper parsingMapper;
    private final IndicatorProcessingMapper processingMapper;
    private final IndicatorQueryMapper queryMapper;
    private final SpiderTaskMapper spiderTaskMapper;

    /**
     * 执行完整四步流程
     */
    @Transactional
    public void execute(SpiderTask spiderTask) {
        log.info("开始执行四步流程: taskId={}", spiderTask.getTaskId());

        String taxNo = spiderTask.getTaxNo();
        String uscCode = spiderTask.getUscCode();
        String appDate = spiderTask.getAppDate() != null ? spiderTask.getAppDate().toString() : "2024-02-06";

        // ====== Step 1: Collect ======
        IndicatorCollection collection = step1Collect(taxNo, uscCode, appDate);
        if (collection == null || collection.getId() == null) {
            throw new RuntimeException("采集步骤失败");
        }
        spiderTask.setCollectionId(collection.getId());
        spiderTaskMapper.updateById(spiderTask);
        log.info("Step1 完成: collectionId={}", collection.getId());

        // ====== Step 2: Parse ======
        IndicatorParsing parsing = step2Parse(collection);
        if (parsing == null || parsing.getId() == null) {
            throw new RuntimeException("解析步骤失败");
        }
        spiderTask.setParsingId(parsing.getId());
        spiderTaskMapper.updateById(spiderTask);
        log.info("Step2 完成: parsingId={}", parsing.getId());

        // ====== Step 3: Process ======
        IndicatorProcessing processing = step3Process(parsing);
        if (processing == null || processing.getId() == null) {
            throw new RuntimeException("处理步骤失败");
        }
        spiderTask.setProcessingId(processing.getId());
        spiderTaskMapper.updateById(spiderTask);
        log.info("Step3 完成: processingId={}", processing.getId());

        // ====== Step 4: Persist ======
        IndicatorQuery query = step4Persist(collection, parsing, processing);
        if (query == null || query.getId() == null) {
            throw new RuntimeException("持久化步骤失败");
        }
        spiderTask.setQueryId(query.getId());
        spiderTaskMapper.updateById(spiderTask);
        log.info("Step4 完成: queryId={}", query.getId());

        log.info("四步流程全部完成: taskId={}", spiderTask.getTaskId());
    }

    /**
     * Step 1: 使用 Playwright 采集页面数据
     */
    private IndicatorCollection step1Collect(String taxNo, String uscCode, String appDate) {
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
        launchOptions.setHeadless(true);
        Browser browser = playwright.chromium().launch(launchOptions);
        BrowserContext context = browser.newContext();
        Page page = context.newPage();

        try {
            // 1. 访问 spider_web 首页
            log.info("访问 spider_web 首页: {}", SPIDER_WEB_URL);
            page.navigate(SPIDER_WEB_URL, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE)
                    .setTimeout(15000));

            // 1.5 访问企业信息页面，查询企业名称
            log.info("访问企业信息页面...");
            page.navigate(SPIDER_WEB_URL + "/enterprise-info",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15000));
            page.waitForSelector("#enterprise-tax-no", new Page.WaitForSelectorOptions().setTimeout(10000));
            page.fill("#enterprise-tax-no", taxNo != null ? taxNo : "91500000MA5U123456");
            page.click("button:has-text('查询')", new Page.ClickOptions().setTimeout(5000));
            page.waitForTimeout(1000);
            String enterpriseName = "";
            try {
                enterpriseName = page.textContent("#enterprise-name", new Page.TextContentOptions().setTimeout(3000));
                if (enterpriseName == null || enterpriseName.isEmpty()) {
                    enterpriseName = "重庆某某科技有限公司"; // 默认值
                }
            } catch (Exception e) {
                enterpriseName = "重庆某某科技有限公司"; // 默认值
            }
            log.info("采集到企业名称: {}", enterpriseName);

            // 2. 访问申请页面，填写表单并提交
            log.info("访问申请页面...");
            page.navigate(SPIDER_WEB_URL + "/application",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15000));
            page.waitForSelector("#app-tax-no", new Page.WaitForSelectorOptions().setTimeout(10000));

            page.fill("#app-tax-no", taxNo != null ? taxNo : "91500000MA5U123456");
            page.fill("#app-usc-code", uscCode != null ? uscCode : "91500000MA5U123456");
            page.fill("#app-date", appDate);
            page.click("button[type='submit']");
            page.waitForTimeout(1000);

            // 3. 访问发票查询页面
            log.info("访问发票查询页面...");
            page.navigate(SPIDER_WEB_URL + "/invoice-query",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(15000));
            page.waitForSelector("#invoice-tax-no", new Page.WaitForSelectorOptions().setTimeout(10000));

            page.fill("#invoice-tax-no", taxNo != null ? taxNo : "91500000MA5U123456");
            page.fill("#invoice-usc-code", uscCode != null ? uscCode : "91500000MA5U123456");

            // 点击查询
            try {
                page.click("button:has-text('查询')", new Page.ClickOptions().setTimeout(5000));
            } catch (Exception e) {
                page.click("button.btn-primary", new Page.ClickOptions().setTimeout(5000));
            }
            page.waitForTimeout(3000);

            // 4. 提取发票数据
            log.info("提取发票数据...");
            List<CollectedPayload.InvoiceItem> invoices = new ArrayList<>();
            int index = 0;
            while (index < 100) {
                String signId = "#invoice-sign-" + index;
                if (page.locator(signId).count() == 0) {
                    break;
                }
                CollectedPayload.InvoiceItem item = new CollectedPayload.InvoiceItem();
                item.setSign(page.textContent(signId));
                item.setState(page.textContent("#invoice-state-" + index));
                item.setInvoiceTime(page.textContent("#invoice-time-" + index));
                item.setJshj(page.textContent("#invoice-jshj-" + index));
                invoices.add(item);
                log.debug("提取发票 {}: sign={}, jshj={}", index, item.getSign(), item.getJshj());
                index++;
            }
            log.info("共提取 {} 条发票", invoices.size());

            // 5. 写入数据库
            CollectedPayload payload = new CollectedPayload();
            payload.setTaxNo(taxNo);
            payload.setUscCode(uscCode);
            payload.setAppDate(appDate);
            payload.setEnterpriseName(enterpriseName);
            payload.setInvoices(invoices);

            IndicatorCollection collection = new IndicatorCollection();
            collection.setTaxNo(taxNo);
            collection.setUscCode(uscCode);
            collection.setAppDate(LocalDate.parse(appDate));
            collection.setCollectedPayload(JSON.toJSONString(payload));
            collectionMapper.insert(collection);

            return collection;

        } catch (Exception e) {
            log.error("Step1 Collect 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Step1 Collect 失败: " + e.getMessage(), e);
        } finally {
            context.close();
            browser.close();
            playwright.close();
        }
    }

    /**
     * Step 2: 解析采集数据，添加月份字段
     */
    private IndicatorParsing step2Parse(IndicatorCollection collection) {
        try {
            CollectedPayload payload = JSON.parseObject(
                    collection.getCollectedPayload(), CollectedPayload.class);

            ParsedData parsedData = new ParsedData();
            parsedData.setTaxNo(payload.getTaxNo());
            parsedData.setUscCode(payload.getUscCode());
            parsedData.setAppDate(payload.getAppDate());

            List<ParsedData.ParsedInvoiceItem> parsedInvoices = payload.getInvoices().stream()
                    .map(invoice -> {
                        ParsedData.ParsedInvoiceItem item = new ParsedData.ParsedInvoiceItem();
                        item.setSign(invoice.getSign());
                        item.setState(invoice.getState());
                        item.setInvoiceTime(invoice.getInvoiceTime());
                        item.setJshj(invoice.getJshj());
                        item.setMonth(IndicatorDateUtil.extractMonth(invoice.getInvoiceTime()));
                        String jshjClean = invoice.getJshj().replace(",", "");
                        item.setJshjDecimal(new BigDecimal(jshjClean));
                        return item;
                    })
                    .collect(Collectors.toList());

            parsedData.setInvoices(parsedInvoices);

            IndicatorParsing parsing = new IndicatorParsing();
            parsing.setCollectionId(collection.getId());
            parsing.setTaxNo(parsedData.getTaxNo());
            parsing.setUscCode(parsedData.getUscCode());
            parsing.setAppDate(LocalDate.parse(parsedData.getAppDate()));
            parsing.setParsedData(JSON.toJSONString(parsedData));
            parsingMapper.insert(parsing);

            return parsing;
        } catch (Exception e) {
            log.error("Step2 Parse 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Step2 Parse 失败: " + e.getMessage(), e);
        }
    }

    /**
     * Step 3: 筛选发票，计算统计指标
     */
    private IndicatorProcessing step3Process(IndicatorParsing parsing) {
        try {
            ParsedData parsedData = JSON.parseObject(
                    parsing.getParsedData(), ParsedData.class);
            String appDate = parsedData.getAppDate();

            // 筛选条件：销项 + 正常 + 在申请日期前1-12个月
            List<ParsedData.ParsedInvoiceItem> filtered = parsedData.getInvoices().stream()
                    .filter(inv -> "销项".equals(inv.getSign()))
                    .filter(inv -> "正常".equals(inv.getState()))
                    .filter(inv -> IndicatorDateUtil.isInDateRange(inv.getInvoiceTime(), appDate))
                    .collect(Collectors.toList());

            // 按月份汇总
            Map<String, BigDecimal> monthAmounts = filtered.stream()
                    .collect(Collectors.groupingBy(
                            ParsedData.ParsedInvoiceItem::getMonth,
                            Collectors.reducing(BigDecimal.ZERO,
                                    ParsedData.ParsedInvoiceItem::getJshjDecimal,
                                    BigDecimal::add)));

            // 统计指标
            List<BigDecimal> monthlyAmounts = new ArrayList<>(monthAmounts.values());
            BigDecimal meanAmt = IndicatorStatUtil.calculateMean(monthlyAmounts);
            BigDecimal stdAmt = IndicatorStatUtil.calculateStdDev(monthlyAmounts, meanAmt);
            BigDecimal cv = IndicatorStatUtil.calculateCoefficientOfVariation(stdAmt, meanAmt);

            BigDecimal totalSaleAmount = filtered.stream()
                    .map(ParsedData.ParsedInvoiceItem::getJshjDecimal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalSaleAmount = IndicatorStatUtil.roundToFiveDecimals(totalSaleAmount);

            ProcessedResult result = new ProcessedResult();
            result.setTaxNo(parsedData.getTaxNo());
            result.setUscCode(parsedData.getUscCode());
            result.setAppDate(appDate);
            result.setMonthSaleAmounts(monthAmounts);
            result.setMeanAmt(meanAmt);
            result.setStdAmt(stdAmt);
            result.setCv(cv);
            result.setTotalSaleAmount(totalSaleAmount);

            IndicatorProcessing processing = new IndicatorProcessing();
            processing.setParsingId(parsing.getId());
            processing.setTaxNo(result.getTaxNo());
            processing.setUscCode(result.getUscCode());
            processing.setAppDate(LocalDate.parse(result.getAppDate()));
            processing.setProcessedResult(JSON.toJSONString(result));
            processingMapper.insert(processing);

            return processing;
        } catch (Exception e) {
            log.error("Step3 Process 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Step3 Process 失败: " + e.getMessage(), e);
        }
    }

    /**
     * Step 4: 组装业务JSON，写入查询表
     */
    private IndicatorQuery step4Persist(IndicatorCollection collection,
                                        IndicatorParsing parsing,
                                        IndicatorProcessing processing) {
        try {
            BusinessJson businessJson = new BusinessJson();
            businessJson.setCollectionId(collection.getId());
            businessJson.setParsingId(parsing.getId());
            businessJson.setProcessingId(processing.getId());
            businessJson.setTaxNo(collection.getTaxNo());
            businessJson.setUscCode(collection.getUscCode());
            businessJson.setAppDate(collection.getAppDate().toString());
            businessJson.setCollectedData(JSON.parseObject(collection.getCollectedPayload(), CollectedPayload.class));
            businessJson.setParsedData(JSON.parseObject(parsing.getParsedData(), ParsedData.class));
            businessJson.setProcessedResult(JSON.parseObject(processing.getProcessedResult(), ProcessedResult.class));

            IndicatorQuery query = new IndicatorQuery();
            query.setCollectionId(collection.getId());
            query.setParsingId(parsing.getId());
            query.setProcessingId(processing.getId());
            query.setTaxNo(businessJson.getTaxNo());
            query.setUscCode(businessJson.getUscCode());
            query.setAppDate(LocalDate.parse(businessJson.getAppDate()));
            query.setBusinessJson(JSON.toJSONString(businessJson));
            queryMapper.insert(query);

            return query;
        } catch (Exception e) {
            log.error("Step4 Persist 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Step4 Persist 失败: " + e.getMessage(), e);
        }
    }
}
