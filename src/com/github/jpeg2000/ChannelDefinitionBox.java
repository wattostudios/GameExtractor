
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class is designed to represent a Channel Definition Box of
 * JPEG JP2 file format.  A Channel Definition Box has a length, and
 * a fixed type of "cdef".  Its content defines the type of the image
 * channels: color channel, alpha channel or premultiplied alpha channel.
 *
 * CORRECTION - it is actually "ComponentDefinitionBox" in the spec.
 *
 * @author http://bfo.com
 */
public class ChannelDefinitionBox extends Box {

  /** The cached data elements. */
  private short num;

  private short[] channels;

  private short[] types;

  private short[] associations;

  public ChannelDefinitionBox() {
    super(fromString("cdef"));
  }

  /** Constructs a <code>ChannelDefinitionBox</code> based on the provided  <code>ColorModel</code>.
   *
   * This was from JAI but is untested by BFO
   *
  public ChannelDefinitionBox(ColorModel colorModel) {
      this();
  
      // creates the buffers for the channel definitions.
      short length = (short)(colorModel.getComponentSize().length - 1);
      num = (short)(length * (colorModel.isAlphaPremultiplied() ? 3 : 2));
      channels = new short[num];
      types = new short[num];
      associations = new short[num];
  
      // fills the arrays.
      fillBasedOnBands(length, colorModel.isAlphaPremultiplied(), channels, types, associations);
  }
  
  private static void fillBasedOnBands(int numComps, boolean isPremultiplied, short[] c, short[] t, short[] a) {
      int num = numComps * (isPremultiplied ? 3 : 2);
      if (isPremultiplied) {
          for (int i = numComps * 2; i < num; i++) {
              c[i] = (short)(i - numComps * 2);
              t[i] = 2;       // 2 -- premultiplied
              a[i] = (short)(i + 1 - numComps * 2);
          }
      }
  
      for (int i = 0; i < numComps; i++) {
          int j = i + numComps;
          c[i] = (short)i;
          t[i] = 0;       // The original channel
          a[j] = a[i] = (short)(i + 1);
  
          c[j] = (short)numComps;
          t[j] = 1;           // 1 -- transparency
      }
  }
   */

  /**
   * Constructs a <code>ChannelDefinitionBox</code> based on the provided
   * channel definitions.
   */
  public ChannelDefinitionBox(short[] channel, short[] types, short[] associations) {
    this();
    this.num = (short) channel.length;
    this.channels = channel;
    this.types = types;
    this.associations = associations;
  }

  /**
   * Returns the association which associates a color channel to a color
   * component in the color space of the image.
   */
  public short[] getAssociation() {
    return associations;
  }

  /**
   * Returns the defined channels.
   */
  public short[] getChannel() {
    return channels;
  }

  @Override
  public int getLength() {
    return 2 + num * 6;
  }

  /**
   * Returns the channel types. Values are 0 (color), 1 (opacity), 2 (premultiplied opacity) or -1 (unspecified)
   */
  public short[] getTypes() {
    return types;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    // Note all of these are actually unsigned shorts, so any images with > 16383 channels will fail...
    num = in.readShort();
    channels = new short[num];
    types = new short[num];
    associations = new short[num];

    for (int i = 0; i < num; i++) {
      channels[i] = in.readShort();
      types[i] = in.readShort();
      associations[i] = in.readShort();
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"channels\":[");
    for (int i = 0; i < channels.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      sb.append("{\"channel\":");
      sb.append(channels[i]);
      sb.append(",\"type\":");
      sb.append(types[i]);
      sb.append(",\"association\":");
      sb.append(associations[i]);
      sb.append("}");
    }
    sb.append("]}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.writeShort(num);
    for (int i = 0; i < num; i++) {
      out.writeShort(channels[i]);
      out.writeShort(types[i]);
      out.writeShort(associations[i]);
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    for (int i = 0; i < channels.length; i++) {
      out.writeEmptyElement("cmap");
      out.writeAttribute("channel", Integer.toString(channels[i]));
      out.writeAttribute("type", Integer.toString(types[i]));
      out.writeAttribute("assoc", Integer.toString(associations[i]));
    }
    out.writeEndElement();
  }

}
