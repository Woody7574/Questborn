package ua.woody.questborn.addons.api;

public class AvailabilityResult {

    private final boolean allowed;
    private final String message;

    private AvailabilityResult(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public static AvailabilityResult ok() {
        return new AvailabilityResult(true, null);
    }

    public static AvailabilityResult deny(String message) {
        return new AvailabilityResult(false, message);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMessage() {
        return message;
    }
}
