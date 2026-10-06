package ua.woody.questborn.model;

import java.util.List;

public class DialogueNode {
    private final String id;
    private final List<String> text;
    private final List<DialogueOption> options;
    private final String sound;

    public DialogueNode(String id, List<String> text, List<DialogueOption> options, String sound) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.sound = sound;
    }

    public String getSound() { return sound; }

    public String getId() { return id; }
    public List<String> getText() { return text; }
    public List<DialogueOption> getOptions() { return options; }
}
