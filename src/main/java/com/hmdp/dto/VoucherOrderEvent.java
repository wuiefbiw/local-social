package com.hmdp.dto;
import java.io.Serializable;
import lombok.Data;

@Data
public class VoucherOrderEvent implements Serializable {

    private Long voucherId;
    private Long userId;
    private Long orderId;
}


