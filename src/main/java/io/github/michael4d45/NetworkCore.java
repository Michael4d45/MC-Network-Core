package io.github.michael4d45;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class NetworkCore implements ModInitializer {
  public static final String MOD_ID = "network-core";
  public static final Block NETWORK_CORE_BLOCK =
      registerBlock(
          "network_core",
          NetworkCoreBlock::new,
          AbstractBlock.Settings.create().strength(3.0f, 6.0f).requiresTool());

  // Block entity type registration
  public static BlockEntityType<NetworkCoreBlockEntity> NETWORK_CORE_BLOCK_ENTITY_TYPE;

  @Override
  public void onInitialize() {
    // Register the block item
    registerBlockItem("network_core", NETWORK_CORE_BLOCK);

    // Register block entity type (builder provided by Fabric API)
    NETWORK_CORE_BLOCK_ENTITY_TYPE =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MOD_ID, "network_core"),
            FabricBlockEntityTypeBuilder.create(NetworkCoreBlockEntity::new, NETWORK_CORE_BLOCK)
                .build());

    // Add to Redstone creative tab
    ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE)
        .register(
            entries -> {
              entries.add(NETWORK_CORE_BLOCK);
            });
  }

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
