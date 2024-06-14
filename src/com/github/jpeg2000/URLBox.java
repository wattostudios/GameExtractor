
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class represents the "url " box.
 * Its content are a one-byte version, a three-byte flags and
 * a URL pertains to the UUID List box within its UUID Info superbox.
 *
 * @author http://bfo.com
 */
public class URLBox extends Box {

  /** The element values. */
  private int version;

  private int flags;

  private String url;

  public URLBox() {
    super(fromString("url "));
  }

  /** Constructs a <code>DataEntryURLBox</code> from its data elements. */
  public URLBox(int version, int flags, String url) {
    this();
    this.version = version;
    this.flags = flags;
    this.url = url;
  }

  /** Returns the <code>Flags</code> data element. */
  public int getFlags() {
    return flags;
  }

  @Override
  public int getLength() {
    return 4 + url.length();
  }

  /** Returns the <code>URL</code> data element. */
  public String getURL() {
    return url;
  }

  /** Returns the <code>Version</code> data element. */
  public int getVersion() {
    return version;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    version = in.read();
    flags = (in.read() << 16) | (in.read() << 8) | in.read();

    byte[] b = new byte[in.length() - in.getPos()];
    in.readFully(b, 0, b.length);
    url = new String(b, 0, b.length, "ISO-8859-1");
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"version\":");
    sb.append(getVersion());
    sb.append(",\"flags\":");
    sb.append(getFlags());
    sb.append(",\"url\":\"");
    sb.append(getURL().replaceAll("\"", "\\\""));
    sb.append("\"}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.write(version);
    out.write(flags >> 16);
    out.write(flags >> 8);
    out.write(flags);
    out.write(url.getBytes("ISO-8859-1"));
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    out.writeAttribute("version", Integer.toString(getVersion()));
    out.writeAttribute("flags", "0x" + Integer.toHexString(getFlags()));
    out.writeCharacters(getURL());
    out.writeEndElement();
  }

}
