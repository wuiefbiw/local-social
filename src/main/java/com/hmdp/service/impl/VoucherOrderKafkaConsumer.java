package com.hmdp.service.impl;

import com.hmdp.dto.VoucherOrderEvent;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.utils.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class VoucherOrderKafkaConsumer {

    @Resource
    private VoucherOrderServiceImpl voucherOrderService;

    @KafkaListener(topics = KafkaTopics.SECKILL_ORDER_CREATE, groupId = "hmdp-seckill-order")
    public void consume(VoucherOrderEvent event, Acknowledgment ack) {
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(event.getOrderId());
        voucherOrder.setUserId(event.getUserId());
        voucherOrder.setVoucherId(event.getVoucherId());

        try {
            voucherOrderService.handleVoucherOrder(voucherOrder);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka处理订单异常, orderId={}", event.getOrderId(), e);
            throw e;
        }
    }
}

