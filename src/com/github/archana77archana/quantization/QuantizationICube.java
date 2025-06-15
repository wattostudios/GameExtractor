
package com.github.archana77archana.quantization;

/**
 *
 * @author casey
 */
public interface QuantizationICube {
  void classification();

  void reduction();

  void assignment();

  int[] colorMap();

  int[][] pixels();
}
