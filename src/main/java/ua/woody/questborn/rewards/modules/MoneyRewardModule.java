package ua.woody.questborn.rewards.modules;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import ua.woody.questborn.rewards.RewardExecutionContext;
import ua.woody.questborn.rewards.RewardHandler;
import ua.woody.questborn.rewards.RewardModule;

import java.util.Map;

public class MoneyRewardModule implements RewardModule {
    @Override
    public String getKey() {
        return "money";
    }

    @Override
    public void execute(RewardExecutionContext ctx, Player player, Object config) {
        if (config == null)
            return;

        double amount;
        boolean silent = false;

        if (config instanceof Map<?, ?> map) {
            silent = RewardHandler.getSilent(map);

            Object rawAmount = map.get("amount");
            if (rawAmount == null) {
                player.sendMessage(ctx.getLang().tr("rewards.money.invalid"));
                return;
            }

            try {
                amount = Double.parseDouble(String.valueOf(rawAmount));
            } catch (Exception ex) {
                player.sendMessage(ctx.getLang().tr("rewards.money.invalid"));
                return;
            }
        } else {
            try {
                amount = Double.parseDouble(String.valueOf(config));
            } catch (Exception ex) {
                player.sendMessage(ctx.getLang().tr("rewards.money.invalid"));
                return;
            }
        }

        if (amount <= 0)
            return;

        amount *= ctx.getMultiplier();

        Economy econ = RewardHandler.getEconomy();
        if (econ == null) {
            boolean vaultEnabled = ctx.getPlugin().getConfig().getBoolean("integration.vault.enabled", true);
            if (vaultEnabled) {
                ctx.getPlugin().getLogger().warning("Vault economy provider not found. Money reward skipped.");
            }
            return;
        }

        EconomyResponse resp = econ.depositPlayer(player, amount);
        if (!resp.transactionSuccess()) {
            ctx.getPlugin().getLogger().warning("Money deposit failed for " + player.getName()
                    + " (amount=" + amount + "): " + resp.errorMessage);
            return;
        }

        if (player != null && !silent && !ctx.isSilent()) {
            String formatPattern = ctx.getPlugin().getConfig().getString("rewards.money-format", "#,##0.##");
            String formattedAmount;
            try {
                java.text.DecimalFormat df = new java.text.DecimalFormat(formatPattern);
                formattedAmount = df.format(amount);
            } catch (Exception e) {
                formattedAmount = String.valueOf(amount);
            }

            String msg = ctx.lang().tr("rewards.money.added", Map.of("amount", formattedAmount));
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        }
    }
}
