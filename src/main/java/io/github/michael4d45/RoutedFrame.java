package io.github.michael4d45;

/**
 * Common routing metadata for frames that can be delivered through the {@link DataRouter}.
 *
 * <p>Implement this on any frame type that has a destination world and port so it can be
 * transported generically without overloading methods for each concrete frame class.
 */
public interface RoutedFrame {

  /**
   * Gets the numeric ID assigned to the destination world.
   *
   * <p>The mapping between world IDs and actual {@code ServerWorld}s is managed by {@link
   * DataRouter}. An ID of {@code -1} should be treated as invalid.
   *
   * @return destination world ID
   */
  int getDstWorld();

  /**
   * Gets the destination port within the destination world.
   *
   * @return destination port (0-65535)
   */
  int getDstPort();
}
