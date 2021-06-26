/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.datatype;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class PalettedImageResource extends ImageResource {

  int[] palette = new int[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] getPalette() {
    return palette;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPalette(int[] palette) {
    this.palette = palette;
    setImageShrunk(false); // to force regeneration of the thumbnail
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PalettedImageResource(int[] pixels, int width, int height, int[] palette) {
    super(pixels, width, height);
    this.palette = palette;
  }

  /**
  **********************************************************************************************
  For BlankImageResource Only!
  **********************************************************************************************
  **/
  public PalettedImageResource(Resource resource) {
    super(resource);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PalettedImageResource(Resource resource, int[] pixels, int width, int height, int[] palette) {
    super(resource, pixels, width, height);
    this.palette = palette;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    PalettedImageResource imageResource = new PalettedImageResource((Resource) resource.clone(), pixels.clone(), width, height, palette);
    imageResource.setImageShrunk(imageShrunk);
    imageResource.setProperties(properties);
    return imageResource;
  }

  /**
  **********************************************************************************************
  Copies all the values from <i>resource</i> into this resource (ie does a replace without
  affecting pointers)
  **********************************************************************************************
  **/
  public void copyFrom(PalettedImageResource imageResource) {
    super.copyFrom(imageResource);
    this.palette = imageResource.getPalette();
  }

  /**
  **********************************************************************************************
  Merges the palette into the pixels, generating the image
  **********************************************************************************************
  **/
  public int[] getImagePixels() {
    if (imageShrunk) {
      // shrunk image - the pixel[] is actually merged already
      return super.getImagePixels();
    }

    int numPixels = pixels.length;
    int numColors = palette.length;

    int[] mergedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];
      if (pixel < 0 || pixel > numColors) {
        mergedPixels[i] = 0;
      }
      else {
        mergedPixels[i] = palette[pixel];
      }
    }

    return mergedPixels;
  }

}