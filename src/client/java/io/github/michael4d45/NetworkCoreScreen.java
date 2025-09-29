package io.github.michael4d45;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class NetworkCoreScreen extends HandledScreen<NetworkCoreScreenHandler> {
    private TextFieldWidget portField;
    private TextFieldWidget symbolPeriodField;

    public NetworkCoreScreen(NetworkCoreScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        portField = new TextFieldWidget(textRenderer, x + 10, y + 20, 100, 20, Text.of("Port"));
        portField.setText(String.valueOf(handler.getPropertyDelegate().get(0)));
        addSelectableChild(portField);

        symbolPeriodField = new TextFieldWidget(textRenderer, x + 10, y + 50, 100, 20, Text.of("Symbol Period"));
        symbolPeriodField.setText(String.valueOf(handler.getPropertyDelegate().get(1)));
        addSelectableChild(symbolPeriodField);

        addDrawableChild(ButtonWidget.builder(Text.of("Apply"), button -> apply()).position(x + 10, y + 80).size(50, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.of("Reset"), button -> reset()).position(x + 70, y + 80).size(50, 20).build());
    }

    private void apply() {
        // TODO: send packet
    }

    private void reset() {
        portField.setText("8080");
        symbolPeriodField.setText("20");
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // TODO: draw background
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        portField.render(context, mouseX, mouseY, delta);
        symbolPeriodField.render(context, mouseX, mouseY, delta);
    }
}