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
import javax.swing.ImageIcon;
import org.watto.ge.helper.FileTypeDetector;

public class BlankImageResource extends ImageResource {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public BlankImageResource(Resource resource) {
    super(resource);
    this.resource = resource;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    return new BlankImageResource((Resource) resource.clone());
  }

  /**
  **********************************************************************************************
  Copies all the values from <i>resource</i> into this resource (ie does a replace without
  affecting pointers)
  **********************************************************************************************
  **/
  @Override
  public void copyFrom(ImageResource imageResource) {
    this.resource = imageResource.getResource();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getHeight() {
    return 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Image getImage() {
    return getThumbnail();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int[] getImagePixels() {
    return new int[0];
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getNumPixels() {
    return 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int[] getPixels() {
    return new int[0];
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Image getThumbnail() {
    FileType fileType = FileTypeDetector.getFileType(resource.getExtension());
    if (fileType == null) {
      return new ImageIcon("images/FileTypes/Other.png").getImage();
    }
    else {
      return fileType.getImage();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getThumbnailSize() {
    return 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getWidth() {
    return 0;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setHeight(int height) {
    // do nothing
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setPixels(int[] pixels) {
    // do nothing
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setWidth(int width) {
    // do nothing
  }

}