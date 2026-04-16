package com.spider.exc;

import com.spider.exc.dto.Indicator1Result;
import com.spider.exc.dto.Indicator2Result;
import com.spider.exc.dto.Indicator3Result;
import com.spider.exc.service.IndicatorCalculateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指标计算功能测试类
 */
@SpringBootTest
public class IndicatorCalculateTest {
    
    @Autowired
    private IndicatorCalculateService calculateService;
    
    /**
     * 测试指标一：经营规模指标
     * 使用测试数据：税号 91110000MA01ABCD00，申请日期 2024-03-01
     */
    @Test
    public void testIndicator1() {
        String taxNo = "91110000MA01ABCD00";
        String uscCode = "91110000MA01ABCD00";
        LocalDate appDate = LocalDate.of(2024, 3, 1);
        
        Indicator1Result result = calculateService.calculateIndicator1(taxNo, uscCode, appDate);
        
        assertNotNull(result);
        assertNotNull(result.getSaleJshjSum());
        assertTrue(result.getSaleJshjSum().compareTo(BigDecimal.ZERO) >= 0);
        
        System.out.println("指标一结果：");
        System.out.println("  销项发票总额: " + result.getSaleJshjSum());
        System.out.println("  说明: " + result.getComment());
    }
    
    /**
     * 测试指标二：经营稳定性指标
     */
    @Test
    public void testIndicator2() {
        String taxNo = "91110000MA01ABCD00";
        String uscCode = "91110000MA01ABCD00";
        LocalDate appDate = LocalDate.of(2024, 3, 1);
        
        Indicator2Result result = calculateService.calculateIndicator2(taxNo, uscCode, appDate);
        
        assertNotNull(result);
        assertNotNull(result.getMeanAmt());
        assertNotNull(result.getStdAmt());
        assertNotNull(result.getCv());
        
        System.out.println("指标二结果：");
        System.out.println("  月均金额: " + result.getMeanAmt());
        System.out.println("  标准差: " + result.getStdAmt());
        System.out.println("  波动系数: " + result.getCv());
        System.out.println("  说明: " + result.getComment());
    }
    
    /**
     * 测试指标三：贷款决策
     */
    @Test
    public void testIndicator3() {
        String taxNo = "91110000MA01ABCD00";
        String uscCode = "91110000MA01ABCD00";
        LocalDate appDate = LocalDate.of(2024, 3, 1);
        
        Indicator3Result result = calculateService.calculateIndicator3(taxNo, uscCode, appDate);
        
        assertNotNull(result);
        assertNotNull(result.getIsLoanable());
        assertNotNull(result.getLoanLimit());
        assertNotNull(result.getReason());
        assertNotNull(result.getFeatures());
        
        System.out.println("指标三结果：");
        System.out.println("  是否可贷: " + result.getIsLoanable());
        System.out.println("  贷款额度: " + result.getLoanLimit());
        System.out.println("  决策原因: " + result.getReason());
        System.out.println("  特征值: " + result.getFeatures());
    }
    
    /**
     * 测试边界场景：经营规模不足
     */
    @Test
    public void testIndicator3_LowScale() {
        // 这个测试需要数据库中有小金额的发票数据
        // 或者可以修改测试数据来验证
        String taxNo = "91110000MA01ABCD00";
        String uscCode = "91110000MA01ABCD00";
        LocalDate appDate = LocalDate.of(2024, 3, 1);
        
        Indicator3Result result = calculateService.calculateIndicator3(taxNo, uscCode, appDate);
        
        // 如果总额小于50000，应该不可贷
        if (result.getFeatures().get("sale_jshj_sum") != null) {
            BigDecimal saleJshjSum = (BigDecimal) result.getFeatures().get("sale_jshj_sum");
            if (saleJshjSum.compareTo(new BigDecimal("50000")) < 0) {
                assertFalse(result.getIsLoanable());
                assertEquals(BigDecimal.ZERO.setScale(5), result.getLoanLimit());
            }
        }
    }
}
