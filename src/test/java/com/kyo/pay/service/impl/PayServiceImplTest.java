package com.kyo.pay.service.impl;

import com.kyo.pay.PayApplicationTests;
import com.kyo.pay.service.IPayService;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;


public class PayServiceImplTest extends PayApplicationTests {

    @Autowired
    private IPayService payService;
    @Test
    public void create() {

        payService.create("12344123123198", BigDecimal.valueOf(0.01), BestPayTypeEnum.ALIPAY_APP);
    }
}