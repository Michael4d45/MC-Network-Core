package io.github.michael4d45;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkCore implements ModInitializer {

  public static final String MOD_ID = "network-core";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static final Block NETWORK_CORE_BLOCK =
      registerBlock(
          "network_core",
          NetworkCoreBlock::new,
          AbstractBlock.Settings.create().strength(3.0f, 6.0f).requiresTool());

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing NetworkCore mod");
    Router.init();
    // Block entity types
    ModBlockEntities.registerAll();
    // Register the block item
    registerBlockItem("network_core", NETWORK_CORE_BLOCK);
    // Add to Redstone creative tab
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
        .register(
            entries -> {
              entries.add(NETWORK_CORE_BLOCK);
            });
  }

  // (Block entity registration now lives in ModBlockEntities again.)
  private static Block registerBlock(
      String name,
      java.util.function.Function<AbstractBlock.Settings, Block> blockFactory,
      AbstractBlock.Settings settings) {
    RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, name));
    Block block = blockFactory.apply(settings.registryKey(blockKey));
    return Registry.register(Registries.BLOCK, blockKey, block);
  }

  private static void registerBlockItem(String name, Block block) {
    RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
    BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
    Registry.register(Registries.ITEM, itemKey, blockItem);
  }
}
