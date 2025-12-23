package ua.woody.questborn.model;

import java.util.ArrayList;
import java.util.List;

public class QuestAvailability {

    private final List<String> after;
    private final AfterMode afterMode;
    private final String permission;
    private final String permissionMessage;

    public QuestAvailability(List<String> after, AfterMode afterMode, String permission, String permissionMessage) {
        this.after = after != null ? after : new ArrayList<>();
        this.afterMode = afterMode != null ? afterMode : AfterMode.ALL;
        this.permission = permission;
        this.permissionMessage = permissionMessage;
    }

    public List<String> getAfter() { return after; }
    public AfterMode getAfterMode() { return afterMode; }
    public String getPermission() { return permission; }
    public String getPermissionMessage() { return permissionMessage; }

    public boolean isEmpty() {
        return (after == null || after.isEmpty()) &&
                (permission == null || permission.isBlank());
    }
}
