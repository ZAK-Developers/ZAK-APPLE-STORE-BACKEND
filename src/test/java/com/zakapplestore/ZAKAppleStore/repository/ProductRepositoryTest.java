package com.zakapplestore.ZAKAppleStore.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void loadsNewArrivalsWithoutSqlError() {
        assertThatCode(() -> productRepository.findNewArrivals(8)).doesNotThrowAnyException();
    }
}
