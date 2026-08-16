package mcm.mcmAI.domain.pendingaction.entity;

import mcm.mcmAI.domain.pendingaction.type.ActionNextStep;

public record PendingActionOption(
        String key,
        String label,
        ActionNextStep actionNextStep
) {
}