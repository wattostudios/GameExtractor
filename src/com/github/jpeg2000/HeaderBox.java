
package com.github.jpeg2000;

/**
 * This represents the "jp2h" box
 *
 * @author http://bfo.com
 */
public class HeaderBox extends ContainerBox {

  private ImageHeaderBox ihdr;

  private BitsPerComponentBox bpcc;

  private PaletteBox pclr;

  private ComponentMappingBox cmap;

  private ChannelDefinitionBox cdef;

  private ResolutionSuperBox res;

  private ColorSpecificationBox colr; // the first one seen

  public HeaderBox() {
    super(fromString("jp2h"));
  }

  @Override
  public ContainerBox add(Box box) {
    if (box instanceof ImageHeaderBox) {
      if (ihdr == null) {
        ihdr = (ImageHeaderBox) box;
      }
      else {
        throw new IllegalStateException("More than one ihdr box");
      }
    }
    else {
      if (ihdr == null) {
        throw new IllegalStateException("ihdr box must come first");
      }
      if (box instanceof BitsPerComponentBox) {
        if (bpcc == null) {
          bpcc = (BitsPerComponentBox) box;
        }
        else {
          throw new IllegalStateException("More than one bpcc box");
        }
      }
      else if (box instanceof ColorSpecificationBox) {
        if (colr == null) {
          colr = (ColorSpecificationBox) box;
        }
        // More than one is allowed
      }
      else if (box instanceof PaletteBox) {
        if (pclr == null) {
          pclr = (PaletteBox) box;
        }
        else {
          throw new IllegalStateException("More than one pclr box");
        }
      }
      else if (box instanceof ComponentMappingBox) {
        if (cmap == null) {
          cmap = (ComponentMappingBox) box;
        }
        else {
          throw new IllegalStateException("More than one cmap box");
        }
      }
      else if (box instanceof ChannelDefinitionBox) {
        if (cdef == null) {
          cdef = (ChannelDefinitionBox) box;
        }
        else {
          throw new IllegalStateException("More than one cdef box");
        }
      }
      else if (box instanceof ResolutionSuperBox) {
        if (res == null) {
          res = (ResolutionSuperBox) box;
        }
        else {
          throw new IllegalStateException("More than one res box");
        }
      }
    }
    return super.add(box);
  }

  public BitsPerComponentBox getBitsPerComponentBox() {
    return bpcc;
  }

  public ChannelDefinitionBox getChannelDefinitionBox() {
    return cdef;
  }

  public ColorSpecificationBox getColorSpecificationBoxes() {
    return colr;
  }

  public ComponentMappingBox getComponentMappingBox() {
    return cmap;
  }

  public ImageHeaderBox getImageHeaderBox() {
    return ihdr;
  }

  public PaletteBox getPaletteBox() {
    return pclr;
  }

  public ResolutionSuperBox getResolutionSuperBox() {
    return res;
  }
}
