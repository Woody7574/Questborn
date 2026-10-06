package ua.woody.questborn.managers;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.lang.ColorFormatter;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestDefinition;
import ua.woody.questborn.model.QuestItem;
import ua.woody.questborn.model.QuestObjective;
import ua.woody.questborn.util.ItemDisplayUtil;
import ua.woody.questborn.util.QuestDisplayBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestNotificationService {
    private final QuestbornPlugin plugin;
    private final QuestManager questManager;
    private final LanguageManager lang;
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public QuestNotificationService(QuestbornPlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
        this.lang = plugin.getLanguage();
    }

    public void sendQuestActivationMessage(Player player, QuestDefinition quest, boolean showActivationDetails) {
        List<String> template = lang.trList("quests.activate.success",
                Map.of("quest", quest.getDisplayName()));

        if (template.isEmpty()) {
            return;
        }

        List<String> objectiveDetails = getLimitedObjectiveDetails(player, quest);
        List<String> lineBuffer = new ArrayList<>();

        for (String line : template) {
            String trimmedLine = line.trim();
            boolean isEmpty = ColorFormatter.stripColors(line).replace('\u00A0', ' ').trim().isEmpty();

            if (isEmpty) {
                lineBuffer.add(line);
                continue;
            }

            if (trimmedLine.equals("(objective_details)") || trimmedLine.equals("{objective_details}")) {
                if (showActivationDetails) {
                    for (String buffered : lineBuffer) player.sendMessage(buffered);
                    lineBuffer.clear();
                    for (String detailLine : objectiveDetails) {
                        player.sendMessage(detailLine);
                    }
                } else {
                    lineBuffer.clear();
                }
                continue;
            }

            if (trimmedLine.equals("(clickable_details)") || trimmedLine.equals("{clickable_details}")) {
                if (showActivationDetails) {
                    for (String buffered : lineBuffer) player.sendMessage(buffered);
                    lineBuffer.clear();
                    sendClickableLink(player, quest);
                } else {
                    lineBuffer.clear();
                }
                continue;
            }

            for (String buffered : lineBuffer) player.sendMessage(buffered);
            lineBuffer.clear();

            if (line.contains("(objective_details)") || line.contains("{objective_details}")) {
                if (showActivationDetails) {
                    if (!objectiveDetails.isEmpty()) {
                        for (String detailLine : objectiveDetails) {
                            player.sendMessage(detailLine);
                        }
                    }
                }
                continue;
            }

            if (line.contains("(clickable_details)") || line.contains("{clickable_details}")) {
                if (showActivationDetails) {
                    sendClickableLink(player, quest);
                }
                continue;
            }

            player.sendMessage(line);
        }
    }

    public void sendNewStageMessage(Player player, QuestDefinition quest, int newStageNum, boolean showActivationDetails) {
        if (!showActivationDetails) {
            return;
        }

        List<String> template = lang.trList("quests.stage.new",
                Map.of("quest", quest.getDisplayName(), "stage", String.valueOf(newStageNum)));

        if (template.isEmpty()) {
            return;
        }

        List<String> objectiveDetails = getLimitedObjectiveDetails(player, quest);
        List<String> lineBuffer = new ArrayList<>();

        for (String line : template) {
            String trimmedLine = line.trim();
            boolean isEmpty = ColorFormatter.stripColors(line).replace('\u00A0', ' ').trim().isEmpty();

            if (isEmpty) {
                lineBuffer.add(line);
                continue;
            }

            if (trimmedLine.equals("(objective_details)") || trimmedLine.equals("{objective_details}")) {
                if (showActivationDetails) {
                    for (String buffered : lineBuffer) player.sendMessage(buffered);
                    lineBuffer.clear();
                    for (String detailLine : objectiveDetails) {
                        player.sendMessage(detailLine);
                    }
                } else {
                    lineBuffer.clear();
                }
                continue;
            }

            if (trimmedLine.equals("(clickable_details)") || trimmedLine.equals("{clickable_details}")) {
                if (showActivationDetails) {
                    for (String buffered : lineBuffer) player.sendMessage(buffered);
                    lineBuffer.clear();
                    sendClickableLink(player, quest);
                } else {
                    lineBuffer.clear();
                }
                continue;
            }

            for (String buffered : lineBuffer) player.sendMessage(buffered);
            lineBuffer.clear();

            if (line.contains("(objective_details)") || line.contains("{objective_details}")) {
                if (showActivationDetails) {
                    if (!objectiveDetails.isEmpty()) {
                        for (String detailLine : objectiveDetails) {
                            player.sendMessage(detailLine);
                        }
                    }
                }
                continue;
            }

            if (line.contains("(clickable_details)") || line.contains("{clickable_details}")) {
                if (showActivationDetails) {
                    sendClickableLink(player, quest);
                }
                continue;
            }

            player.sendMessage(line);
        }
    }

    private String convertHexForBungee(String text) {
        if (text == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hex);
            matcher.appendReplacement(result, color.toString());
        }
        matcher.appendTail(result);

        return ChatColor.translateAlternateColorCodes('&', result.toString());
    }

    private TextComponent createComponentWithColors(String text) {
        String bungeeText = convertHexForBungee(text);
        return new TextComponent(TextComponent.fromLegacyText(bungeeText));
    }

    private TextComponent createMultiLineComponent(String text) {
        TextComponent result = new TextComponent();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.addExtra(new TextComponent("\n"));
            }
            TextComponent lineComponent = createComponentWithColors(lines[i]);
            result.addExtra(lineComponent);
        }

        return result;
    }

    private void sendClickableLink(Player player, QuestDefinition quest) {
        String mode = plugin.getConfig().getString("integration.npc.mode", plugin.getConfig().getString("integration.mode", "MIXED"));
        if ("NPC_ONLY".equalsIgnoreCase(mode)) {
            return;
        }

        String command = "/quest details";

        String clickableTextRaw = lang.tr("chat.clickable");
        String hoverTextRaw = lang.tr("chat.hover");

        if (clickableTextRaw == null || clickableTextRaw.isEmpty() ||
                hoverTextRaw == null || hoverTextRaw.isEmpty()) {
            return;
        }

        TextComponent message;
        if (clickableTextRaw.contains("\n")) {
            message = createMultiLineComponent(clickableTextRaw);
        } else {
            message = createComponentWithColors(clickableTextRaw);
        }

        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

        TextComponent hoverComponent;
        if (hoverTextRaw.contains("\n")) {
            hoverComponent = createMultiLineComponent(hoverTextRaw);
        } else {
            hoverComponent = createComponentWithColors(hoverTextRaw);
        }

        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new BaseComponent[]{hoverComponent})));

        player.spigot().sendMessage(message);
    }

    private List<String> getLimitedObjectiveDetails(Player player, QuestDefinition quest) {
        List<String> details = new ArrayList<>();

        QuestObjective objective = questManager.resolveObjective(player, quest);

        if (objective != null) {
            List<String> objectiveLines = QuestDisplayBuilder.build(objective, lang);
            int limit = Math.min(objectiveLines.size(), 5);
            for (int i = 0; i < limit; i++) {
                details.add(objectiveLines.get(i));
            }
        } else {
            Map<QuestItem, Integer> materials = questManager.resolveRequiredMaterials(player, quest);
            if (materials != null && !materials.isEmpty()) {
                details.add(lang.tr("gui.quest_details.info.required-items"));
                int count = 0;
                for (Map.Entry<QuestItem, Integer> entry : materials.entrySet()) {
                    if (count >= 3) break;
                    QuestItem item = entry.getKey();
                    String materialName;

                    if (item.isVanilla()) {
                        materialName = ItemDisplayUtil.findLocalization(item.getMaterial(), lang, false);
                    } else {
                        materialName = ItemDisplayUtil.getItemsAdderDisplayName(item.getItemsAdderId(), lang);
                    }

                    details.add(lang.color(" &7• &f" + materialName + " &7x&f" + entry.getValue()));
                    count++;
                }
            }
        }

        return details;
    }
}
