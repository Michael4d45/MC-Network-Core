package io.github.michael4d45;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CoreRuntime {

  TxRuntime ingress = new TxRuntime();
  RxRuntime egress = new RxRuntime();
  Queue<Frame> rxQueue = new LinkedList<>(); // frames waiting for egress

  // Capacity for RX queue (frames pending emission). Status frames, routed frames, IPv4 frames
  // all share this. Drops increment overflow counter / bit.
  private static final int RX_QUEUE_CAPACITY = 64;

  // Telemetry counters (server-thread only, no sync needed)
  long txFramesParsed = 0; // successfully parsed ingress frames (data/control/ipv4)
  long txFramesDropped = 0; // would be used if we had a bounded TX ring (currently unused)
  long txFramingErrors = 0; // framing errors encountered by parser
  long rxFramesEmitted = 0; // frames fully emitted on egress
  long rxOverflowDrops = 0; // frames dropped because RX queue full

  // Cached last computed error flags bitfield (bit0 RX_OVERFLOW, bit1 TX_FRAMING_ERR)
  private int errorFlagsBitfield = 0;

  public void processTxSymbol(NetworkCoreEntity be, int transmitPower) {
    TxFramerStateMachine.State prevState = ingress.state;
    TxFramerStateMachine.Result result =
        TxFramerStateMachine.process(
            prevState, ingress.buffer, ingress.expectedLength, transmitPower);
    ingress.state = result.state;
    ingress.buffer = result.buffer; // buffer is a new list copy from process
    ingress.expectedLength = result.expectedLength;
    if (transmitPower != 0) {
      NetworkCore.LOGGER.debug(
          "INGRESS transmitPower={} bufSize={}", transmitPower, ingress.buffer.size());
    }
    if (ingress.state != prevState) {
      NetworkCore.LOGGER.debug(
          "INGRESS prevState={} newState={} bufSize={} expected={} committed={}",
          prevState,
          result.state,
          ingress.buffer.size(),
          ingress.expectedLength,
          result.committedFrame != null);
    }
    if (result.committedFrame != null) {
      NetworkCore.LOGGER.info("Committed frame: {}", result.committedFrame);
      // framing error count removed for now; could add metrics collection later
      switch (result.committedFrame) {
        case IPv4Frame ipv4Frame -> IPv4Router.sendFrame(ipv4Frame);
        case RoutedFrame dataFrame -> DataRouter.sendFrame(dataFrame);
        case ControlFrame controlFrame -> processControlFrame(controlFrame, be);
        default -> {}
      }
      txFramesParsed++;
    }
    if (result.errorIncremented) {
      txFramingErrors++;
    }
  }

  private void processControlFrame(ControlFrame controlFrame, NetworkCoreEntity be) {
    NetworkCore.LOGGER.info("Processing control frame: {}", controlFrame);
    switch (controlFrame.opcode) {
      case 0x0 -> // NOP
      // Idle/resync assist - no action needed
      {
        if (controlFrame.getArgs().length != 0) {
          NetworkCore.LOGGER.warn(
              "Invalid NOP control frame: expected 0 args, got {}", controlFrame.getArgs().length);
        } else {
          NetworkCore.LOGGER.debug("NOP control frame");
        }
      }

      case 0x1 -> {
        // RESET
        // Flush TX/RX, clear errors
        if (controlFrame.getArgs().length != 0) {
          NetworkCore.LOGGER.warn(
              "Invalid RESET control frame: expected 0 args, got {}",
              controlFrame.getArgs().length);
        } else {
          ingress.state = TxFramerStateMachine.State.IDLE;
          ingress.buffer.clear();
          ingress.expectedLength = 0;
          egress.state = RxEmitterStateMachine.State.IDLE;
          egress.currentFrame = null;
          egress.symbols = null;
          egress.position = 0;
          egress.lastOutputPower = 0;
          rxQueue.clear();
          NetworkCore.LOGGER.info("RESET control frame processed");
        }
      }

      case 0x2 -> // MODEQ - Request status frame
      // Generate and queue status frame for RX
      {
        if (controlFrame.getArgs().length != 0) {
          NetworkCore.LOGGER.warn(
              "Invalid MODEQ control frame: expected 0 args, got {}",
              controlFrame.getArgs().length);
        } else {
          int port = be.getPort();
          int worldId = be.getWorldId();
          queueStatusFrame(worldId, port);
        }
      }

      case 0x3 -> {
        // SETPORT - Set port
        int[] args = controlFrame.getArgs();
        if (args.length >= 1 && args.length <= 4) {
          int port = 0;
          // Build port from nibbles (high nibbles first)
          for (int i = 0; i < args.length; i++) {
            port = (port << 4) | args[i];
          }
          if (port >= 0 && port <= 65535) {
            be.setPort(port);
          } else {
            NetworkCore.LOGGER.warn("SETPORT control frame: port {} out of range 0-65535", port);
          }
        } else {
          NetworkCore.LOGGER.warn(
              "Invalid SETPORT control frame, arg count {} not in range 1-4", args.length);
        }
      }

      case 0x4 -> // STATSCLR - Clear counters
      {
        if (controlFrame.getArgs().length != 0) {
          NetworkCore.LOGGER.warn(
              "Invalid STATSCLR control frame: expected 0 args, got {}",
              controlFrame.getArgs().length);
        } else {
          txFramesParsed = 0;
          txFramesDropped = 0;
          txFramingErrors = 0;
          rxFramesEmitted = 0;
          rxOverflowDrops = 0;
          recomputeErrorFlags();
          NetworkCore.LOGGER.info("STATSCLR control frame processed");
        }
      }

      default -> NetworkCore.LOGGER.warn("Unknown control frame opcode {}", controlFrame.opcode);
    }
  }

  public void processRxOutput() {
    RxEmitterStateMachine.State previousState = egress.state;
    Frame previousFrame = egress.currentFrame;
    RxEmitterStateMachine.Result result =
        RxEmitterStateMachine.process(
            egress.state, egress.currentFrame, egress.symbols, egress.position, rxQueue);
    if (egress.state != result.newState || egress.state == RxEmitterStateMachine.State.OUTPUTTING) {
      NetworkCore.LOGGER.debug(
          "currentFrame={} symbols={} position={}",
          egress.currentFrame,
          egress.symbols,
          egress.position);

      NetworkCore.LOGGER.debug(
          "EGRESS prevState={} newState={} frameNull={} symbolsNull={} posIdx={} outSym={}",
          egress.state,
          result.newState,
          result.currentFrame == null,
          result.symbols == null,
          result.position,
          result.outputSymbol);
    }
    egress.state = result.newState;
    egress.currentFrame = result.currentFrame;
    egress.symbols = result.symbols;
    egress.position = result.position;
    Integer outputSymbol = result.outputSymbol;
    if (outputSymbol != null) {
      egress.lastOutputPower = outputSymbol;
    } else {
      egress.lastOutputPower = 0;
    }
    // Count frame emission when we transition from OUTPUTTING to IDLE having previously had a
    // frame.
    if (previousState == RxEmitterStateMachine.State.OUTPUTTING
        && result.newState == RxEmitterStateMachine.State.IDLE
        && previousFrame != null) {
      rxFramesEmitted++;
    }
  }

  public int getLastOutputPower() {
    return egress.lastOutputPower;
  }

  public void sendFrame(Frame frame) {
    if (frame == null) {
      return;
    }
    if (rxQueue.size() >= RX_QUEUE_CAPACITY) {
      rxOverflowDrops++;
      recomputeErrorFlags();
      NetworkCore.LOGGER.warn("RX queue full ({}), dropping frame {}", RX_QUEUE_CAPACITY, frame);
      return;
    }
    rxQueue.add(frame);
    NetworkCore.LOGGER.debug("Queued frame for egress, queue size now {}", rxQueue.size());
  }

  private void queueStatusFrame(int worldId, int port) {
    int rxDepth = Math.min(rxQueue.size(), 13);
    int txDepth = 0; // No TX queue yet
    recomputeErrorFlags();
    StatusFrame statusFrame =
        new StatusFrame(worldId, port, rxDepth, txDepth, errorFlagsBitfield & 0xF);
    if (rxQueue.size() >= RX_QUEUE_CAPACITY) {
      rxOverflowDrops++;
      recomputeErrorFlags();
      NetworkCore.LOGGER.warn("RX queue full, cannot queue status frame");
      return;
    }
    rxQueue.add(statusFrame);
    NetworkCore.LOGGER.info("Queued status frame for RX (port={}, world={})", port, worldId);
  }

  private void recomputeErrorFlags() {
    int flags = 0;
    if (rxOverflowDrops > 0) {
      flags |= 0x1; // bit0 RX_OVERFLOW
    }
    if (txFramingErrors > 0) {
      flags |= 0x2; // bit1 TX_FRAMING_ERR
    }
    this.errorFlagsBitfield = flags;
  }

  // Accessors for stats command
  public long getTxFramesParsed() {
    return txFramesParsed;
  }

  public long getTxFramesDropped() {
    return txFramesDropped;
  }

  public long getTxFramingErrors() {
    return txFramingErrors;
  }

  public long getRxFramesEmitted() {
    return rxFramesEmitted;
  }

  public long getRxOverflowDrops() {
    return rxOverflowDrops;
  }

  public int getRxQueueDepth() {
    return rxQueue.size();
  }

  public int getErrorFlagsBitfield() {
    return errorFlagsBitfield;
  }

  private static final class TxRuntime {
    TxFramerStateMachine.State state = TxFramerStateMachine.State.IDLE;
    List<Integer> buffer = new ArrayList<>();
    int expectedLength = 0;
  }

  private static final class RxRuntime {
    RxEmitterStateMachine.State state = RxEmitterStateMachine.State.IDLE;
    Frame currentFrame;
    int[] symbols;
    int position = 0;
    int lastOutputPower = 0;
  }
}
