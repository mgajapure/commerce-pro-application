package com.commerce_pro_backend.common.data;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Runs each seed operation in its own NEW transaction so that
 * JPA entity state from one insert does not conflict with the next.
 *
 * This avoids "Row was updated or deleted by another transaction"
 * errors when inserting multiple child entities (e.g. sub-categories
 * under the same parent) in a single CommandLineRunner lambda.
 */
@Component
public class SeedTransactionHelper {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T run(Supplier<T> action) {
        return action.get();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runVoid(Runnable action) {
        action.run();
    }
}
