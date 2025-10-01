package io.github.michael4d45;

/** Stub for Frame abstract class. */
public abstract class Frame {

  public int destinationPort; // port number 1-255
  public int destinationWorld; // world number

  public abstract int[] getPayload();

  public abstract int[] buildSymbols();
}
