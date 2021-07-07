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

import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import javax.swing.JLabel;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.converter.ByteConverter;

public class ImageResource {

  int[] pixels = new int[0];

  int width = 0;

  int height = 0;

  int thumbnailSize = 100; // default size

  /** Have the pixels been shrunk to the size of a thumbnail, or are they still the full image? **/
  boolean imageShrunk = false;

  /** the Resource this image belongs to **/
  Resource resource = null;

  /** Any additional properties, useful mostly for writing images **/
  Resource_Property[] properties = null;

  ImageResource nextFrame = null;

  public ImageResource getPreviousFrame() {
    return previousFrame;
  }

  public void setPreviousFrame(ImageResource previousFrame) {
    this.previousFrame = previousFrame;
  }

  ImageResource previousFrame = null; // not used for animations, but used for manual changes (eg image collections, where the user can go forward or backward)

  public boolean isManualFrameTransition() {
    return manualFrameTransition;
  }

  public void setManualFrameTransition(boolean manualFrameTransition) {
    this.manualFrameTransition = manualFrameTransition;
  }

  boolean manualFrameTransition = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource(int[] pixels, int width, int height) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource(Image image, int width, int height) {

    this.width = width;
    this.height = height;

    try {
      PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, true);
      pixelGrabber.grabPixels();

      // get the pixels, and convert them to positive values in an int[] array
      try {
        pixels = (int[]) pixelGrabber.getPixels();
      }
      catch (ClassCastException e) {
        byte[] pixelBytes = (byte[]) pixelGrabber.getPixels();

        int numPixels = pixelBytes.length;
        pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(pixelBytes[i]);
        }
      }
    }
    catch (Throwable t) {
      this.width = 0;
      this.height = 0;
      this.pixels = new int[0];
    }

  }

  /**
  **********************************************************************************************
  For BlankImageResource Only!
  **********************************************************************************************
  **/
  public ImageResource(Resource resource) {
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource(Resource resource, int[] pixels, int width, int height) {
    this.resource = resource;
    this.pixels = pixels;
    this.width = width;
    this.height = height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, String value) {
    Resource_Property property = new Resource_Property(code, value);

    if (properties == null) {
      // add property to new array
      properties = new Resource_Property[] { property };
      return;
    }

    int numProperties = properties.length;

    // expand array then add property
    Resource_Property[] temp = properties;
    properties = new Resource_Property[numProperties + 1];
    System.arraycopy(temp, 0, properties, 0, numProperties);
    properties[numProperties] = property;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    ImageResource imageResource = new ImageResource((Resource) resource.clone(), pixels.clone(), width, height);
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
  public void copyFrom(ImageResource imageResource) {

    this.resource = imageResource.getResource();
    this.pixels = imageResource.getPixels();
    this.width = imageResource.getWidth();
    this.height = imageResource.getHeight();
    this.thumbnailSize = imageResource.getThumbnailSize();
    this.imageShrunk = imageResource.isImageShrunk();
    this.properties = imageResource.getProperties();

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
    return pixels;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource getNextFrame() {
    return nextFrame;
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
  public int[] getPixels() {
    return pixels;
  }

  public Resource_Property[] getProperties() {
    return properties;
  }

  /**
  **********************************************************************************************
  Gets the property. If the property isn't found, tries to get the property from the Resource.
  **********************************************************************************************
  **/
  public String getProperty(String code) {
    if (properties == null) {
      if (resource != null) {
        return resource.getProperty(code);
      }
      return "";
    }

    int numProperties = properties.length;

    for (int i = 0; i < numProperties; i++) {
      if (properties[i].getCode().equals(code)) {
        // found
        return properties[i].getValue();
      }
    }

    // not found here, try the Resource instead
    if (resource != null) {
      return resource.getProperty(code);
    }

    return "";
  }

  /**
  **********************************************************************************************
  Gets the property, if it exists, otherwise returns the <i>valueIfNotFound</i>
  **********************************************************************************************
  **/
  public boolean getProperty(String code, boolean valueIfNotFound) {
    String value = getProperty(code);
    if (value == null || value.equals("")) {
      return valueIfNotFound;
    }

    if (value.equalsIgnoreCase("true")) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  Gets the property, if it exists, otherwise returns the <i>valueIfNotFound</i>
  **********************************************************************************************
  **/
  public int getProperty(String code, int valueIfNotFound) {
    String value = getProperty(code);
    if (value == null || value.equals("")) {
      return valueIfNotFound;
    }

    try {
      return Integer.parseInt(value);
    }
    catch (Throwable t) {
      return valueIfNotFound;
    }
  }

  /**
  **********************************************************************************************
  Gets the property, if it exists, otherwise returns the <i>valueIfNotFound</i>
  **********************************************************************************************
  **/
  public String getProperty(String code, String valueIfNotFound) {
    String value = getProperty(code);
    if (value == null || value.equals("")) {
      return valueIfNotFound;
    }

    return value;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource() {
    return resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image getThumbnail() {
    if (!imageShrunk) {
      shrinkToThumbnail();
    }

    return getImage();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getThumbnailSize() {
    return thumbnailSize;
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
  public boolean isAnimation() {
    return (nextFrame != null && manualFrameTransition == false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isImageShrunk() {
    return imageShrunk;
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
  public void setImageShrunk(boolean imageShrunk) {
    this.imageShrunk = imageShrunk;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setNextFrame(ImageResource nextFrame) {
    this.nextFrame = nextFrame;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setPixels(int[] pixels) {
    this.pixels = pixels;
  }

  public void setProperties(Resource_Property[] properties) {
    this.properties = properties;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setProperty(String code, String value) {
    Resource_Property property = new Resource_Property(code, value);

    if (properties == null) {
      // add property to new array
      properties = new Resource_Property[] { property };
      return;
    }

    int numProperties = properties.length;

    for (int i = 0; i < numProperties; i++) {
      if (properties[i].getCode().equals(code)) {
        // found, so replace
        properties[i] = property;
        return;
      }
    }

    // expand array then add property
    Resource_Property[] temp = properties;
    properties = new Resource_Property[numProperties + 1];
    System.arraycopy(temp, 0, properties, 0, numProperties);
    properties[numProperties] = property;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setResource(Resource resource) {
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWidth(int width) {
    this.width = width;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void shrinkToThumbnail() {
    if (pixels.length <= 0) {
      return;
    }

    Image image = getImage();
    int thumbWidth = image.getWidth(null);
    int thumbHeight = image.getHeight(null);
    Image thumbnail = null;

    boolean highQualityThumbnails = Settings.getBoolean("HighQualityThumbnails");

    if (thumbWidth <= 0 || thumbHeight <= 0) {
      // the getWidth()/getHeight() doesn't work yet, so guess a resize and check

      if (!highQualityThumbnails) {
        thumbnail = image.getScaledInstance(-1, thumbnailSize, Image.SCALE_FAST);
      }
      else {
        thumbnail = image.getScaledInstance(-1, thumbnailSize, Image.SCALE_SMOOTH);
      }

      thumbWidth = thumbnail.getWidth(null);
      thumbHeight = thumbnailSize;
      if (thumbWidth > thumbHeight) {
        // guessed height was larger, but the width is larger, so regenerate
        if (!highQualityThumbnails) {
          thumbnail = image.getScaledInstance(thumbnailSize, -1, Image.SCALE_FAST);
        }
        else {
          thumbnail = image.getScaledInstance(thumbnailSize, -1, Image.SCALE_SMOOTH);
        }
      }
    }
    else {

      boolean retainSize = false;
      if (thumbWidth <= thumbnailSize && thumbHeight <= thumbnailSize) {
        // the image is smaller than the thumbnail size - see if we want to return the real image
        // instead of upscaling it to be larger
        if (!Settings.getBoolean("UpscaleSmallThumbnails")) {
          // return the real image (not upscaled)
          thumbnail = image;
          retainSize = true;
        }

      }

      if (!retainSize) {
        if (thumbWidth > thumbHeight) {
          if (!highQualityThumbnails) {
            thumbnail = image.getScaledInstance(thumbnailSize, -1, Image.SCALE_FAST);
          }
          else {
            thumbnail = image.getScaledInstance(thumbnailSize, -1, Image.SCALE_SMOOTH);
          }
        }
        else {
          if (!highQualityThumbnails) {
            thumbnail = image.getScaledInstance(-1, thumbnailSize, Image.SCALE_FAST);
          }
          else {
            thumbnail = image.getScaledInstance(-1, thumbnailSize, Image.SCALE_SMOOTH);
          }
        }
      }
    }

    if (thumbnail == null) {
      return;
    }

    thumbWidth = thumbnail.getWidth(null);
    thumbHeight = thumbnail.getHeight(null);

    // now that we have generated a thumbnail, remove the pixel data to free up memory
    try {
      PixelGrabber pixelGrabber = new PixelGrabber(thumbnail, 0, 0, thumbWidth, thumbHeight, false);
      pixelGrabber.grabPixels();

      // get the pixels, and convert them to positive values in an int[] array
      this.pixels = (int[]) pixelGrabber.getPixels();
      this.width = thumbWidth;
      this.height = thumbHeight;

      imageShrunk = true;
    }
    catch (Throwable t) {
      // Couldn't get the pixels for some reason
      ErrorLogger.log(t);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String toString() {
    return resource.toString();
  }

}