package com.foodordering.order_microservice.exception;

public class InvalidOrderStateException
        extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}