package io.github.michael4d45;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CoreRuntime {

  TxRuntime ingress = new TxRuntime();
  RxRuntime egress = new RxRuntime();
  Queue<Frame> rxQueue = new LinkedList<>(); // frames waiting for egress

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
    }
  }

  private void processControlFrame(ControlFrame controlFrame, NetworkCoreEntity be) {
    NetworkCore.LOGGER.info("Processing control frame: {}", controlFrame);
    switch (controlFrame.opcode) {
      case 0x0 -> // NOP
          // Idle/resync assist - no action needed
          NetworkCore.LOGGER.debug("NOP control frame");

      case 0x1 -> {
        // RESET
        // Flush TX/RX, clear errors
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

      case 0x2 -> // MODEQ - Request status frame
      // Generate and queue status frame for RX
      {
        int port = be.getPort();
        int worldId = be.getWorldId();
        int rxDepth = Math.min(rxQueue.size(), 13);
        int txDepth = 0; // No TX queue implemented yet
        int errorFlags = 0; // No error counters implemented yet
        StatusFrame statusFrame = new StatusFrame(worldId, port, rxDepth, txDepth, errorFlags);
        if (rxQueue.size() < 10) {
          rxQueue.add(statusFrame);
          NetworkCore.LOGGER.info("Queued status frame for RX");
        } else {
          NetworkCore.LOGGER.warn("RX queue full, cannot queue status frame");
        }
      }

      case 0x3 -> {
        // SETPER - Set symbol period
        int[] args = controlFrame.getArgs();
        if (args.length >= 1) {
          int period = args[0];
          if (period >= 1 && period <= 8) {
            // Update symbol period for this core
            be.setSymbolPeriodTicks(period);
            NetworkCore.LOGGER.info("SETPER control frame: set symbol period to {}", period);
          } else {
            NetworkCore.LOGGER.warn("Invalid symbol period {} in SETPER control frame", period);
          }
        }
      }

      case 0x4 -> {
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

      case 0x5 -> // STATSCLR - Clear counters
          NetworkCore.LOGGER.info("STATSCLR control frame processed");

      default -> NetworkCore.LOGGER.warn("Unknown control frame opcode {}", controlFrame.opcode);
    }
  }

  public void processRxOutput() {
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
  }

  public int getLastOutputPower() {
    return egress.lastOutputPower;
  }

  public void sendFrame(Frame frame) {
    if (frame == null) {
      return;
    }
    rxQueue.add(frame);
    NetworkCore.LOGGER.debug("Queued frame for egress, queue size now {}", rxQueue.size());
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
