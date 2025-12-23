package ua.woody.questborn.commands.sub;

import org.bukkit.command.CommandSender;

public interface SubCommand {

    /**
     * Ім'я сабкоманди (наприклад "help", "activate", "reset")
     */
    String getName();

    /**
     * Виконання сабкоманди.
     *
     * @param sender відправник
     * @param args   аргументи БЕЗ назви сабкоманди
     */
    boolean execute(CommandSender sender, String[] args);
}
