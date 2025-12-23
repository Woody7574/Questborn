package ua.woody.questborn.addons.chains;

import ua.woody.questborn.addons.api.AddonContext;
import ua.woody.questborn.addons.api.QuestbornAddon;

public final class ChainsAddon implements QuestbornAddon {

    @Override
    public String id() {
        return "chains";
    }

    @Override
    public void onLoad(AddonContext ctx) {
        ctx.logger().info("[Chains] addon loaded");
    }

    @Override
    public void onEnable() {
        // nothing
    }

    @Override
    public void onDisable() {
        // nothing
    }
}
