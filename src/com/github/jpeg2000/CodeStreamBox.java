
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This represents the "jp2c" box, which contains the CodeStream
 *
 * @author http://bfo.com
 */
public class CodeStreamBox extends Box {

  private byte[] data;

  private int length;

  private RandomAccessIO io;

  public CodeStreamBox() {
    super(fromString("jp2c"));
  }

  public CodeStreamBox(byte[] data) {
    this();
    this.data = data;
    this.length = data.length;
  }

  @Override
  public int getLength() {
    return length;
  }

  /**
   * Return a RandomAccessIO which contains the codestream to pass to the BitstreamReader
   */
  public RandomAccessIO getRandomAccessIO() throws IOException {
    if (io != null) {
      io.seek(0);
      return io;
    }
    throw new IllegalStateException("Not created from a RandomAccessIO");
  }

  @Override
  public void read(RandomAccessIO io) throws IOException {
    this.io = io;
    this.length = io.length();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    if (data != null) {
      out.write(data);
    }
    else if (io != null) {
      byte[] b = new byte[8192];
      io.seek(0);
      int remaining = io.length();
      while (remaining > 0) {
        int c = Math.min(b.length, remaining);
        io.readFully(b, 0, c);
        out.write(b, 0, c);
        remaining -= c;
      }
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));

    //J2KReadParam param = new SimpleJ2KReadParam();
    HeaderInfo hi = new HeaderInfo();
    //try {
    //HeaderDecoder hd = new HeaderDecoder(getRandomAccessIO(), param, hi);

    out.writeStartElement("SIZ");
    out.writeAttribute("iw", Integer.toString(hi.siz.xsiz - hi.siz.x0siz));
    out.writeAttribute("ih", Integer.toString(hi.siz.ysiz - hi.siz.y0siz));
    out.writeAttribute("tw", Integer.toString(hi.siz.xtsiz));
    out.writeAttribute("th", Integer.toString(hi.siz.ytsiz));
    out.writeAttribute("numc", Integer.toString(hi.siz.csiz));
    for (int i = 0; i < hi.siz.csiz; i++) {
      out.writeStartElement("component");
      out.writeAttribute("depth", Integer.toString(hi.siz.getOrigBitDepth(i)));
      out.writeAttribute("signed", Boolean.toString(hi.siz.isOrigSigned(i)));
      out.writeAttribute("subx", Integer.toString(hi.siz.xrsiz[i]));
      out.writeAttribute("suby", Integer.toString(hi.siz.yrsiz[i]));
      out.writeEndElement();
    }
    //}
    //catch (IOException e) {
    //  throw new RuntimeException(e);
    //}
    out.writeEndElement();
  }
}
