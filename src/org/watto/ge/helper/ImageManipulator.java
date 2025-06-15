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
  This changes the order of the colors in the color palette. Basically, palette[] and newPaletteOrder[]
  should have the same values in it, but in a different order, so this method will work out the
  mapping and change the pixel values to match this new order
  **********************************************************************************************
  **/
  public void swapPaletteOrder(int[] newPaletteOrder) {
    // Just in case
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }

    // work out the new mappings
    int numColors = newPaletteOrder.length;
    int[] mapping = new int[numColors];

    int[] oldPalette = palette.getPalette();
    int oldNumColors = oldPalette.length;

    for (int i = 0; i < numColors; i++) {
      // assume colors are mostly unchanged in the palettes (also works as a fallback if the color can't be found)
      mapping[i] = i;

      int currentColor = newPaletteOrder[i];
      try {
        if (oldPalette[i] == currentColor) {
          // matches
          continue;
        }
      }
      catch (Throwable t) {
        continue; // also helps in case oldPalette is too small, even though they should match in length if people are nice 
      }

      // find the matching color
      for (int o = 0; o < oldNumColors; o++) {
        if (currentColor == oldPalette[o]) {
          // match
          mapping[i] = o;
          break;
        }
      }
    }

    // change the pixels to match the new palette order
    int numPixels = pixels.length;
    for (int i = 0; i < numPixels; i++) {
      pixels[i] = mapping[pixels[i]];
    }

    // finally, set the new palette
    palette = new Palette(newPaletteOrder);
  }

  /**
  **********************************************************************************************
  You have an image with a palette. You're applying a completely different palette, and you need
  to map all the colors from the existing palette into the new one, by best match. This is what
  this method does. Used, for example, if you have a global color palette and you need to force
  this image to conform to that palette, by closest color match.
  **********************************************************************************************
  **/
  public void swapAndConvertPalette(int[] newPalette) {
    // Just in case
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }

    /*
    
    // work out the new mappings
    int[] oldPalette = palette.getPalette();
    int oldNumColors = oldPalette.length;
    int[] mapping = new int[oldNumColors];
    
    int newNumColors = newPalette.length;
    
    // Convert both palettes into colors, for quick comparison
    ColorSplitAlpha[] oldColorPalette = new ColorSplitAlpha[oldNumColors];
    ColorSplitAlpha[] newColorPalette = new ColorSplitAlpha[newNumColors];
    
    for (int i=0;i<oldNumColors;i++) {
      oldColorPalette[i] = new ColorSplitAlpha(oldPalette[i]);
    }
    for (int j=0;j<newNumColors;j++) {
      newColorPalette[j] = new ColorSplitAlpha(newPalette[j]);
    }
    
    // for every color in the old palette, find the closest match in the new palette
    for (int i = 0; i < oldNumColors; i++) {
      ColorSplitAlpha oldColor = oldColorPalette[i];
      
      // Build an array
      for (int j=0;j<newNumColors;j++) {
        ColorSplitAlpha newColor = newColorPalette[i];
        
        // DO SOMETHING HERE - NOT FINISHED
      }
      
      // sort the array
      
      // choose the best match
    }
    
    
    
    // change the pixels to match the new palette
    int numPixels = pixels.length;
    for (int i = 0; i < numPixels; i++) {
      pixels[i] = mapping[pixels[i]];
    }
    
    // finally, set the new palette
    palette = new Palette(newPalette);
    */

    ColorConverter.changePaletteMatch(this, new Palette(newPalette));
  }

  /**
  **********************************************************************************************
  For an image that has a range of alphas, this will change it to only having a single alpha (either on or off)
  **********************************************************************************************
  **/
  public void changeToSingleAlpha(int percentCloseness) {

    int threshold = (int) ((((float) percentCloseness) / 100) * 255);

    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }

    // convert the palette
    int[] paletteArray = palette.getPalette();
    int numColors = paletteArray.length;

    for (int p = 0; p < numColors; p++) {
      ColorSplitAlpha color = new ColorSplitAlpha(paletteArray[p]);
      if (color.getAlpha() < threshold) {
        color.setAlpha(0);
      }
      else {
        color.setAlpha(255);
      }
      paletteArray[p] = color.getColor();
    }

    palette.setPalette(paletteArray);

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

    // After color reduction, if the palette is too small, increase it to the right size
    int paletteColors = palette.getNumColors();
    if (paletteColors < numColors) {
      palette.resizePalette(numColors);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeColorCountRGBSingleAlpha(int numColors) {
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }
    ColorConverter.changeColorCountRGBSingleAlpha(this, numColors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeColorCountRGB(int numColors) {
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }
    ColorConverter.changeColorCountRGB(this, numColors);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeColorCountRGBKeepingExistingAlpha(int numColors) {
    if (palette == null || palette.getNumColors() <= 0) {
      convertToPaletted();
    }
    ColorConverter.changeColorCountRGBKeepingExistingAlpha(this, numColors);
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
  If the palette has less than <i>numColors</i>, it will be resized, filled with empty colors
  **********************************************************************************************
  **/
  public void resizePalette(int numColors) {
    if (palette == null || palette.getNumColors() <= 0) {
      ColorConverter.convertToPaletted(this);
    }

    if (palette.getNumColors() < numColors) {
      palette.resizePalette(numColors);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource[] generateMipmaps() {

    int mipmapWidth = getWidth();
    int mipmapHeight = getHeight();

    // determine the number of mipmaps
    int mipmapCount = 0;
    while (mipmapWidth > 0 && mipmapHeight > 0) {
      mipmapCount++;

      mipmapWidth /= 2;
      mipmapHeight /= 2;
    }

    return generateMipmaps(mipmapCount);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource[] generateMipmaps(int mipmapCount) {

    int[] mipmapPixels = getImagePixels();
    int mipmapWidth = getWidth();
    int mipmapHeight = getHeight();

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
  public void setImagePixels(int[] pixels) {
    this.pixels = pixels;
    this.palette = null;
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
  get the pixel indexes, but instead of returning int[], it returns byte[]
  **********************************************************************************************
  **/
  public byte[] getPixelBytes() {

    int numPixels = pixels.length;
    byte[] pixelBytes = new byte[numPixels];

    for (int i = 0; i < numPixels; i++) {
      pixelBytes[i] = (byte) pixels[i];
    }

    return pixelBytes;
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
    if (this.palette == null || this.palette.getNumColors() == 0) {
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