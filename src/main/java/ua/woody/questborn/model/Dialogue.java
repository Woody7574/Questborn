package ua.woody.questborn.model;

import java.util.Map;

public class Dialogue {
    private final String id;
    private final Map<String, DialogueNode> nodes;
    private final int cpt;
    private final long updatePeriod;

    public Dialogue(String id, Map<String, DialogueNode> nodes, int cpt, long updatePeriod) {
        this.id = id;
        this.nodes = nodes;
        this.cpt = cpt;
        this.updatePeriod = updatePeriod;
    }

    public String getId() { return id; }
    public Map<String, DialogueNode> getNodes() { return nodes; }
    public DialogueNode getNode(String nodeId) { return nodes.get(nodeId); }
    public int getCpt() { return cpt; }
    public long getUpdatePeriod() { return updatePeriod; }
}
