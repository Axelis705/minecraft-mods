package com.scardax.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Barra de 0 a 300 % (de 5 en 5). Avisa al soltar. */
final class PercentSlider extends AbstractSliderButton {
    private final MobCatalog.Entry entry;
    private final Consumer<Integer> onDone;

    PercentSlider(int x, int y, int w, int h, MobCatalog.Entry entry, Consumer<Integer> onDone) {
        super(x, y, w, h, Component.empty(), ClientRules.get(entry.id) / 300.0);
        this.entry = entry;
        this.onDone = onDone;
        updateMessage();
    }

    private int pct() {
        return (int) Math.round(this.value * 60.0) * 5;
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable("scardax.row.spawn", pct()));
    }

    @Override
    protected void applyValue() {
        this.value = pct() / 300.0;
    }

    @Override
    public void onRelease(double x, double y) {
        super.onRelease(x, y);
        onDone.accept(pct());
    }
}
