package com.github.archana77archana.quantization;

/**
 *
 * @author casey
 */
/**
 * A single Node in the tree.
 */
public class QuantizationNode {
  Quantize q = new Quantize();
  QuantizationOctree cube;

  // parent node
  QuantizationNode parent;

  // child nodes
  QuantizationNode child[];
  int nchild;

  // our index within our parent
  int id;
  // our level within the tree
  int level;
  // our color midpoint
  int mid_red;
  int mid_green;
  int mid_blue;

  // the pixel count for this node and all children
  int number_pixels;

  // the pixel count for this node
  int unique;
  // the sum of all pixels contained in this node
  int total_red;
  int total_green;
  int total_blue;

  // used to build the colormap
  int color_number;

  @SuppressWarnings("static-access")
  QuantizationNode(QuantizationOctree cube) {
    this.cube = cube;
    this.parent = this;
    this.child = new QuantizationNode[8];
    this.id = 0;
    this.level = 0;

    this.number_pixels = Integer.MAX_VALUE;

    this.mid_red = (q.MAX_RGB + 1) >> 1;
    this.mid_green = (q.MAX_RGB + 1) >> 1;
    this.mid_blue = (q.MAX_RGB + 1) >> 1;
  }

  @SuppressWarnings("static-access")
  QuantizationNode(QuantizationNode parent, int id, int level) {
    this.cube = parent.cube;
    this.parent = parent;
    this.child = new QuantizationNode[8];
    this.id = id;
    this.level = level;

    // add to the cube
    ++cube.nodes;
    if (level == cube.depth) {
      ++cube.colors;
    }

    // add to the parent
    ++parent.nchild;
    parent.child[id] = this;

    // figure out our midpoint
    int bi = (1 << (q.MAX_TREE_DEPTH - level)) >> 1;
    mid_red = parent.mid_red + ((id & 1) > 0 ? bi : -bi);
    mid_green = parent.mid_green + ((id & 2) > 0 ? bi : -bi);
    mid_blue = parent.mid_blue + ((id & 4) > 0 ? bi : -bi);
  }

  /**
   * Remove this child node, and make sure our parent
   * absorbs our pixel statistics.
   */
  void pruneChild() {
    --parent.nchild;
    parent.unique += unique;
    parent.total_red += total_red;
    parent.total_green += total_green;
    parent.total_blue += total_blue;
    parent.child[id] = null;
    --cube.nodes;
    cube = null;
    parent = null;
  }

  /**
   * Prune the lowest layer of the tree.
   */
  void pruneLevel() {
    if (nchild != 0) {
      for (int id = 0; id < 8; id++) {
        if (child[id] != null) {
          child[id].pruneLevel();
        }
      }
    }
    if (level == cube.depth) {
      pruneChild();
    }
  }

  /**
   * Remove any nodes that have fewer than threshold
   * pixels. Also, as long as we're walking the tree:
   *
   *  - figure out the color with the fewest pixels
   *  - recalculate the total number of colors in the tree
   */
  int reduce(int threshold, int next_threshold) {
    if (nchild != 0) {
      for (int id = 0; id < 8; id++) {
        if (child[id] != null) {
          next_threshold = child[id].reduce(threshold, next_threshold);
        }
      }
    }
    if (number_pixels <= threshold) {
      pruneChild();
    }
    else {
      if (unique != 0) {
        cube.colors++;
      }
      if (number_pixels < next_threshold) {
        next_threshold = number_pixels;
      }
    }
    return next_threshold;
  }

  /*
   * colormap traverses the color cube tree and notes each
   * colormap entry. A colormap entry is any node in the
   * color cube tree where the number of unique colors is
   * not zero.
   */
  void colormap() {
    if (nchild != 0) {
      for (int id = 0; id < 8; id++) {
        if (child[id] != null) {
          child[id].colormap();
        }
      }
    }
    if (unique != 0) {
      int r = ((total_red + (unique >> 1)) / unique);
      int g = ((total_green + (unique >> 1)) / unique);
      int b = ((total_blue + (unique >> 1)) / unique);
      cube.colormap[cube.colors] = (((0xFF) << 24) |
          ((r & 0xFF) << 16) |
          ((g & 0xFF) << 8) |
          ((b & 0xFF) << 0));
      color_number = cube.colors++;
    }
  }

  /* ClosestColor traverses the color cube tree at a
   * particular node and determines which colormap entry
   * best represents the input color.
   */
  void closestColor(int red, int green, int blue, QuantizationOctree.Search search) {
    if (nchild != 0) {
      for (int id = 0; id < 8; id++) {
        if (child[id] != null) {
          child[id].closestColor(red, green, blue, search);
        }
      }
    }

    if (unique != 0) {
      int color = cube.colormap[color_number];
      int distance = distance(color, red, green, blue);
      if (distance < search.distance) {
        search.distance = distance;
        search.color_number = color_number;
      }
    }
  }

  /**
   * Figure out the distance between this node and som color.
   */
  @SuppressWarnings("static-access")
  final int distance(int color, int r, int g, int b) {
    return (q.SQUARES[((color >> 16) & 0xFF) - r + q.MAX_RGB] +
        q.SQUARES[((color >> 8) & 0xFF) - g + q.MAX_RGB] +
        q.SQUARES[((color >> 0) & 0xFF) - b + q.MAX_RGB]);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    if (parent == this) {
      buf.append("root");
    }
    else {
      buf.append("node");
    }
    buf.append(' ');
    buf.append(level);
    buf.append(" [");
    buf.append(mid_red);
    buf.append(',');
    buf.append(mid_green);
    buf.append(',');
    buf.append(mid_blue);
    buf.append(']');
    return new String(buf);
  }
}
