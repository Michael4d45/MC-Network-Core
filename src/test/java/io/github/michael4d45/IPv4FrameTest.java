package io.github.michael4d45;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class IPv4FrameTest {

  @Test
  void testBuildIPv4() {
    // Test building ToIPv4Frame
    ToIPv4Frame frame =
        new ToIPv4Frame(0, 0, new byte[] {(byte) 192, (byte) 168, 1, 10}, 0, new int[] {1, 2, 3});
    int[] symbols = frame.buildSymbols();
    // Expected: SOF=15, TYPE=3, srcWorld=0,0, srcPort=0,0,0,0, dstIp=12,0,10,8,0,1,0,10,
    // dstPort=0,0,0,0,
    // len=0,3,
    // payload=1,2,3, EOF=0
    int[] expected = {
      15, 3, 0, 0, 0, 0, 0, 0, 12, 0, 10, 8, 0, 1, 0, 10, 0, 0, 0, 0, 0, 3, 1, 2, 3, 0
    };
    assertArrayEquals(expected, symbols);
  }

  @Test
  void testParseToIPv4() {
    // Test parsing ToIPv4Frame
    TxFramerStateMachine.Result result =
        TxFramerStateMachine.process(
            TxFramerStateMachine.State.IDLE, java.util.List.of(), 0, 15); // SOF
    result =
        TxFramerStateMachine.process(
            result.state, result.buffer, result.expectedLength, 3); // TYPE=3
    // Add header: srcWorld=0,0 srcPort=0,0,0,0 dstIp=12,0,10,8,0,1,0,10 dstPort=0,0,0,0 len=0,3
    int[] header = {0, 0, 0, 0, 0, 0, 12, 0, 10, 8, 0, 1, 0, 10, 0, 0, 0, 0, 0, 3};
    for (int h : header) {
      result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, h);
    }
    // Add payload: 1,2,3
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 1);
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 2);
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 3);
    // EOF
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 0);
    assertNotNull(result.committedFrame);
    assertInstanceOf(ToIPv4Frame.class, result.committedFrame);
    ToIPv4Frame frame = (ToIPv4Frame) result.committedFrame;
    assertEquals(0, frame.srcWorld);
    assertEquals(0, frame.srcPort);
    assertEquals(0, frame.dstPort);
    assertArrayEquals(new byte[] {(byte) 192, (byte) 168, 1, 10}, frame.getDstIp());
    assertArrayEquals(new int[] {1, 2, 3}, frame.getPayload());
  }

  @Test
  void testParseFromIPv4() {
    // Test parsing FromIPv4Frame
    TxFramerStateMachine.Result result =
        TxFramerStateMachine.process(
            TxFramerStateMachine.State.IDLE, java.util.List.of(), 0, 15); // SOF
    result =
        TxFramerStateMachine.process(
            result.state, result.buffer, result.expectedLength, 4); // TYPE=4
    // Add header: dstWorld=0,0 dstPort=0,0,0,0 srcIp=12,0,10,8,0,1,0,10 srcPort=0,0,0,0 len=0,3
    int[] header = {0, 0, 0, 0, 0, 0, 12, 0, 10, 8, 0, 1, 0, 10, 0, 0, 0, 0, 0, 3};
    for (int h : header) {
      result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, h);
    }
    // Add payload: 1,2,3
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 1);
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 2);
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 3);
    // EOF
    result = TxFramerStateMachine.process(result.state, result.buffer, result.expectedLength, 0);
    assertNotNull(result.committedFrame);
    assertInstanceOf(FromIPv4Frame.class, result.committedFrame);
    FromIPv4Frame frame = (FromIPv4Frame) result.committedFrame;
    assertEquals(0, frame.dstWorld);
    assertEquals(0, frame.dstPort);
    assertEquals(0, frame.srcPort);
    assertArrayEquals(new byte[] {(byte) 192, (byte) 168, 1, 10}, frame.getSrcIp());
    assertArrayEquals(new int[] {1, 2, 3}, frame.getPayload());
  }
}
