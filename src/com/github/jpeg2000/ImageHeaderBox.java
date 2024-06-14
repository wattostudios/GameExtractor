
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This represents the "ihdr" Box
 *
 * The content of an image header box contains the width/height, number of
 * image components, the bit depth (coded with sign/unsign information),
 * the compression type (7 for JP2 file), the flag to indicate the color
 * space is known or not, and a flag to indicate whether the intellectual
 * property information included in this file.
 *
 * @author http://bfo.com
 */
public class ImageHeaderBox extends Box {

  private int width;

  private int height;

  private short numComp;

  private byte bitDepth;

  private byte compressionType;

  private byte unknownColor;

  private byte intelProp;

  public ImageHeaderBox() {
    super(fromString("ihdr"));
  }

  /** Create an Image Header Box from the element values. */
  public ImageHeaderBox(int width, int height, int numComp, int bitDepth, boolean unknownColor, boolean intelProp) {
    this();
    this.height = height;
    this.width = width;
    this.numComp = (short) numComp;
    this.bitDepth = (byte) bitDepth;
    this.compressionType = (byte) 7;
    this.unknownColor = (byte) (unknownColor ? 1 : 0);
    this.intelProp = (byte) (intelProp ? 1 : 0);
  }

  /** Returns the bit depth for all the image components. */
  public byte getBitDepth() {
    return bitDepth;
  }

  /** Returns the compression type. */
  public byte getCompressionType() {
    return compressionType;
  }

  /** Returns the height of the image. */
  public int getHeight() {
    return height;
  }

  /** Returns the <code>IntellectualProperty</code> flag. */
  public byte getIntellectualProperty() {
    return intelProp;
  }

  @Override
  public int getLength() {
    return 14;
  }

  /** Returns the number of image components. */
  public short getNumComponents() {
    return numComp;
  }

  /** Returns the <code>UnknowColorspace</code> flag. */
  public byte getUnknownColorspace() {
    return unknownColor;
  }

  /** Returns the width of the image. */
  public int getWidth() {
    return width;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    height = in.readInt();
    width = in.readInt();
    numComp = in.readShort();
    bitDepth = in.readByte();
    compressionType = in.readByte();
    unknownColor = in.readByte();
    intelProp = in.readByte();
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"width\":\"");
    sb.append(getWidth());
    sb.append(",\"height\":\"");
    sb.append(getHeight());
    sb.append(",\"numc\":\"");
    sb.append(getNumComponents());
    if (getBitDepth() != 255) {
      sb.append(",\"depth\":\"");
      sb.append(getBitDepth());
    }
    if (getCompressionType() != 7) {
      sb.append(",\"compression\":\"");
      sb.append(getCompressionType());
    }
    sb.append("\",\"unknownColorSpace\":");
    sb.append(getUnknownColorspace());
    sb.append("\",\"ip\":");
    sb.append(getIntellectualProperty());
    sb.append("}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.writeInt(height);
    out.writeInt(width);
    out.writeShort(numComp);
    out.write(bitDepth);
    out.write(compressionType);
    out.write(unknownColor);
    out.write(intelProp);
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeEmptyElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    out.writeAttribute("width", Integer.toString(getWidth()));
    out.writeAttribute("height", Integer.toString(getHeight()));
    out.writeAttribute("numc", Integer.toString(getNumComponents()));
    if (getBitDepth() != 255) {
      out.writeAttribute("depth", Integer.toString(getBitDepth()));
    }
    if (getCompressionType() != 7) {
      out.writeAttribute("compression", Integer.toString(getCompressionType()));
    }
    out.writeAttribute("unknownColorSpace", Integer.toString(getUnknownColorspace()));
    out.writeAttribute("ip", Integer.toString(getIntellectualProperty()));
  }
}
