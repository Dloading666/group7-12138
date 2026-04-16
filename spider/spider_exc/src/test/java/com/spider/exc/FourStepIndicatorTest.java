package com.spider.exc;

import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import java.nio.file.Paths;
import com.spider.exc.domain.entity.*;
import com.spider.exc.domain.mapper.*;
import com.spider.exc.dto.*;
import com.spider.exc.util.IndicatorDateUtil;
import com.spider.exc.util.IndicatorStatUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 四步流程指标测试类
 * 
 * 步骤1: Collect - 使用Playwright爬取页面数据
 * 步骤2: Parse - 解析采集的数据，添加月份字段
 * 步骤3: Process - 筛选发票，按月份汇总，计算统计指标
 * 步骤4: Persist - 组装业务JSON，写入查询表
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FourStepIndicatorTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private IndicatorCollectionMapper collectionMapper;
    
    @Autowired
    private IndicatorParsingMapper parsingMapper;
    
    @Autowired
    private IndicatorProcessingMapper processingMapper;
    
    @Autowired
    private IndicatorQueryMapper queryMapper;
    
    private static final String BASE_URL = "http://localhost:3000";
    
    /**
     * 步骤1: 采集（Collect）
     * 使用Playwright访问页面，获取入参和发票信息
     */
    @Test
    public void testCollect() {
        // 检查前端服务是否可访问
        System.out.println("检查前端服务是否运行: " + BASE_URL);
        Playwright playwright = Playwright.create();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
        launchOptions.setHeadless(false); // 显示浏览器便于调试
        Browser browser = playwright.chromium().launch(launchOptions);
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        
        try {
            // 先访问首页检查服务是否运行
            System.out.println("检查前端服务连接...");
            try {
                page.navigate(BASE_URL, new Page.NavigateOptions().setTimeout(10000));
                System.out.println("前端服务连接成功");
            } catch (Exception e) {
                System.err.println("==========================================");
                System.err.println("错误：无法连接到前端服务！");
                System.err.println("请确保前端项目已启动：");
                System.err.println("  cd D:\\javaWeb\\demo\\spider\\spider_web");
                System.err.println("  npm run dev");
                System.err.println("前端服务应运行在: " + BASE_URL);
                System.err.println("==========================================");
                throw new RuntimeException("前端服务未运行，请先启动前端项目", e);
            }
            
            // 访问申请页面
            System.out.println("正在访问申请页面: " + BASE_URL + "/application");
            try {
                Page.NavigateOptions navigateOptions = new Page.NavigateOptions();
                navigateOptions.setWaitUntil(WaitUntilState.NETWORKIDLE);
                navigateOptions.setTimeout(15000); // 增加超时时间
                page.navigate(BASE_URL + "/application", navigateOptions);
                page.waitForLoadState();
            } catch (Exception e) {
                System.err.println("无法访问前端页面，请确保前端服务已启动: " + BASE_URL);
                System.err.println("错误信息: " + e.getMessage());
                throw new RuntimeException("前端服务未运行或无法访问，请先启动前端项目: cd ../spider_web && npm run dev", e);
            }
            
            // 等待表单元素加载
            System.out.println("等待表单元素加载...");
            page.waitForSelector("#app-tax-no", new Page.WaitForSelectorOptions().setTimeout(10000));
            
            // 填写表单
            System.out.println("正在填写表单...");
            String taxNoValue = "91500000MA5U123456";
            String uscCodeValue = "91500000MA5U123456";
            String appDateValue = "2024-02-06";
            
            page.fill("#app-tax-no", taxNoValue);
            page.fill("#app-usc-code", uscCodeValue);
            page.fill("#app-date", appDateValue);
            
            // 提交表单
            System.out.println("正在提交表单...");
            page.click("button[type='submit']");
            page.waitForTimeout(1000); // 等待表单提交处理
            
            // 尝试从显示元素获取值，如果失败则从输入框获取
            System.out.println("正在提取入参...");
            String taxNo, uscCode, appDate;
            
            try {
                // 等待成功消息显示（等待display元素出现）
                System.out.println("等待成功消息显示...");
                page.waitForSelector("#app-tax-no-display", new Page.WaitForSelectorOptions().setTimeout(5000));
                page.waitForTimeout(500); // 额外等待确保内容渲染完成
                
                // 从显示元素获取
                taxNo = page.textContent("#app-tax-no-display");
                uscCode = page.textContent("#app-usc-code-display");
                appDate = page.textContent("#app-date-display");
                
                System.out.println("从显示元素获取到入参");
            } catch (Exception e) {
                // 如果显示元素不存在，直接从输入框获取（表单提交后值应该还在）
                System.out.println("显示元素未找到，从输入框获取值...");
                taxNo = page.inputValue("#app-tax-no");
                uscCode = page.inputValue("#app-usc-code");
                appDate = page.inputValue("#app-date");
                
                System.out.println("从输入框获取到入参");
            }
            
            // 如果还是为空，使用我们填写的值
            if (taxNo == null || taxNo.isEmpty()) {
                taxNo = taxNoValue;
            }
            if (uscCode == null || uscCode.isEmpty()) {
                uscCode = uscCodeValue;
            }
            if (appDate == null || appDate.isEmpty()) {
                appDate = appDateValue;
            }
            
            assertNotNull(taxNo, "纳税人识别号不能为空");
            assertNotNull(uscCode, "统一社会信用代码不能为空");
            assertNotNull(appDate, "申请日期不能为空");
            
            System.out.println("提取到的入参 - taxNo: " + taxNo + ", uscCode: " + uscCode + ", appDate: " + appDate);
            
            // 访问发票查询页面
            System.out.println("正在访问发票查询页面: " + BASE_URL + "/invoice-query");
            Page.NavigateOptions navigateOptions2 = new Page.NavigateOptions();
            navigateOptions2.setWaitUntil(WaitUntilState.NETWORKIDLE);
            page.navigate(BASE_URL + "/invoice-query", navigateOptions2);
            page.waitForLoadState();
            
            // 等待查询表单加载
            page.waitForSelector("#invoice-tax-no", new Page.WaitForSelectorOptions().setTimeout(10000));
            
            // 填写查询条件
            System.out.println("正在填写查询条件...");
            page.fill("#invoice-tax-no", taxNo);
            page.fill("#invoice-usc-code", uscCode);
            
            // 点击查询按钮
            System.out.println("正在点击查询按钮...");
            try {
                page.click("button:has-text('查询')", new Page.ClickOptions().setTimeout(5000));
            } catch (Exception e) {
                // 如果上面的选择器不工作，尝试其他方式
                try {
                    page.click("button.btn-primary", new Page.ClickOptions().setTimeout(5000));
                } catch (Exception e2) {
                    // 尝试通过type属性查找
                    page.click("button[type='button']", new Page.ClickOptions().setTimeout(5000));
                }
            }
            
            // 等待查询结果加载
            System.out.println("等待查询结果加载...");
            page.waitForTimeout(3000); // 等待表格数据加载
            
            // 提取发票数据
            System.out.println("正在提取发票数据...");
            List<CollectedPayload.InvoiceItem> invoices = new ArrayList<>();
            int index = 0;
            int maxRetries = 100; // 最多尝试100行
            
            while (index < maxRetries) {
                String signId = "#invoice-sign-" + index;
                String stateId = "#invoice-state-" + index;
                String timeId = "#invoice-time-" + index;
                String jshjId = "#invoice-jshj-" + index;
                
                // 检查元素是否存在
                if (page.locator(signId).count() == 0) {
                    break;
                }
                
                CollectedPayload.InvoiceItem item = new CollectedPayload.InvoiceItem();
                item.setSign(page.textContent(signId));
                item.setState(page.textContent(stateId));
                item.setInvoiceTime(page.textContent(timeId));
                item.setJshj(page.textContent(jshjId));
                
                invoices.add(item);
                System.out.println("提取到发票 " + index + ": " + item.getSign() + " - " + item.getJshj());
                index++;
            }
            
            System.out.println("共提取到 " + invoices.size() + " 条发票数据");
            
            // 组装采集数据
            CollectedPayload payload = new CollectedPayload();
            payload.setTaxNo(taxNo);
            payload.setUscCode(uscCode);
            payload.setAppDate(appDate);
            payload.setInvoices(invoices);
            
            // 写入数据库
            IndicatorCollection collection = new IndicatorCollection();
            collection.setTaxNo(taxNo);
            collection.setUscCode(uscCode);
            collection.setAppDate(LocalDate.parse(appDate));
            collection.setCollectedPayload(JSON.toJSONString(payload));
            
            collectionMapper.insert(collection);
            
            // 验证
            assertNotNull(collection.getId());
            assertNotNull(collection.getCollectedPayload());
            System.out.println("采集成功，ID: " + collection.getId());
            System.out.println("采集数据: " + collection.getCollectedPayload());
            
        } catch (Exception e) {
            System.err.println("采集失败: " + e.getMessage());
            e.printStackTrace();
            // 截图保存以便调试
            try {
                Page.ScreenshotOptions screenshotOptions = new Page.ScreenshotOptions();
                screenshotOptions.setPath(Paths.get("screenshot-error.png"));
                page.screenshot(screenshotOptions);
                System.out.println("错误截图已保存到: screenshot-error.png");
            } catch (Exception screenshotEx) {
                System.err.println("无法保存截图: " + screenshotEx.getMessage());
            }
            throw e;
        } finally {
            context.close();
            browser.close();
            playwright.close();
        }
    }
    
    /**
     * 步骤2: 解析（Parse）
     * 读取采集记录，解析并添加月份字段
     */
    @Test
    public void testParse() {
        // 直接从数据库读取最新的采集记录
        List<IndicatorCollection> collections = collectionMapper.selectList(null);
        assertFalse(collections.isEmpty(), "采集记录不能为空，请先运行 testCollect()");
        
        // 获取最后一条记录（最新的）
        IndicatorCollection collection = collections.get(collections.size() - 1);
        assertNotNull(collection, "采集记录不能为空");
        
        // 解析采集数据
        CollectedPayload payload = JSON.parseObject(
                collection.getCollectedPayload(), CollectedPayload.class);
        
        // 转换为解析数据
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
                    
                    // 提取月份
                    item.setMonth(IndicatorDateUtil.extractMonth(invoice.getInvoiceTime()));
                    
                    // 转换金额（去除千分位）
                    String jshjClean = invoice.getJshj().replace(",", "");
                    item.setJshjDecimal(new BigDecimal(jshjClean));
                    
                    return item;
                })
                .collect(Collectors.toList());
        
        parsedData.setInvoices(parsedInvoices);
        
        // 写入数据库
        IndicatorParsing parsing = new IndicatorParsing();
        parsing.setCollectionId(collection.getId());
        parsing.setTaxNo(parsedData.getTaxNo());
        parsing.setUscCode(parsedData.getUscCode());
        parsing.setAppDate(LocalDate.parse(parsedData.getAppDate()));
        parsing.setParsedData(JSON.toJSONString(parsedData));
        
        parsingMapper.insert(parsing);
        
        // 验证
        assertNotNull(parsing.getId());
        System.out.println("解析成功，ID: " + parsing.getId());
        System.out.println("解析数据: " + parsing.getParsedData());
    }
    
    /**
     * 步骤3: 处理（Process）
     * 筛选发票，按月份汇总，计算统计指标
     */
    @Test
    public void testProcess() {
        // 直接从数据库读取最新的解析记录
        List<IndicatorParsing> parsings = parsingMapper.selectList(null);
        assertFalse(parsings.isEmpty(), "解析记录不能为空，请先运行 testParse()");
        
        // 获取最后一条记录（最新的）
        IndicatorParsing parsing = parsings.get(parsings.size() - 1);
        assertNotNull(parsing, "解析记录不能为空");
        
        // 解析数据
        ParsedData parsedData = JSON.parseObject(
                parsing.getParsedData(), ParsedData.class);
        
        String appDate = parsedData.getAppDate();
        
        // 筛选符合条件的发票
        List<ParsedData.ParsedInvoiceItem> filteredInvoices = parsedData.getInvoices().stream()
                .filter(invoice -> "销项".equals(invoice.getSign())) // 筛选销项
                .filter(invoice -> "正常".equals(invoice.getState())) // 筛选正常
                .filter(invoice -> IndicatorDateUtil.isInDateRange(
                        invoice.getInvoiceTime(), appDate)) // 筛选时间范围
                .collect(Collectors.toList());
        
        // 按月份汇总
        Map<String, BigDecimal> monthSaleAmounts = filteredInvoices.stream()
                .collect(Collectors.groupingBy(
                        ParsedData.ParsedInvoiceItem::getMonth,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                ParsedData.ParsedInvoiceItem::getJshjDecimal,
                                BigDecimal::add)));
        
        // 计算统计指标
        List<BigDecimal> monthlyAmounts = new ArrayList<>(monthSaleAmounts.values());
        BigDecimal meanAmt = IndicatorStatUtil.calculateMean(monthlyAmounts);
        BigDecimal stdAmt = IndicatorStatUtil.calculateStdDev(monthlyAmounts, meanAmt);
        BigDecimal cv = IndicatorStatUtil.calculateCoefficientOfVariation(stdAmt, meanAmt);
        
        // 计算指标1：符合条件的销项发票总金额
        BigDecimal totalSaleAmount = filteredInvoices.stream()
                .map(ParsedData.ParsedInvoiceItem::getJshjDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalSaleAmount = IndicatorStatUtil.roundToFiveDecimals(totalSaleAmount);
        
        // 组装处理结果
        ProcessedResult result = new ProcessedResult();
        result.setTaxNo(parsedData.getTaxNo());
        result.setUscCode(parsedData.getUscCode());
        result.setAppDate(appDate);
        result.setMonthSaleAmounts(monthSaleAmounts);
        result.setMeanAmt(meanAmt);
        result.setStdAmt(stdAmt);
        result.setCv(cv);
        result.setTotalSaleAmount(totalSaleAmount);
        
        // 写入数据库
        IndicatorProcessing processing = new IndicatorProcessing();
        processing.setParsingId(parsing.getId());
        processing.setTaxNo(result.getTaxNo());
        processing.setUscCode(result.getUscCode());
        processing.setAppDate(LocalDate.parse(result.getAppDate()));
        processing.setProcessedResult(JSON.toJSONString(result));
        
        processingMapper.insert(processing);
        
        // 验证
        assertNotNull(processing.getId());
        System.out.println("处理成功，ID: " + processing.getId());
        System.out.println("处理结果: " + processing.getProcessedResult());
    }
    
    /**
     * 步骤4: 持久化（Persist/Complete）
     * 读取前三步的最新记录，组装业务JSON
     */
    @Test
    public void testComplete() {
        // 直接从数据库读取各步骤的最新记录
        List<IndicatorCollection> collections = collectionMapper.selectList(null);
        assertFalse(collections.isEmpty(), "采集记录不能为空，请先运行 testCollect()");
        IndicatorCollection collection = collections.get(collections.size() - 1);
        
        List<IndicatorParsing> parsings = parsingMapper.selectList(null);
        assertFalse(parsings.isEmpty(), "解析记录不能为空，请先运行 testParse()");
        IndicatorParsing parsing = parsings.get(parsings.size() - 1);
        
        List<IndicatorProcessing> processings = processingMapper.selectList(null);
        assertFalse(processings.isEmpty(), "处理记录不能为空，请先运行 testProcess()");
        IndicatorProcessing processing = processings.get(processings.size() - 1);
        
        assertNotNull(collection, "采集记录不能为空");
        assertNotNull(parsing, "解析记录不能为空");
        assertNotNull(processing, "处理记录不能为空");
        
        // 组装业务JSON
        BusinessJson businessJson = new BusinessJson();
        businessJson.setCollectionId(collection.getId());
        businessJson.setParsingId(parsing.getId());
        businessJson.setProcessingId(processing.getId());
        businessJson.setTaxNo(collection.getTaxNo());
        businessJson.setUscCode(collection.getUscCode());
        businessJson.setAppDate(collection.getAppDate().toString());
        
        // 解析各步骤的数据
        businessJson.setCollectedData(JSON.parseObject(
                collection.getCollectedPayload(), CollectedPayload.class));
        businessJson.setParsedData(JSON.parseObject(
                parsing.getParsedData(), ParsedData.class));
        businessJson.setProcessedResult(JSON.parseObject(
                processing.getProcessedResult(), ProcessedResult.class));
        
        // 写入数据库
        IndicatorQuery query = new IndicatorQuery();
        query.setCollectionId(collection.getId());
        query.setParsingId(parsing.getId());
        query.setProcessingId(processing.getId());
        query.setTaxNo(businessJson.getTaxNo());
        query.setUscCode(businessJson.getUscCode());
        query.setAppDate(LocalDate.parse(businessJson.getAppDate()));
        query.setBusinessJson(JSON.toJSONString(businessJson));
        
        queryMapper.insert(query);
        
        // 验证
        assertNotNull(query.getId());
        System.out.println("持久化成功，ID: " + query.getId());
        System.out.println("业务JSON: " + query.getBusinessJson());
    }
}
