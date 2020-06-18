package com.emc.ecs.servicebroker;

import java.util.NoSuchElementException;

public class EcsManagementResourceNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public EcsManagementResourceNotFoundException(String message) {
        super(message);
    }

    public EcsManagementResourceNotFoundException(NoSuchElementException e) {
        super(e);
    }
}
