/*
 * Copyright Anatoly Starostin (c) 2017.
 */

package treeton.core.config.context.resources;

public class ResourceInstantiationException extends Exception {
    String resourceName;

    public ResourceInstantiationException(String message) {
        super(message);
    }

    public ResourceInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceInstantiationException(String resourceName, String message) {
        super(message);
        this.resourceName = resourceName;
    }

    public ResourceInstantiationException(String resourceName, String message, Throwable cause) {
        super(message, cause);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getMessage() {
        return super.getMessage() + " (resource: " + resourceName + ")";
    }

    public String toString() {
        return super.toString() + " (resource: " + resourceName + ")";
    }

}
