
package com.github.archana77archana.quantization;

/**
 *
 * @author casey
 */
public class QuantizationOctree implements QuantizationICube {

  Quantize q = new Quantize();

  int pixels[][];
  int max_colors;
  int colormap[];

  QuantizationNode root;
  int depth;

  // counter for the number of colors in the cube. this gets
  // recalculated often.
  int colors;

  // counter for the number of nodes in the tree
  int nodes;

  @SuppressWarnings("static-access")
  QuantizationOctree(int pixels[][], int max_colors) {
    this.pixels = pixels;
    this.max_colors = max_colors;

    int i = max_colors;
    // tree_depth = log max_colors
    //                 4
    for (depth = 1; i != 0; depth++) {
      i /= 4;
    }
    if (depth > 1) {
      --depth;
    }
    if (depth > q.MAX_TREE_DEPTH) {
      depth = q.MAX_TREE_DEPTH;
    }
    else if (depth < 2) {
      depth = 2;
    }

    root = new QuantizationNode(this);
  }

  @SuppressWarnings("static-access")
  public void classification() {
    int pixels[][] = this.pixels;

    int width = pixels.length;
    int height = pixels[0].length;

    // convert to indexed color
    for (int x = width; x-- > 0;) {
      for (int y = height; y-- > 0;) {
        int pixel = pixels[x][y];
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = (pixel >> 0) & 0xFF;

        // a hard limit on the number of nodes in the tree
        if (nodes > q.MAX_NODES) {
          System.out.println("pruning");
          root.pruneLevel();
          --depth;
        }

        // walk the tree to depth, increasing the
        // number_pixels count for each node
        QuantizationNode node = root;
        for (int level = 1; level <= depth; ++level) {
          int id = (((red > node.mid_red ? 1 : 0) << 0) |
              ((green > node.mid_green ? 1 : 0) << 1) |
              ((blue > node.mid_blue ? 1 : 0) << 2));
          if (node.child[id] == null) {
            new QuantizationNode(node, id, level);
          }
          node = node.child[id];
          node.number_pixels += q.SHIFT[level];
        }

        ++node.unique;
        node.total_red += red;
        node.total_green += green;
        node.total_blue += blue;
      }
    }
  }

  public void reduction() {
    int threshold = 1;
    while (colors > max_colors) {
      colors = 0;
      threshold = root.reduce(threshold, Integer.MAX_VALUE);
    }
  }

  public int[][] result(int[][] pixels, int[] colormap) {
    int[][] imageArray = new int[pixels.length][pixels[0].length];

    for (int i = 0; i < pixels.length; i++) {
      for (int j = 0; j < pixels[0].length; j++) {
        imageArray[i][j] = colormap[pixels[i][j]];
      }
    }

    return imageArray;
  }

  //  /**
  //   * The result of a closest color search.
  //   */
  static class Search {
    int distance;
    int color_number;
  }

  @SuppressWarnings("static-access")
  public void assignment() {
    colormap = new int[colors];

    colors = 0;
    root.colormap();

    int pixels[][] = this.pixels;

    int width = pixels.length;
    int height = pixels[0].length;

    Search search = new Search();

    // convert to indexed color
    for (int x = width; x-- > 0;) {
      for (int y = height; y-- > 0;) {
        int pixel = pixels[x][y];
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = (pixel >> 0) & 0xFF;
        //System.out.println("Red: "+red+" Green: "+green+" Blue: "+blue);

        // walk the tree to find the cube containing that color
        QuantizationNode node = root;
        for (;;) {
          int id = (((red > node.mid_red ? 1 : 0) << 0) |
              ((green > node.mid_green ? 1 : 0) << 1) |
              ((blue > node.mid_blue ? 1 : 0) << 2));
          if (node.child[id] == null) {
            break;
          }
          node = node.child[id];
        }

        if (q.QUICK) {
          // if QUICK is set, just use that
          // node. Strictly speaking, this isn't
          // necessarily best match.
          pixels[x][y] = node.color_number;
        }
        else {
          // Find the closest color.
          search.distance = Integer.MAX_VALUE;
          node.parent.closestColor(red, green, blue, search);
          pixels[x][y] = search.color_number;
        }
      }
    }
  }

  public int[] colorMap() {
    return colormap;
  }

  public int[][] pixels() {
    return pixels;
  }
}
