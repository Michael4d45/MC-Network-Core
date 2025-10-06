package io.github.michael4d45;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class CoreRuntime {

    TxRuntime ingress = new TxRuntime();
    RxRuntime egress = new RxRuntime();
    Queue<Frame> rxQueue = new LinkedList<>(); // frames waiting for egress

    // Capacity for RX queue (frames pending emission). Status frames, routed frames, IPv4 frames
    // all share this. Drops increment overflow counter / bit.
    // When full, local senders receive BLOCK_BUSY (Data Control 0x3).
    // IPv4 senders receive TARGET_BUSY (IPv4 Control 0x7).
    private static final int RX_QUEUE_CAPACITY = 64;

    // Telemetry counters (server-thread only, no sync needed)
    long txFramesParsed = 0; // successfully parsed ingress frames (data/control/ipv4)
    long txFramingErrors = 0; // framing errors encountered by parser
    long rxFramesEmitted = 0; // frames fully emitted on egress
    long rxOverflowDrops = 0; // frames dropped because RX queue full

    // Cached last computed error flags bitfield:
    // bit0=RX_OVERFLOW, bit1=TX_FRAMING_ERR, bit2=PORT_ALLOC_FAILURE, bit3=IPV4_ROUTING_FAILURE
    private int errorFlagsBitfield = 0;

    public void processTxSymbol(NetworkCoreEntity be, int transmitPower) {
        TxFramerStateMachine.State prevState = ingress.state;
        TxFramerStateMachine.Result result
                = TxFramerStateMachine.process(
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
                case IPv4Frame ipv4Frame ->
                    IPv4Router.sendFrame(ipv4Frame);
                case DataFrame dataFrame ->
                    DataRouter.sendLocalDataFrame(be, dataFrame);
                case DataControlFrame controlFrame ->
                    processDataControlFrame(controlFrame, be);
                default ->
                    NetworkCore.LOGGER.debug(
                            "Unhandled frame type emitted from TX parser: {}",
                            result.committedFrame.getClass().getSimpleName());
            }
            txFramesParsed++;
        }
        if (result.errorIncremented) {
            txFramingErrors++;
            // Keep error flags up-to-date when framing errors occur so STATUS_REPLY reflects them
            recomputeErrorFlags();
        }
    }

    private void processDataControlFrame(DataControlFrame controlFrame, NetworkCoreEntity be) {
        NetworkCore.LOGGER.info("Processing data control frame: {}", controlFrame);
        int code = controlFrame.getCode();
        int[] args = controlFrame.getArgs();
        switch (code) {
            case 0x0 -> {
                if (args.length != 0) {
                    NetworkCore.LOGGER.warn("NOP control frame should have no args (got {})", args.length);
                }
                break;
            }
            case 0x1 -> {
                if (args.length == 4) {
                    int port = (args[0] << 12) | (args[1] << 8) | (args[2] << 4) | args[3];
                    NetworkCore.LOGGER.warn("Port {} unreachable", port);
                } else {
                    NetworkCore.LOGGER.warn("PORT_UNREACHABLE frame requires 4 arg nibbles");
                }
                break;
            }
            case 0x2 -> {
                NetworkCore.LOGGER.warn(
                        "MALFORMED_FRAME notification received (args={}). Dropped.", Arrays.toString(args));
                break;
            }
            case 0x3 -> {
                if (args.length == 4) {
                    int port = (args[0] << 12) | (args[1] << 8) | (args[2] << 4) | args[3];
                    NetworkCore.LOGGER.info("Target port {} busy", port);
                } else {
                    NetworkCore.LOGGER.warn("BLOCK_BUSY frame requires 4 arg nibbles");
                }
                break;
            }
            case 0x4 -> {
                DataControlFrame reply = new DataControlFrame(0x5, args);
                sendFrame(reply);
                break;
            }
            case 0x5 -> {
                NetworkCore.LOGGER.info("Received ECHO_REPLY payload={} ", Arrays.toString(args));
                break;
            }
            case 0x6 -> {
                if (args.length != 0) {
                    NetworkCore.LOGGER.warn("MODEQ control frame should have no args");
                }
                int port = be.getPort();
                queueStatusReply(port);
                break;
            }
            case 0x7 -> {
                if (args.length != 0) {
                    NetworkCore.LOGGER.warn("RESET control frame should have no args");
                }
                performReset();
                break;
            }
            case 0x8 -> {
                if (args.length != 4) {
                    NetworkCore.LOGGER.warn(
                            "SETPORT control frame requires 4 arg nibbles (got {})", args.length);
                    break;
                }
                int requestedPort = (args[0] << 12) | (args[1] << 8) | (args[2] << 4) | (args[3] & 0xF);
                if (requestedPort < 0 || requestedPort > 65535) {
                    NetworkCore.LOGGER.warn("SETPORT request out of range: {}", requestedPort);
                    break;
                }
                NetworkCore.LOGGER.info("SETPORT request to {}", requestedPort);
                be.setPort(requestedPort);
                queueStatusReply(be.getPort());
                break;
            }
            case 0xA -> {
                if (args.length == 8) {
                    byte[] ip = new byte[4];
                    ip[0] = (byte) ((args[0] << 4) | args[1]);
                    ip[1] = (byte) ((args[2] << 4) | args[3]);
                    ip[2] = (byte) ((args[4] << 4) | args[5]);
                    ip[3] = (byte) ((args[6] << 4) | args[7]);
                    NetworkCore.LOGGER.warn("Host unreachable: {}", Arrays.toString(ip));
                } else {
                    NetworkCore.LOGGER.warn(
                            "HOST_UNREACHABLE frame requires 8 arg nibbles (got {})", args.length);
                }
                break;
            }
            case 0xB -> {
                NetworkCore.LOGGER.warn("Network error received (args={})", Arrays.toString(args));
                break;
            }
            case 0xC -> {
                if (args.length == 4) {
                    int port = (args[0] << 12) | (args[1] << 8) | (args[2] << 4) | args[3];
                    NetworkCore.LOGGER.info("Remote target port {} busy", port);
                } else {
                    NetworkCore.LOGGER.warn("TARGET_BUSY frame requires 4 arg nibbles (got {})", args.length);
                }
                break;
            }
            default ->
                NetworkCore.LOGGER.warn("Unknown data control code {}", code);
        }
    }

    private void performReset() {
        ingress.state = TxFramerStateMachine.State.IDLE;
        ingress.buffer.clear();
        ingress.expectedLength = 0;
        egress.state = RxEmitterStateMachine.State.IDLE;
        egress.currentFrame = null;
        egress.symbols = null;
        egress.position = 0;
        egress.lastOutputPower = 0;
        rxQueue.clear();
        recomputeErrorFlags();
        NetworkCore.LOGGER.info("Core runtime reset");
    }

    public void processRxOutput() {
        RxEmitterStateMachine.State previousState = egress.state;
        Frame previousFrame = egress.currentFrame;
        RxEmitterStateMachine.Result result
                = RxEmitterStateMachine.process(
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

    public boolean sendFrame(Frame frame) {
        if (frame == null) {
            return true; // null frames are ignored, not "failed"
        }
        // Validate frame can be built before queuing - all frames limited to 255 total args
        try {
            int[] args = frame.getPayloadArgs();
            if (args.length > 255) {
                NetworkCore.LOGGER.error(
                        "Cannot queue frame with args length {} (max 255): {}", args.length, frame);
                rxOverflowDrops++;
                recomputeErrorFlags();
                return false;
            }
        } catch (IllegalStateException e) {
            NetworkCore.LOGGER.error("Cannot queue invalid frame: {}", frame, e);
            rxOverflowDrops++;
            recomputeErrorFlags();
            return false;
        }
        if (rxQueue.size() >= RX_QUEUE_CAPACITY) {
            rxOverflowDrops++;
            recomputeErrorFlags();
            NetworkCore.LOGGER.warn("RX queue full ({}), dropping frame {}", RX_QUEUE_CAPACITY, frame);
            return false;
        }
        rxQueue.add(frame);
        NetworkCore.LOGGER.debug("Queued frame for egress, queue size now {}", rxQueue.size());
        return true;
    }

    private void queueStatusReply(int port) {
        int rxDepth = Math.min(rxQueue.size(), RX_QUEUE_CAPACITY);
        recomputeErrorFlags();
        // Build STATUS_REPLY payload: signature + 4 port nibbles + 2 rxDepth nibbles + errorFlags + 8
        // IP nibbles + 4 UDP port nibbles
        int[] payload = new int[20];
        payload[0] = 0xA; // signature
        payload[1] = (port >> 12) & 0xF;
        payload[2] = (port >> 8) & 0xF;
        payload[3] = (port >> 4) & 0xF;
        payload[4] = port & 0xF;
        payload[5] = (rxDepth >> 4) & 0xF; // rxDepth high nibble
        payload[6] = rxDepth & 0xF; // rxDepth low nibble
        payload[7] = errorFlagsBitfield & 0xF;
        byte[] ip = IPv4Router.getLocalIp();
        // ip bytes may be signed; mask to 0..255 before extracting nibbles
        int b0 = ip[0] & 0xFF;
        int b1 = ip[1] & 0xFF;
        int b2 = ip[2] & 0xFF;
        int b3 = ip[3] & 0xFF;
        payload[8] = (b0 >> 4) & 0xF;
        payload[9] = b0 & 0xF;
        payload[10] = (b1 >> 4) & 0xF;
        payload[11] = b1 & 0xF;
        payload[12] = (b2 >> 4) & 0xF;
        payload[13] = b2 & 0xF;
        payload[14] = (b3 >> 4) & 0xF;
        payload[15] = b3 & 0xF;
        int udpPort = IPv4Router.getUdpPort();
        payload[16] = (udpPort >> 12) & 0xF;
        payload[17] = (udpPort >> 8) & 0xF;
        payload[18] = (udpPort >> 4) & 0xF;
        payload[19] = udpPort & 0xF;
        DataControlFrame statusReply = new DataControlFrame(0x9, payload); // CODE 0x9 = STATUS_REPLY
        if (rxQueue.size() >= RX_QUEUE_CAPACITY) {
            rxOverflowDrops++;
            recomputeErrorFlags();
            NetworkCore.LOGGER.warn("RX queue full, cannot queue status reply");
            return;
        }
        rxQueue.add(statusReply);
        NetworkCore.LOGGER.info("Queued STATUS_REPLY for RX (port={})", port);
    }

    private void recomputeErrorFlags() {
        int flags = 0;
        if (rxOverflowDrops > 0) {
            flags |= 0x1; // bit0 RX_OVERFLOW
        }
        if (txFramingErrors > 0) {
            flags |= 0x2; // bit1 TX_FRAMING_ERR
        }
        // bit2 PORT_ALLOC_FAILURE - reserved for future use
        // bit3 IPV4_ROUTING_FAILURE - reserved for future use
        this.errorFlagsBitfield = flags;
    }

    // Accessors for stats command
    public long getTxFramesParsed() {
        return txFramesParsed;
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

    public void processRemoteDataControlFrame(
            DataControlFrame frame,
            byte[] srcIp,
            int srcUdpPort,
            byte[] dstIp,
            int dstUdpPort,
            int port) {
        NetworkCore.LOGGER.info("Processing remote data control frame: {}", frame);
        int code = frame.getCode();
        int[] args = frame.getArgs();
        switch (code) {
            case 0x4 -> { // ECHO_REQUEST
                DataControlFrame reply = new DataControlFrame(0x5, args);
                IPv4Frame response = new IPv4Frame(srcIp, srcUdpPort, dstIp, dstUdpPort, reply);
                IPv4Router.sendFrame(response);
            }
            case 0x6 -> { // MODEQ
                DataControlFrame reply = createStatusReply(port);
                IPv4Frame response = new IPv4Frame(srcIp, srcUdpPort, dstIp, dstUdpPort, reply);
                IPv4Router.sendFrame(response);
            }
            default ->
                NetworkCore.LOGGER.debug("Ignoring remote DataControl code {}", code);
        }
    }

    private DataControlFrame createStatusReply(int port) {
        int rxDepth = Math.min(rxQueue.size(), RX_QUEUE_CAPACITY);
        recomputeErrorFlags();
        // Build STATUS_REPLY payload: signature + 4 port nibbles + 2 rxDepth nibbles + errorFlags + 8
        // IP nibbles + 4 UDP port nibbles
        int[] payload = new int[20];
        payload[0] = 0xA; // signature
        payload[1] = (port >> 12) & 0xF;
        payload[2] = (port >> 8) & 0xF;
        payload[3] = (port >> 4) & 0xF;
        payload[4] = port & 0xF;
        payload[5] = (rxDepth >> 4) & 0xF; // rxDepth high nibble
        payload[6] = rxDepth & 0xF; // rxDepth low nibble
        payload[7] = errorFlagsBitfield & 0xF;
        byte[] ip = IPv4Router.getLocalIp();
        int b0c = ip[0] & 0xFF;
        int b1c = ip[1] & 0xFF;
        int b2c = ip[2] & 0xFF;
        int b3c = ip[3] & 0xFF;
        payload[8] = (b0c >> 4) & 0xF;
        payload[9] = b0c & 0xF;
        payload[10] = (b1c >> 4) & 0xF;
        payload[11] = b1c & 0xF;
        payload[12] = (b2c >> 4) & 0xF;
        payload[13] = b2c & 0xF;
        payload[14] = (b3c >> 4) & 0xF;
        payload[15] = b3c & 0xF;
        int udpPort = IPv4Router.getUdpPort();
        payload[16] = (udpPort >> 12) & 0xF;
        payload[17] = (udpPort >> 8) & 0xF;
        payload[18] = (udpPort >> 4) & 0xF;
        payload[19] = udpPort & 0xF;
        return new DataControlFrame(0x9, payload); // CODE 0x9 = STATUS_REPLY
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
