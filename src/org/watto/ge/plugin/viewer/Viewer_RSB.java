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

package org.watto.ge.plugin.viewer;

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RSB extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RSB() {
    super("RSB", "Ghost Recon RSB Image");
    setExtensions("rsb");

    setGames("Ghost Recon");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(4);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Number Of Bits Used For Red
      int red = fm.readInt();
      if (red >= 4 && red <= 8) {
        rating += 5;
      }

      // 4 - Number Of Bits Used For Green
      int green = fm.readInt();
      if (green >= 4 && green <= 8) {
        rating += 5;
      }

      // 4 - Number Of Bits Used For Blue
      int blue = fm.readInt();
      if (blue >= 4 && blue <= 8) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // 4 - Unknown (2/4/5/6)
      fm.skip(4);

      // 4 - Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Number Of Bits Used For Red
      int numRed = fm.readInt();
      FieldValidator.checkLength(numRed, 10);

      // 4 - Number Of Bits Used For Green
      int numGreen = fm.readInt();
      FieldValidator.checkLength(numGreen, 10);

      // 4 - Number Of Bits Used For Blue
      int numBlue = fm.readInt();
      FieldValidator.checkLength(numBlue, 10);

      // 4 - Number Of Bits Used For Alpha
      int numAlpha = fm.readInt();
      FieldValidator.checkLength(numAlpha, 10);

      // X - Pixels
      ImageResource imageResource = null;
      if (numRed == 8 && numGreen == 8 && numBlue == 8 && numAlpha == 8) {
        // 8-8-8-8
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResource.addProperty("ImageFormat", "BGRA");
      }
      else if (numRed == 4 && numGreen == 4 && numBlue == 4 && numAlpha == 4) {
        // 4-4-4-4
        imageResource = ImageFormatReader.readBGRA4444(fm, width, height);
        imageResource.addProperty("ImageFormat", "BGRA4444");
      }
      else if (numRed == 5 && numGreen == 6 && numBlue == 5 && numAlpha == 0) {
        // 5-6-5
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else {
        //System.out.println("Unknown format: " + numRed + "," + numGreen + "," + numBlue + "," + numAlpha);
      }

      fm.close();

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}