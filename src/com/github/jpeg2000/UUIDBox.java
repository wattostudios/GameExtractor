
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This class represents the "uuid" box.
 * Its content is a 16-byte UUID followed with a various-length data.
 *
 * @author http://bfo.com
 */
public class UUIDBox extends Box {

  public static final String UUID_XMP = "be7acfcb97a942e89c71999491e3afac";

  private byte[] uuid;

  private byte[] data;

  /**
   * Constructs a <code>UUIDBox</code> from its content data array.
   */
  public UUIDBox() {
    super(fromString("uuid"));
  }

  /**
   * Create a new UUID box
   * @param key the key, which must be a 16 byte long array encoded as a 32-character long hex string
   * @parma data the data
   */
  public UUIDBox(String key, byte[] data) {
    this();
    if (key.length() != 32) {
      throw new IllegalArgumentException();
    }
    uuid = new byte[16];
    for (int i = 0; i < 16; i++) {
      uuid[i] = (byte) ((Character.digit(key.charAt(i * 2), 16) << 4) + Character.digit(key.charAt(i * 2 + 1), 16));
    }
    this.data = data;
  }

  /** Returns the UUID data of this box. */
  public byte[] getData() {
    return data;
  }

  @Override
  public int getLength() {
    return uuid.length + data.length;
  }

  /** Returns the UUID of this box. */
  public String getUUID() {
    return toString(uuid);
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    uuid = new byte[16];
    in.readFully(uuid, 0, 16);
    data = new byte[in.length() - in.getPos()];
    in.readFully(data, 0, data.length);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append("\"uuid\":\"");
    sb.append(getUUID());
    sb.append("\",\"data\":");
    sb.append(toString(getData()));
    sb.append("\"}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.write(uuid, 0, uuid.length);
    out.write(data, 0, data.length);
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    out.writeAttribute("uuid", getUUID());
    boolean raw = true;
    if (UUID_XMP.equals(getUUID())) {
      try {
        String s = new String(getData(), "UTF-8");
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.transform(new StreamSource(new StringReader(s)), new StreamResult(new StringWriter()));
        // Syntax is valid, redo output to actual outputstream, removing startDoc/endDoc
        final XMLEventWriter w = XMLOutputFactory.newInstance().createXMLEventWriter(new StAXResult(out));
        t.transform(new StreamSource(new StringReader(s)), new StAXResult(new XMLEventWriter() {

          public void add(XMLEvent event) throws XMLStreamException {
            if (!event.isStartDocument() && !event.isEndDocument()) {
              w.add(event);
            }
          }

          public void add(XMLEventReader reader) throws XMLStreamException {
            w.add(reader);
          }

          public void close() throws XMLStreamException {
          }

          public void flush() throws XMLStreamException {
            w.flush();
          }

          public NamespaceContext getNamespaceContext() {
            return w.getNamespaceContext();
          }

          public String getPrefix(String uri) throws XMLStreamException {
            return w.getPrefix(uri);
          }

          public void setDefaultNamespace(String uri) throws XMLStreamException {
            w.setDefaultNamespace(uri);
          }

          public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
            w.setNamespaceContext(context);
          }

          public void setPrefix(String prefix, String uri) throws XMLStreamException {
            w.setPrefix(prefix, uri);
          }
        }));
        raw = false;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (raw) {
      out.writeCharacters(toString(data));
    }
    out.writeEndElement();
  }
}
