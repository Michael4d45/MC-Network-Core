# Network Core

A Fabric Minecraft mod that implements a deterministic, NIC-inspired redstone-to-packet mediation layer for Minecraft 1.21.7.

## Description

Network Core provides a hardware-inspired network interface controller (NIC) that bridges redstone signals to a custom packet protocol. Each Network Core block acts as a virtual network card, allowing redstone contraptions to send and receive data frames across the Minecraft world.

The mod implements a fixed nibble-based wire protocol with frame delimiting, port-based virtual channels, and FIFO ring buffers for reliable data transmission.

## Features

- **Redstone Integration**: Transmit and receive data using redstone power levels (0-15)
- **Port-Based Routing**: Assign unique ports (1-255) to Network Core blocks for virtual channels
- **Deterministic Protocol**: Fixed wire format with SOF/EOF framing and error handling
- **World Persistence**: Automatic saving/loading of port assignments and block state
- **GUI Configuration**: In-game interface to configure port and symbol timing
- **Command Interface**: Administrative commands for testing and debugging
- **Multi-World Support**: Unique port spaces per Minecraft world/dimension

## Installation

### Requirements

- Minecraft 1.21.7
- Fabric Loader 0.17.2+
- Fabric API 0.129.0+

### Steps

1. Download the mod JAR from [releases](https://github.com/michael4d45/network-core/releases)
2. Place the JAR file in your `.minecraft/mods` folder
3. Launch Minecraft with Fabric

### Configuration

- **Port**: Set the network port (1-255) for this block
- **Symbol Period**: Adjust timing between symbols (1-8 ticks, default 2)

### Redstone Interface

- **Transmit**: Apply redstone power to the block's input face (opposite of facing direction)
- **Receive**: The block outputs redstone power on its facing direction based on received data

### Protocol Details

See [NETWORK_CORE_PROTOCOL.md](NETWORK_CORE_PROTOCOL.md) for complete protocol specification including frame formats, control commands, and state machines.

## Commands

All commands require operator permissions:

- `/networkcore sendtest <symbol>` - Send a test symbol (0-15) to the nearest Network Core
- `/networkcore pauseTickProcess` - Pause processing for the nearest Network Core (for testing)
- `/networkcore resumeTickProcess` - Resume processing for the nearest Network Core

## Development

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew runClient
```

### Code Quality

The project uses Spotless for code formatting:

```bash
./gradlew format      # Apply formatting
./gradlew formatCheck # Check formatting
```

### Project Structure

- `src/main/java/` - Main mod code
- `src/main/resources/` - Assets, data, and mod metadata
- `networkcore_test/` - Test datapack for protocol validation

### Dependencies

- Minecraft 1.21.7
- Fabric Loader 0.17.2
- Fabric API 0.129.0+1.21.7
- Java 21

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Run `./gradlew build` and `./gradlew test`
6. Submit a pull request

## License

This project is licensed under MIT - see [LICENSE](LICENSE) for details.

## Credits

Developed by Michael
