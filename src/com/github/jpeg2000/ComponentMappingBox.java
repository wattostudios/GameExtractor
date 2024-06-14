
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This represents the "cmap" box.
 * This box exists if and only is a PaletteBox exists.  Its
 * content defines the type LUT output components and their mapping to the
 * color component.
 *
 * @author http://bfo.com
 */
public class ComponentMappingBox extends Box {

  private short[] components;

  private byte[] type;

  private byte[] map;

  public ComponentMappingBox() {
    super(fromString("cmap"));
  }

  /**
   * Constructs a <code>ComponentMappingBox</code> from the provided
   * component mapping.
   */
  public ComponentMappingBox(short[] comp, byte[] t, byte[] m) {
    this();
    this.components = comp;
    this.type = t;
    this.map = m;
  }

  public short[] getComponent() {
    return components;
  }

  public byte[] getComponentAssociation() {
    return map;
  }

  public byte[] getComponentType() {
    return type;
  }

  @Override
  public int getLength() {
    return components.length * 4;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    int len = in.length() / 4;
    components = new short[len];
    type = new byte[len];
    map = new byte[len];

    for (int i = 0; i < len; i++) {
      components[i] = in.readShort();
      type[i] = in.readByte();
      map[i] = in.readByte();
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"components\":[");
    for (int i = 0; i < components.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("{\"component\":");
      sb.append(components[i]);
      sb.append(",\"type\":");
      sb.append(type[i]);
      sb.append(",\"association\":");
      sb.append(map[i]);
      sb.append("}");
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    for (int i = 0; i < type.length; i++) {
      out.writeShort(components[i]);
      out.write(type[i]);
      out.write(map[i]);
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    for (int i = 0; i < components.length; i++) {
      out.writeEmptyElement("cmap");
      out.writeAttribute("component", Integer.toString(components[i]));
      out.writeAttribute("type", Integer.toString(type[i]));
      out.writeAttribute("assoc", Integer.toString(map[i]));
    }
    out.writeEndElement();
  }

}
