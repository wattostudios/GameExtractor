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

package org.watto.ge.helper;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import javax.swing.JLabel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;

/**
**********************************************************************************************
Given an image, it can manipulate is such as converting from true-color to paletted, reducing
the number of colors, etc.
**********************************************************************************************
**/

public class ImageManipulator {

  int[] pixels = new int[0];

  Palette palette = new Palette();

  int width = 0;

  int height = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageManipulator(ImageResource imageResource) {
    this.pixels = imageResource.getImagePixels();
    this.width = imageResource.getWidth();
    this.height = imageResource.getHeight();
  }

  /**
  **********************************************************************************************
  Palette to be set later
  **********************************************************************************************
  **/
  public ImageManipulator(int[] pixels, int width, int height) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageManipulator(int[] pixels, int width, int height, int[] palette) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
    setPalette(new Palette(palette));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageManipulator(int[] pixels, int width, int height, Palette palette) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
    setPalette(palette);
  }

  /**
  **********************************************************************************************
  Gets the image from the PreviewPanel_3DModel
  **********************************************************************************************
  **/
  public ImageManipulator(PreviewPanel_3DModel previewPanel) {

    this.width = previewPanel.getImageWidth();
    this.height = previewPanel.getImageHeight();

    BufferedImage bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics g = bufImage.getGraphics();
    g.drawImage(previewPanel.getImage(), 0, 0, null);

    pixels = bufImage.getRGB(0, 0, width, height, null, 0, width);

  }

  /**
  **********************************************************************************************
  Gets the image from the PreviewPanel_Image
  **********************************************************************************************
  **/
  public ImageManipulator(PreviewPanel_Image previewPanel) {

    ImageResource imageResource = previewPanel.getImageResource();
    if (imageResource != null) {
      this.pixels = imageResource.getImagePixels();
      this.width = imageResource.getWidth();
      this.height = imageResource.getHeight();
    }
    else {
      this.width = previewPanel.getImageWidth();
      this.height = previewPanel.getImageHeight();

      BufferedImage bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics g = bufImage.getGraphics();
      g.drawImage(previewPanel.getImage(), 0, 0, null);

      pixels = bufImage.getRGB(0, 0, width, height, null, 0, width);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeColorCount(int numColors) {
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }
    ColorConverter.changeColorCount(this, numColors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void convertToPaletted() {
    if (palette == null || palette.getNumColors() <= 0) {
      ColorConverter.convertToPaletted(this);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource[] generateMipmaps() {

    int[] mipmapPixels = getImagePixels();
    int mipmapWidth = getWidth();
    int mipmapHeight = getHeight();

    // determine the number of mipmaps
    int mipmapCount = 0;
    while (mipmapWidth > 0 && mipmapHeight > 0) {
      mipmapCount++;

      mipmapWidth /= 2;
      mipmapHeight /= 2;
    }

    // reset the width and height
    mipmapWidth = getWidth();
    mipmapHeight = getHeight();

    ImageResource[] imageResources = new ImageResource[mipmapCount];
    for (int i = 0; i < mipmapCount; i++) {
      // store a mipmap for the existing size
      imageResources[i] = new ImageResource(mipmapPixels, mipmapWidth, mipmapHeight);

      // shrink the image ready for the next iteration of the loop
      int newWidth = mipmapWidth / 2;
      int newHeight = mipmapHeight / 2;

      if (newWidth <= 0 || newHeight <= 0) {
        // end the loop - we should already have stored the last mipmap above, and we don't want to try generating a mipmap for size 0
        break;
      }

      int[] newPixels = new int[newWidth * newHeight];
      for (int h = 0, j = 0; h + 1 < mipmapHeight; h += 2, j++) {
        for (int w = 0, x = 0; w + 1 < mipmapWidth; w += 2, x++) {
          // if the width or height are not a multiple of 2, we need to combat this here by stripping off a row or column
          // (thus the h+1/w+1 in the "while" part of the "for" loops)

          // average of the 4 pixels
          int rowStart1 = (h * mipmapWidth) + w;
          int rowStart2 = ((h + 1) * mipmapWidth) + w;
          newPixels[j * newWidth + x] = ColorConverter.getAverage(mipmapPixels[rowStart1], mipmapPixels[rowStart1 + 1], mipmapPixels[rowStart2], mipmapPixels[rowStart2 + 1]);
        }
      }

      mipmapPixels = newPixels;
      mipmapWidth = newWidth;
      mipmapHeight = newHeight;
    }

    return imageResources;

  }

  /**
  **********************************************************************************************
  Generates Mipmaps for paletted images. Each mipmap shares the same color palette
  **********************************************************************************************
  **/
  public ImageManipulator[] generatePalettedMipmaps() {

    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }

    int[] mipmapPixels = getPixels();
    int mipmapWidth = getWidth();
    int mipmapHeight = getHeight();

    // determine the number of mipmaps
    int mipmapCount = 0;
    while (mipmapWidth > 0 && mipmapHeight > 0) {
      mipmapCount++;

      mipmapWidth /= 2;
      mipmapHeight /= 2;
    }

    // reset the width and height
    mipmapWidth = getWidth();
    mipmapHeight = getHeight();

    int[] paletteColors = palette.getPalette();

    ImageManipulator[] imageResources = new ImageManipulator[mipmapCount];
    for (int i = 0; i < mipmapCount; i++) {
      // store a mipmap for the existing size
      imageResources[i] = new ImageManipulator(mipmapPixels, mipmapWidth, mipmapHeight, palette);

      // shrink the image ready for the next iteration of the loop
      int newWidth = mipmapWidth / 2;
      int newHeight = mipmapHeight / 2;

      if (newWidth <= 0 || newHeight <= 0) {
        // end the loop - we should already have stored the last mipmap above, and we don't want to try generating a mipmap for size 0
        break;
      }

      int[] newPixels = new int[newWidth * newHeight];
      for (int h = 0, j = 0; h + 1 < mipmapHeight; h += 2, j++) {
        for (int w = 0, x = 0; w + 1 < mipmapWidth; w += 2, x++) {
          // if the width or height are not a multiple of 2, we need to combat this here by stripping off a row or column
          // (thus the h+1/w+1 in the "while" part of the "for" loops)

          // average of the 4 pixels
          int rowStart1 = (h * mipmapWidth) + w;
          int rowStart2 = ((h + 1) * mipmapWidth) + w;
          // This averages the COLORS of the 4 pixels - need to re-map these colors to palette colors down further
          newPixels[j * newWidth + x] = ColorConverter.getAverage(paletteColors[mipmapPixels[rowStart1]], paletteColors[mipmapPixels[rowStart1 + 1]], paletteColors[mipmapPixels[rowStart2]], paletteColors[mipmapPixels[rowStart2 + 1]]);
        }
      }

      // Now we need to compare all the pixels to the color palette, and choose the closest matching color for each pixel.
      // (ie. because this new mipmap needs to have the same color palette as the original image)
      ImageManipulator mipmapIM = new ImageManipulator(newPixels, newWidth, newHeight);
      mipmapIM.convertToPaletted();
      ColorConverter.changePaletteMatch(mipmapIM, palette);

      mipmapPixels = mipmapIM.getPixels();
      mipmapWidth = newWidth;
      mipmapHeight = newHeight;
    }

    return imageResources;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getHeight() {
    return height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image getImage() {
    ColorModel model = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
    return new JLabel().createImage(new MemoryImageSource(width, height, model, getImagePixels(), 0, width));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] getImagePixels() {
    if (palette == null || palette.getNumColors() == 0) {
      // not a paletted image - pixels are already ARGB
      return pixels;
    }

    // Convert the paletted image to ARGB
    int[] palette = this.palette.getPalette();

    int numPixels = pixels.length;
    int[] rgb = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      rgb[i] = palette[pixels[i]];
    }
    return rgb;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getNumColors() {
    return palette.getNumColors();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getNumPixels() {
    return pixels.length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] getPalette() {
    return palette.getPalette();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Palette getPaletteObject() {
    return palette;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] getPixels() {
    return pixels;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getWidth() {
    return width;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setHeight(int height) {
    this.height = height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPalette(int[] palette) {
    if (this.palette.getNumColors() == 0) {
      // needs the palette to be added to PaletteManager
      setPalette(new Palette(palette));
    }
    else {
      // just update the current palette
      this.palette.setPalette(palette);

    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPalette(Palette palette) {
    this.palette = palette;
    PaletteManager.addPalette(palette);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPixels(int[] pixels) {
    this.pixels = pixels;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWidth(int width) {
    this.width = width;
  }

}