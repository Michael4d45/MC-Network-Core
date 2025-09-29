package io.github.michael4d45;

import net.fabricmc.api.ModInitializer;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkCore implements ModInitializer {
	public static final String MOD_ID = "network-core";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Block NETWORK_CORE_BLOCK = new NetworkCoreBlock(net.minecraft.block.AbstractBlock.Settings.create().strength(4.0f));
	public static final BlockItem NETWORK_CORE_BLOCK_ITEM = new BlockItem(NETWORK_CORE_BLOCK, new Item.Settings());
	public static final BlockEntityType<NetworkCoreBlockEntity> NETWORK_CORE_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(NetworkCoreBlockEntity::new, NETWORK_CORE_BLOCK).build();
	public static final ScreenHandlerType<NetworkCoreScreenHandler> NETWORK_CORE_SCREEN_HANDLER = new ScreenHandlerType<>((syncId, inventory) -> new NetworkCoreScreenHandler(syncId, inventory), FeatureSet.empty());

	@Override
	public void onInitialize() {
		Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "network_core"), NETWORK_CORE_BLOCK);
		Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "network_core"), NETWORK_CORE_BLOCK_ITEM);
		Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "network_core"), NETWORK_CORE_BLOCK_ENTITY);
		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "network_core"), NETWORK_CORE_SCREEN_HANDLER);

		LOGGER.info("Hello Fabric world!");
	}
}