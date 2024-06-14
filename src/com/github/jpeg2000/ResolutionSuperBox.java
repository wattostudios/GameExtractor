
package com.github.jpeg2000;

/**
 * Represents the "res" resolution superbox
 */
public class ResolutionSuperBox extends ContainerBox {

  public ResolutionSuperBox() {
    super(fromString("res "));
  }

  public ResolutionSuperBox(ResolutionBox box) {
    this();
    add(box);
  }

}
