
package com.github.jpeg2000;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import jj2000.j2k.io.RandomAccessIO;

/**
 * This represents the "colr" box.
 * Its content contains the method to define the color space,
 * the precedence and approximation accuracy (0 for JP2 files), the
 * enumerated color space, and the ICC color profile if any.
 *
 * @author http://bfo.com
 */
public class ColorSpecificationBox extends Box {

  /** The enumerated color space defined in JP2 file format. */
  public static final int ECS_sRGB = 16;

  public static final int ECS_GRAY = 17;

  public static final int ECS_YCC = 18;

  private byte method;

  private byte precedence;

  private byte approximation;

  private int ecs;

  private volatile byte[] profileData;

  private volatile ICC_Profile profile;

  public ColorSpecificationBox() {
    super(fromString("colr"));
    method = 1;
    approximation = 1;
  }

  public ColorSpecificationBox(ColorSpace cs) {
    this();
    if (cs.isCS_sRGB()) {
      ecs = 16;
    }
    else if (cs.getNumComponents() == 1) {
      ecs = 17;
    }
    else if (cs.getNumComponents() == 4) {
      ecs = 12;
    }
    else if (cs instanceof ICC_ColorSpace) {
      method = 2;
      profile = ((ICC_ColorSpace) cs).getProfile();
    }
  }

  public ColorSpecificationBox(ICC_Profile profile) {
    this(2, 0, 1, 0, profile);
  }

  public ColorSpecificationBox(int ecs) {
    this(1, 0, 1, ecs, null);
  }

  /** 
   *  Creates a <code>ColorSpecificationBox</code> from the provided data
   *  elements.
   */
  public ColorSpecificationBox(int m, int p, int a, int ecs, ICC_Profile profile) {
    this();
    this.method = (byte) m;
    this.precedence = (byte) p;
    this.approximation = (byte) a;
    this.ecs = ecs;
    this.profile = profile;
  }

  /** Returns <code>ApproximationAccuracy</code>. */
  public byte getApproximationAccuracy() {
    return approximation;
  }

  /** Returns the enumerated color space. */
  public int getEnumeratedColorSpace() {
    return ecs;
  }

  /**
   * Returns the ICC color profile in this color specification box,
   * or null none exists.
   */
  public ICC_Profile getICCProfile() {
    if (profile == null && profileData != null) {
      synchronized (this) {
        if (profile == null && profileData != null) {
          profile = ICC_Profile.getInstance(profileData);
        }
      }
    }
    return profile;
  }

  /**
   * Returns the raw bytes of the ICC color profile in this color
   * specification box, or null if none exists..
   */
  public byte[] getICCProfileData() {
    // Reason for this method is to allows access to the ICC profile
    // data without instantiation of the ICC_Profile object.
    if (profileData == null && profile != null) {
      synchronized (this) {
        if (profileData == null && profile != null) {
          profileData = profile.getData();
        }
      }
    }
    return profileData;
  }

  @Override
  public int getLength() {
    return 3 + (method == 1 ? 4 : getICCProfileData().length);
  }

  /** Returns the method to define the color space. */
  public byte getMethod() {
    return method;
  }

  /** Returns <code>Precedence</code>. */
  public byte getPrecedence() {
    return precedence;
  }

  @Override
  public void read(RandomAccessIO in) throws IOException {
    method = in.readByte();
    precedence = in.readByte();
    approximation = in.readByte();
    if (method == 2 || method == 3) {
      profileData = new byte[in.length() - in.getPos()];
      in.readFully(profileData, 0, profileData.length);
    }
    else {
      ecs = in.readInt();
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.deleteCharAt(sb.length() - 1);
    sb.append(",\"method\":");
    sb.append(method);
    sb.append(",\"precedence\":");
    sb.append(precedence);
    sb.append(",\"approximation\":");
    sb.append(approximation);
    sb.append(",\"ecs\":");
    sb.append(ecs);
    sb.append("}");
    return sb.toString();
  }

  @Override
  public void write(DataOutputStream out) throws IOException {
    out.write(method);
    out.write(precedence);
    out.write(approximation);
    if (method == 1) {
      out.writeInt(ecs);
    }
    else if (getICCProfileData() != null) {
      out.write(getICCProfileData());
    }
  }

  @Override
  public void write(XMLStreamWriter out) throws XMLStreamException {
    out.writeStartElement(toString(getType()).trim());
    out.writeAttribute("length", Integer.toString(getLength()));
    out.writeAttribute("method", Integer.toString(getMethod()));
    out.writeAttribute("precedence", Integer.toString(getPrecedence()));
    out.writeAttribute("approximation", Integer.toString(getApproximationAccuracy()));
    if (getMethod() == 1) {
      out.writeStartElement("enumcs");
      out.writeCharacters(Integer.toString(getEnumeratedColorSpace()));
      out.writeEndElement();
    }
    else if (getICCProfile() != null) {
      out.writeStartElement("profile");
      out.writeCharacters(toString(getICCProfileData()));
      out.writeEndElement();
    }
    out.writeEndElement();
  }

}
