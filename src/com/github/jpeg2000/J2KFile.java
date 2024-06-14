
package com.github.jpeg2000;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * The J2KFile is the top level representation of a JP2 or JPX encoded
 * file. It contains boxes and may be read or written in the same was
 * as any {@link ContainerBox}
 *
 * @author http://bfo.com
 */
public class J2KFile {

  private static final long SIGMARKER = 0x6a5020200d0a870al;

  private HeaderBox jp2h;

  private FileTypeBox ftyp;

  private CodeStreamBox jp2c;

  private List<Box> boxes;

  public J2KFile() {
    boxes = new ArrayList<Box>();
  }

  /**
   * Add a Box to this JP2File
   * @param box the box
   * @return this
   */
  public J2KFile add(Box box) throws IOException {
    if (box instanceof FileTypeBox) {
      if (ftyp == null) {
        ftyp = (FileTypeBox) box;
      }
      else {
        throw new IOException("More than one ftyp box");
      }
    }
    else if (ftyp == null) {
      throw new IOException("No ftyp Box");
    }
    else if (box instanceof HeaderBox) {
      if (jp2h == null) {
        jp2h = (HeaderBox) box;
      }
      else {
        throw new IOException("More than one jp2h box");
      }
    }
    else if (box instanceof CodeStreamBox) {
      if (jp2h == null) {
        throw new IOException("jp2c must follow jp2h");
      }
      else if (jp2c == null) {
        jp2c = (CodeStreamBox) box;
      }
      else {
        // Others are allowed.
      }
    }
    boxes.add(box);
    return this;
  }

  /**
   * Return the full list of {@link Boxes} from the file.
   * The returned list is read-only
   */
  public List<Box> getBoxes() {
    return Collections.<Box> unmodifiableList(boxes);
  }

  /**
   * Return the {@link CodeStreamBox} from the file
   */
  public CodeStreamBox getCodeStreamBox() {
    return jp2c;
  }

  /**
   * Return the {@link HeaderBox} from the file
   */
  public HeaderBox getHeaderBox() {
    return jp2h;
  }

  /**
   * Read a J2KFile from the specified input
   */
  public J2KFile read(RandomAccessIO in) throws IOException {
    if (in.readInt() != 12 || in.readInt() != SIGMARKER >> 32 || in.readInt() != (int) SIGMARKER) {
      throw new IOException("No JP2 Signature Box");
    }
    while (in.length() - in.getPos() >= 8) {        // 8 is minimum length for box
      add(ContainerBox.readBox(in));
    }
    return this;
  }

  /**
   * Write the J2KFile to the specified OutputStream
   */
  public OutputStream write(OutputStream out) throws IOException {
    if (jp2h == null || jp2c == null) {
      throw new IOException("Missing jp2h or jp2c");
    }
    DataOutputStream o = new DataOutputStream(out);
    o.writeInt(12);
    o.writeLong(SIGMARKER);
    for (int i = 0; i < boxes.size(); i++) {
      ContainerBox.writeBox(boxes.get(i), o);
    }
    return out;
  }

  /**
   * Serialize the J2KFile as XML to the specified XMLStreamWriter
   */
  public XMLStreamWriter write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement("j2k");
    for (Box box : boxes) {
      box.write(out);
    }
    out.writeEndElement();
    return out;
  }

}
