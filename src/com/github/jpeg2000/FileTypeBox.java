
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This represents the "ftyp: box.
 *
 * The content of a file type box contains the brand ("jp2 " for JP2 file",
 * the minor version (0 for JP2 file format), and a compatibility list (one of
 * which should be "jp2 " if brand is not "jp2 ".)
 *
 * @author http://bfo.com
 */
public class FileTypeBox extends Box {

  private int brand;

  private int minorVersion;

  private int[] compat;

  public FileTypeBox() {
    super(fromString("ftyp"));
    brand = 0x6a703220;
    minorVersion = 0;
    compat = new int[] { fromString("jp2 ") };
  }

  /** Constructs a <code>FileTypeBox</code> from the provided brand, minor
   *  version and compatibility list.
   */
  public FileTypeBox(int br, int minorVersion, int[] compat) {
    this();
    this.brand = br;
    this.minorVersion = minorVersion;
    this.compat = compat;
  }

  /** Returns the brand of this file type box. */
  public int getBrand() {
    return brand;
  }

  /** Returns the compatibility list of this file type box. */
  public int[] getCompatibilityList() {
    return compat;
  }

  @Override
  public int getLength() {
    return 8 + compat.length * 4;
  }

  /** Returns the minor version of this file type box. */
  public int getMinorVersion() {
    return minorVersion;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    brand = in.readInt();
    minorVersion = in.readInt();
    int len = (in.length() - in.getPos()) / 4;
    if (len > 0) {
      compat = new int[len];
      for (int i = 0; i < len; i++) {
        compat[i] = in.readInt();
      }
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"brand\":\"");
    sb.append(toString(brand));
    sb.append("\",\"minorVersion\":");
    sb.append(minorVersion);
    sb.append("\",\"compat\":[");
    for (int i = 0; i < compat.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("\"");
      sb.append(toString(compat[i]));
      sb.append("\"");
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.writeInt(brand);
    out.writeInt(minorVersion);
    for (int i = 0; i < compat.length; i++) {
      out.writeInt(compat[i]);
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    out.writeAttribute("brand", toString(brand));
    out.writeAttribute("minorVersion", "0x" + Integer.toHexString(minorVersion));
    for (int i = 0; i < compat.length; i++) {
      out.writeStartElement("compat");
      out.writeCharacters(toString(compat[i]));
      out.writeEndElement();
    }
    out.writeEndElement();
  }

}
