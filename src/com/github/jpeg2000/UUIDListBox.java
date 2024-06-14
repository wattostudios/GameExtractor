
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class represents the "ulst" box.
 * Its contents include the number of UUID entry and a list of 16-byte UUIDs.
 *
 * @author http://bfo.com
 */
public class UUIDListBox extends Box {

  private byte[][] uuids;

  public UUIDListBox() {
    super(fromString("ulst"));
  }

  /**
   * Constructs a <code>UUIDListBox</code> from the provided list of UUIDs
   * @param uuids a list of uuids, each represented as 32-character long strings representing a hex-encoded 16-byte array
   */
  public UUIDListBox(String[] uuids) {
    this();
    this.uuids = new byte[uuids.length][16];
    for (int j = 0; j < uuids.length; j++) {
      String key = uuids[j];
      if (key.length() != 32) {
        throw new IllegalArgumentException();
      }
      for (int i = 0; i < 16; i++) {
        this.uuids[j][i] = (byte) ((Character.digit(key.charAt(i * 2), 16) << 4) + Character.digit(key.charAt(i * 2 + 1), 16));
      }
    }
  }

  @Override
  public int getLength() {
    return 2 + uuids.length * 16;
  }

  public int getSize() {
    return uuids.length;
  }

  public String getUUID(int i) {
    return toString(uuids[i]);
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    int num = in.readShort();
    uuids = new byte[num][16];
    for (int i = 0; i < num; i++) {
      in.readFully(uuids[i], 0, 16);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"uuids\":[");
    for (int i = 0; i < uuids.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("\"");
      sb.append(toString(uuids[i]));
      sb.append("\"");
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.writeShort(uuids.length);
    for (int i = 0; i < uuids.length; i++) {
      out.write(uuids[i]);
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    for (int i = 0; i < uuids.length; i++) {
      out.writeStartElement("uuid");
      out.writeCharacters(toString(uuids[i]));
      out.writeEndElement();
    }
    out.writeEndElement();
  }

}
