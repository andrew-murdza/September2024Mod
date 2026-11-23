package net.amurdza.examplemod.util;

/** One deliberate attack press gets at most one protection explanation, and harvesting consumes that press. */
public final class MiningPressFeedback {
    private boolean consumed;

    public void harvested() { consumed = true; }

    public boolean shouldExplainBlocked() {
        if (consumed) return false;
        consumed = true;
        return true;
    }

    public void release() { consumed = false; }
}
