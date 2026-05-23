package com.zakapplestore.ZAKAppleStore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String email;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal grandTotal;
    private LocalDateTime createdAt;
    private String shippingAddress;
    private List<OrderItemSummaryResponse> items;
}
