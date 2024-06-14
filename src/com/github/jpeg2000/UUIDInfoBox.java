
package com.github.jpeg2000;

/**
 * Represents the "uinf" box
 *
 * @author http://bfo.com
 */
public class UUIDInfoBox extends ContainerBox {

  private UUIDListBox ulst;

  private URLBox url;

  public UUIDInfoBox() {
    super(fromString("uinf"));
  }

  public UUIDInfoBox(UUIDListBox ulst, URLBox url) {
    this();
    add(ulst);
    add(url);
  }

  @Override
  public ContainerBox add(Box box) {
    if (box instanceof UUIDListBox) {
      if (ulst == null) {
        ulst = (UUIDListBox) box;
      }
      else {
        throw new IllegalStateException("More than one ulst box");
      }
    }
    else if (box instanceof URLBox) {
      if (ulst == null) {
        throw new IllegalStateException("ulst box must come before url box");
      }
      else if (url == null) {
        url = (URLBox) box;
      }
      else {
        throw new IllegalStateException("More than one url box");
      }
    }
    return super.add(box);
  }

  public URLBox getURLBox() {
    return url;
  }

  public UUIDListBox getUUIDListBox() {
    return ulst;
  }
}
