package io.github.michael4d45;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkCore implements ModInitializer {

  public static final String MOD_ID = "network-core";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static final Block NETWORK_CORE_BLOCK =
      registerBlock(
          "network_core",
          NetworkCoreBlock::new,
          AbstractBlock.Settings.create()
              .strength(3.0f, 6.0f)
              .requiresTool()
              .pistonBehavior(PistonBehavior.NORMAL));

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing NetworkCore mod");
    DataRouter.init();
    IPv4Router.init();
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
    // Register commands
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> {
          registerCommands(dispatcher);
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

  private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
    var root =
        CommandManager.literal("networkcore")
            .then(
                CommandManager.literal("sendtest")
                    .then(
                        CommandManager.argument("symbol", IntegerArgumentType.integer(0, 15))
                            .executes(
                                context -> {
                                  ServerCommandSource source = context.getSource();
                                  ServerPlayerEntity player = source.getPlayer();
                                  if (player == null) {
                                    source.sendError(
                                        Text.literal("This command can only be run by a player"));
                                    return 0;
                                  }
                                  NetworkCoreEntity nearest = findNearestNetworkCore(player);
                                  if (nearest == null) {
                                    source.sendError(
                                        Text.literal(
                                            "No NetworkCore block found within 16 blocks"));
                                    return 0;
                                  }
                                  int symbol = IntegerArgumentType.getInteger(context, "symbol");
                                  nearest.getRuntime().processTxSymbol(nearest, symbol);
                                  source.sendMessage(
                                      Text.literal(
                                          "Sent symbol "
                                              + symbol
                                              + " to NetworkCore at "
                                              + nearest.getPos()));
                                  return 1;
                                })))
            .then(
                CommandManager.literal("udpaddress")
                    .executes(
                        context -> {
                          ServerCommandSource source = context.getSource();
                          source.sendMessage(
                              Text.literal(
                                  "UDP address for sending packets: "
                                      + IPv4Router.getUdpAddress()));
                          return 1;
                        }))
            .then(
                CommandManager.literal("listports")
                    .executes(
                        context -> {
                          ServerCommandSource source = context.getSource();
                          source.sendMessage(Text.literal("Listing all allocated ports:"));
                          for (var world : source.getServer().getWorlds()) {
                            var ports = DataRouter.getAllocatedPorts(world);
                            if (!ports.isEmpty()) {
                              source.sendMessage(
                                  Text.literal("World: " + world.getRegistryKey().getValue()));
                              for (var entry : ports.entrySet()) {
                                BlockPos pos = entry.getKey();
                                int port = entry.getValue();
                                source.sendMessage(Text.literal("  Port " + port + " at " + pos));
                              }
                            }
                          }
                          source.sendMessage(Text.literal("End of port list."));
                          return 1;
                        }))
            .then(
                CommandManager.literal("stats")
                    .executes(
                        context -> {
                          ServerCommandSource source = context.getSource();
                          ServerPlayerEntity player = source.getPlayer();
                          if (player == null) {
                            source.sendError(
                                Text.literal("This command can only be run by a player"));
                            return 0;
                          }
                          NetworkCoreEntity nearest = findNearestNetworkCore(player);
                          if (nearest == null) {
                            source.sendError(
                                Text.literal("No NetworkCore block found within 16 blocks"));
                            return 0;
                          }
                          CoreRuntime rt = nearest.getRuntime();
                          String msg =
                              String.format(
                                  "Stats @ %s:\n txFramesParsed=%d txFramesDropped=%d txFramingErrors=%d\n rxFramesEmitted=%d rxOverflowDrops=%d rxQueueDepth=%d errorFlags=0x%X",
                                  nearest.getPos(),
                                  rt.getTxFramesParsed(),
                                  rt.getTxFramesDropped(),
                                  rt.getTxFramingErrors(),
                                  rt.getRxFramesEmitted(),
                                  rt.getRxOverflowDrops(),
                                  rt.getRxQueueDepth(),
                                  rt.getErrorFlagsBitfield());
                          source.sendMessage(Text.literal(msg));
                          return 1;
                        }))
            .then(
                CommandManager.literal("help")
                    .executes(
                        ctx -> {
                          ServerCommandSource src = ctx.getSource();
                          src.sendMessage(
                              Text.literal(
                                  """
                                           NetworkCore commands:
                                           /networkcore sendtest <0-15> - send a symbol to nearest core
                                           /networkcore udpaddress - show UDP address for packets
                                           /networkcore listports - list allocated ports
                                           /networkcore stats - show counters for nearest core
                                           /networkcore help - show this help"""));
                          return 1;
                        }));
    // Register the root and a short alias /nc redirecting to it.
    dispatcher.register(root);
    dispatcher.register(
        CommandManager.literal("nc").redirect(dispatcher.getRoot().getChild("networkcore")));
  }

  private static NetworkCoreEntity findNearestNetworkCore(ServerPlayerEntity player) {
    BlockPos playerPos = player.getBlockPos();
    NetworkCoreEntity nearest = null;
    double minDist = Double.MAX_VALUE;
    for (int x = -16; x <= 16; x++) {
      for (int y = -16; y <= 16; y++) {
        for (int z = -16; z <= 16; z++) {
          BlockPos pos = playerPos.add(x, y, z);
          if (player.getWorld().getBlockEntity(pos) instanceof NetworkCoreEntity be) {
            double dist = playerPos.getSquaredDistance(pos);
            if (dist < minDist) {
              minDist = dist;
              nearest = be;
            }
          }
        }
      }
    }
    return nearest;
  }
}
