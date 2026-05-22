package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByRequestId(String requestId);
}
