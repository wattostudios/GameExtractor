
package com.github.archana77archana.quantization;

/**
 *
 * @author casey
 */

public class Quantize {
  static boolean QUICK = true;

  static int MAX_RGB = 256;
  static int MAX_NODES = 266817;
  static int MAX_TREE_DEPTH = 8;

  // these are precomputed in advance
  static int SQUARES[];
  static int SHIFT[];

  public Quantize() {
  }

  public Quantize(int max_colors) {
    MAX_RGB = max_colors;

    SQUARES = new int[MAX_RGB + MAX_RGB + 1];
    for (int i = -MAX_RGB; i <= MAX_RGB; i++) {
      SQUARES[i + MAX_RGB] = i * i;
    }

    SHIFT = new int[MAX_TREE_DEPTH + 1];
    for (int i = 0; i < MAX_TREE_DEPTH + 1; ++i) {
      SHIFT[i] = 1 << (15 - i);
    }
  }

  /**
   * Reduce the image to the given number of colors. The pixels are
   * reduced in place.
   * @param pixels
   * @param max_colors
   * @return The new color palette.
   */
  public int[][] quantizeImage(int pixels[][]) {
    QuantizationOctree cube = new QuantizationOctree(pixels, MAX_RGB);
    cube.classification();
    cube.reduction();
    cube.assignment();
    return cube.result(cube.pixels, cube.colormap);
  }

  /*
  public static void main(String[] args) throws IOException {
    //Image image =  new ImageIcon("large.jpg").getImage();
  
    BufferedImage hugeImage = ImageIO.read(Quantize.class.getResource("full-color.jpg"));
    QuantizationImageOperations i = new QuantizationImageOperations();
  
    int[][] result = i.convertTo2DUsingGetRGB(hugeImage);
  
    Quantize q = new Quantize();
    int res[][] = q.quantizeImage(result, q.MAX_RGB);
  
    i.writeImage(res.length, res[0].length, res);
  
  }
  */

}
