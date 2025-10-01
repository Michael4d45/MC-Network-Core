package io.github.michael4d45;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Central registration point for all mod BlockEntityTypes. */
public final class ModBlockEntities {

  /** Network Core block entity type (lazy registered in {@link #registerAll()}). */
  public static BlockEntityType<NetworkCoreEntity> NETWORK_CORE;

  private ModBlockEntities() {}

  /** Register all block entity types (idempotent). */
  public static void registerAll() {
    if (NETWORK_CORE != null) {
      return; // already registered
    }
    NETWORK_CORE =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(NetworkCore.MOD_ID, "network_core"),
            FabricBlockEntityTypeBuilder.create(
                    NetworkCoreEntity::new, NetworkCore.NETWORK_CORE_BLOCK)
                .build());
    NetworkCore.LOGGER.debug("Registered block entity types");
  }
}
