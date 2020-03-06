package com.kyo.pay.service.impl;

import com.google.gson.Gson;
import com.kyo.pay.dao.PayInfoMapper;
import com.kyo.pay.enums.PayPlatformEnum;
import com.kyo.pay.pojo.PayInfo;
import com.kyo.pay.service.IPayService;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.enums.BestPayPlatformEnum;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PayServiceImpl implements IPayService {

    private final static String QUEUE_PAY_NOTIFY ="payNotify";

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PayResponse create(String orderId, BigDecimal amount, BestPayTypeEnum bestPayTypeEnum) {



        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                PayPlatformEnum.getByBestPayTypeEnum(bestPayTypeEnum).getCode(),
                OrderStatusEnum.NOTPAY.name(),
                amount);
        payInfoMapper.insertSelective(payInfo);

        //设置请求数据
        PayRequest request = new PayRequest();
        request.setOrderName("4559066-payme");
        request.setOrderAmount(amount.doubleValue());
        request.setOrderId(orderId);
        request.setPayTypeEnum(bestPayTypeEnum);

        //发起请求
        PayResponse response = bestPayService.pay(request);

        log.info("response={}",response);
        return response;
    }

    @Override
    public String asyncNotify(String notifyData) {
        //1.签名校验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("responseData={}",payResponse);

        //2.金额校验 对比数据库数据
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if(payInfo == null){
            throw new RuntimeException("通过orderNo查询到的结果是null");
        }
        //如果订单支付状态不是"已支付"
        if (!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())) {
            //Double类型比较大小，精度。1.00  1.0
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
                //告警
                throw new RuntimeException("异步通知中的金额和数据库里的不一致，orderNo=" + payResponse.getOrderId());
            }


            //3.修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            payInfo.setUpdateTime(null);
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }

        amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY,new Gson().toJson(payInfo));

        //4.通知微信结束通知

        if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.WX) {
            //4. 告诉微信不要再通知了
            return "<xml>\n" +
                    "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                    "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                    "</xml>";
        }else if (payResponse.getPayPlatformEnum() == BestPayPlatformEnum.ALIPAY) {
            return "success";
        }

        throw new RuntimeException("异步通知中错误的支付平台");
    }
    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }
}
