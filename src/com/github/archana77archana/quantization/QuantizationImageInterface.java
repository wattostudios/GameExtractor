
package com.github.archana77archana.quantization;

import java.awt.image.BufferedImage;

/**
 *
 * @author casey
 */
public interface QuantizationImageInterface {
  int[][] convertTo2DUsingGetRGB(BufferedImage image);

  void writeImage(int r, int c, int[][] res);
}
