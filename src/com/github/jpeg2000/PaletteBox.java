
package com.github.jpeg2000;

import java.awt.image.IndexColorModel;
import java.io.DataOutputStream;
import java.io.IOException;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class represents the "pclr" box.
 *
 * Its content contains the number of palette entry, the number of color
 * components, the bit depths of the output components, the LUT.
 *
 * @author http://bfo.com
 */
public class PaletteBox extends Box {

  private int numEntries;

  private int numc;

  private int[] bitDepth;

  private int[] lut;

  public PaletteBox() {
    super(fromString("pclr"));
  }

  /**
   * Constructs a <code>PlatteBox</code> from an <code>IndexColorModel</code>.
   * Lifted from JAI, untested
   */
  public PaletteBox(IndexColorModel icm) {
    this(icm.hasAlpha() ? new int[] { 8, 8, 8, 8 } : new int[] { 8, 8, 8 }, getLUT(icm));
  }

  public PaletteBox(int len, int numc, byte[] lut) {
    this();
    this.numEntries = len;
    this.numc = numc;
    this.bitDepth = new int[numc];
    for (int i = 0; i < numc; i++) {
      bitDepth[i] = 8;
    }
    this.lut = new int[len * numc];
    for (int i = 0; i < lut.length; i++) {
      this.lut[i] = lut[i] & 0xFF;
    }
  }

  /**
   * Constructs a <code>PlatteBox</code> from the provided length, bit
   *  depths of the color components and the LUT.
   */
  public PaletteBox(int[] depths, int[] lut) {
    this();
    this.bitDepth = depths;
    this.lut = lut;
    this.numc = depths.length;
    this.numEntries = lut.length / numc;
  }

  /** Gets the LUT from the <code>IndexColorModel</code> as an two-dimensional
   *  byte array.
   */
  private static int[] getLUT(IndexColorModel icm) {
    int size = icm.getMapSize();
    int numc = icm.hasAlpha() ? 3 : 4;   // IndexColorModel always RGB or RGBA
    int[] lut = new int[size * numc];
    int k = 0;
    for (int i = 0; i < size; i++) {
      lut[k++] = icm.getRed(i);
      lut[k++] = icm.getGreen(i);
      lut[k++] = icm.getBlue(i);
      if (numc == 4) {
        lut[k++] = icm.getAlpha(i);
      }
    }
    return lut;
  }

  public int getComponentDepth(int component) {
    return (bitDepth[component] & 0x7F) + 1;
  }

  public int getComponentValue(int entry, int component) {
    return lut[entry * numc + component];
  }

  /** Return the number of color components. */
  public int getNumComp() {
    return numc;
  }

  /** Return the number of palette entries. */
  public int getNumEntries() {
    return numEntries;
  }

  public boolean isComponentSigned(int component) {
    return (bitDepth[component] & 0x80) != 0;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    numEntries = in.readShort();
    numc = in.readByte() & 0xFF;
    bitDepth = new int[numc];
    for (int i = 0; i < numc; i++) {
      bitDepth[i] = in.readByte() & 0xFF;
    }
    lut = new int[in.length() - in.getPos()];
    for (int i = 0; i < lut.length; i++) {
      int c = i % numc;
      int d = getComponentDepth(c);
      boolean signed = isComponentSigned(c);
      if (d <= 8) {
        lut[i] = signed ? (int) in.readByte() : in.readByte() & 0xFF;
      }
      else {
        lut[i] = signed ? (int) in.readShort() : in.readShort() & 0xFFFF;
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"depth\":[");
    for (int i = 0; i < numc; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(bitDepth[i]);
    }
    sb.append("],\"lut\":[");
    for (int i = 0; i < lut.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(lut[i]);
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.writeShort(numEntries);
    out.write(numc);
    for (int i = 0; i < numc; i++) {
      out.write(bitDepth[i]);
    }
    for (int i = 0; i < lut.length; i++) {
      int c = i % numc;
      int v = i / numc;
      int d = getComponentDepth(c);
      if (d <= 8) {
        out.write(getComponentValue(v, c));
      }
      else {
        out.writeShort(getComponentValue(v, c));
      }
    }
  }

}
