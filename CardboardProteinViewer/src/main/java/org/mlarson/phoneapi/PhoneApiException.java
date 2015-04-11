/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mlarson.phoneapi;

/**
 *
 * @author mlarson
 */
public class PhoneApiException extends Exception {
    private String message = null;

    public PhoneApiException() {
        super();
    }

    public PhoneApiException(String message) {
        super(message);
        this.message = message;
    }

    public PhoneApiException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
