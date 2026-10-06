package ua.woody.questborn.model.npc.mood;

import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.model.NpcConfig;
import net.milkbowl.vault.economy.Economy;

public class MoneyCondition implements MoodCondition {
    private final String operator;
    private final double amount;
    private final String expression;

    public MoneyCondition(String expression) {
        this.expression = expression;
        expression = expression.trim();
        if (expression.startsWith(">=")) {
            this.operator = ">=";
            this.amount = Double.parseDouble(expression.substring(2).trim());
        } else if (expression.startsWith("<=")) {
            this.operator = "<=";
            this.amount = Double.parseDouble(expression.substring(2).trim());
        } else if (expression.startsWith(">")) {
            this.operator = ">";
            this.amount = Double.parseDouble(expression.substring(1).trim());
        } else if (expression.startsWith("<")) {
            this.operator = "<";
            this.amount = Double.parseDouble(expression.substring(1).trim());
        } else if (expression.startsWith("=")) {
            this.operator = "=";
            this.amount = Double.parseDouble(expression.substring(1).trim());
        } else {
            this.operator = ">=";
            this.amount = Double.parseDouble(expression);
        }
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        Economy econ = QuestbornPlugin.getInstance().getEconomy();
        if (econ == null) return false;

        double bal = econ.getBalance(player);
        switch (operator) {
            case ">=": return bal >= amount;
            case "<=": return bal <= amount;
            case ">": return bal > amount;
            case "<": return bal < amount;
            case "=": return Math.abs(bal - amount) < 0.01;
            default: return false;
        }
    }
}
