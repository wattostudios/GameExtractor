
package com.github.jpeg2000;

/**
 * Interface which defines the parameters required to write a JP2 image.
 * Abstracted away from J2KImageReadParamJava
 *
 * @author http://bfo.com
 */
public interface J2KReadParam {

  /**
   * Specifies the decoding rate in bits per pixel (bpp) where the
   * number of pixels is related to the image's original size (Note:
   * this parameter is not affected by <code>resolution</code>).
   * A value of <code>Double.MAX_VALUE</code> means decode
   * with the encoding rate - this is a suitable default.
   */
  public double getDecodingRate();

  /**
   * If true, no ROI de-scaling is performed. Decompression is done
   * like there is no ROI in the image.
   * A suitable default is "true"
   */
  public boolean getNoROIDescaling();

  /**
   * Specifies the resolution level wanted for the decoded image
   * (0 means the lowest available resolution, the resolution
   * level gives an image with the original dimension).  If the given index
   * is greater than the number of available resolution levels of the
   * compressed image, the decoded image has the lowest available
   * resolution (among all tile-components).  This parameter affects only
   * the inverse wavelet transform and not the number of bytes read by the
   * codestream parser, which depends only on <code>decodingRate</code>.
   * A value of -1 means to use the resolution level at encoding.
   * This is a suitable default.
   */
  public int getResolution();

}
