package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import ua.woody.questborn.util.PlaceholderUtil;

public class PlaceholderCondition implements MoodCondition {
    private final String placeholder;
    private final String expression;

    public PlaceholderCondition(String placeholder, String expression) {
        this.placeholder = placeholder;
        this.expression = expression != null ? expression.trim() : "";
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        if (player == null || placeholder == null) return false;
        QuestbornPlugin plugin = QuestbornPlugin.getInstance();
        if (plugin == null) return false;

        String resolved = PlaceholderUtil.format(plugin, player, placeholder);
        if (resolved == null) return false;
        resolved = resolved.trim();

        String expr = this.expression;
        String op = "==";

        if (expr.startsWith(">=")) { op = ">="; expr = expr.substring(2).trim(); }
        else if (expr.startsWith("<=")) { op = "<="; expr = expr.substring(2).trim(); }
        else if (expr.startsWith("!=")) { op = "!="; expr = expr.substring(2).trim(); }
        else if (expr.startsWith(">")) { op = ">"; expr = expr.substring(1).trim(); }
        else if (expr.startsWith("<")) { op = "<"; expr = expr.substring(1).trim(); }
        else if (expr.startsWith("=")) { op = "=="; expr = expr.substring(1).trim(); }
        else if (expr.startsWith("==")) { op = "=="; expr = expr.substring(2).trim(); }

        try {
            double resolvedNum = Double.parseDouble(resolved);
            double exprNum = Double.parseDouble(expr);
            switch (op) {
                case ">=": return resolvedNum >= exprNum;
                case "<=": return resolvedNum <= exprNum;
                case ">": return resolvedNum > exprNum;
                case "<": return resolvedNum < exprNum;
                case "!=": return Math.abs(resolvedNum - exprNum) >= 0.001;
                default: return Math.abs(resolvedNum - exprNum) < 0.001;
            }
        } catch (NumberFormatException e) {
            if ("!=".equals(op)) {
                return !resolved.equalsIgnoreCase(expr);
            }
            return resolved.equalsIgnoreCase(expr);
        }
    }
}
