
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class is defined to represent a Bits Per Component Box of JPEG
 * JP2 file format.  A Bits Per Component box has a length, and a fixed
 * type of "bpcc".  Its content is a byte array containing the bit
 * depths of the color components.
 *
 * This box is necessary only when the bit depth are not identical for all
 * the components.
 *
 * @author http://bfo.com
 */
public class BitsPerComponentBox extends Box {

  private byte[] data;

  public BitsPerComponentBox() {
    super(fromString("bpcc"));
  }

  public BitsPerComponentBox(byte[] bitDepth) {
    this();
    this.data = bitDepth;
  }

  /**
   * Returns the bit depths for all the image components.
   */
  public byte[] getBitDepth() {
    return data;
  }

  @Override
  public int getLength() {
    return data.length;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    data = new byte[in.length()];
    in.readFully(data, 0, data.length);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"bpc\":[");
    for (int i = 0; i < data.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append(data[i]);
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.write(data);
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    for (int i = 0; i < data.length; i++) {
      out.writeStartElement("bpc");
      out.writeCharacters(Integer.toString(data[i] & 0xFF));
      out.writeEndElement();
    }
    out.writeEndElement();
  }

}
