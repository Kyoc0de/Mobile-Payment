package com.kyo.pay.service.impl;

import com.kyo.pay.PayApplicationTests;
import com.kyo.pay.service.IPayService;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;


public class PayServiceTest extends PayApplicationTests {

    @Autowired
    private PayServiceImpl payService;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void create() {
        //BigDecimal.valueOf(0.01)
        //new BigDecimal("0.01")  千万不要用 new BigDecimal(0.01)
        payService.create("1234567891234567", BigDecimal.valueOf(0.01), BestPayTypeEnum.WXPAY_NATIVE);
    }

    @Test
    public void sendMQMsg() {
        amqpTemplate.convertAndSend("payNotify", "hello");
    }
}