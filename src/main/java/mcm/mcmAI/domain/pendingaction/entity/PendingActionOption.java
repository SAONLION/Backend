package mcm.mcmAI.domain.pendingaction.entity;

import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;

public record PendingActionOption(
        String key,
        String label,
        ActionNextStep actionNextStep,
        String staffCallReason
) {

    public PendingActionOption(String key, String label, ActionNextStep actionNextStep) {
        this(key, label, actionNextStep, null);
    }
}