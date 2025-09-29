package io.github.michael4d45;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class NetworkCoreClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HandledScreens.register(NetworkCore.NETWORK_CORE_SCREEN_HANDLER, NetworkCoreScreen::new);
	}
}