package com.posadskiy.email.resend;

/**
 * The request itself is wrong, so retrying it can never succeed.
 */
public class LinkNotAllowedException extends EmailDeliveryException {

    public LinkNotAllowedException(String message) {
        super(message);
    }
}
