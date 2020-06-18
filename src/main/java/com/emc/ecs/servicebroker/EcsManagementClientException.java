package com.emc.ecs.servicebroker;

public class EcsManagementClientException extends Exception {
    private static final long serialVersionUID = 1L;

    public EcsManagementClientException(String message) {
        super(message);
    }

    public EcsManagementClientException(Exception e) {
        super(e);
    }
}