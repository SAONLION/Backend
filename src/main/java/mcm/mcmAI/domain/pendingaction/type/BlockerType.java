package mcm.mcmAI.domain.pendingaction.type;

public enum BlockerType {

    CB1,
    CB3,
    CB5,
    CB6;

    /** 프론트 계약(F23-1)용 표시 타입. 분석은 name()(CB5/CB6)을 그대로 쓴다. */
    public String toDisplayType() {
        return switch (this) {
            case CB5, CB6 -> "CONTENT_OFFER";
            default -> this.name();
        };
    }
}