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

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageFormatWriter;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_SDU_SRSC;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SDU_SRSC_SDUTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_SDU_SRSC_SDUTEX() {
    super("SDU_SRSC_SDUTEX", "The Suffering SDU_TEX Image");
    setExtensions("sdu_tex");

    setGames("The Suffering");
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
  public boolean canReplace(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_SDU_SRSC) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (128/64)
      fm.skip(4);

      // 4 - Bits Per Pixel? (4=DXT1, 8=DXT3)
      int imageFormat = fm.readInt();

      // 4 - Unknown (0/4)
      // 4 - Unknown (-1)
      // 4 - Unknown (512/522)
      // 4 - Unknown
      // 4 - Unknown (3/2)
      // 4 - Unknown (2)
      // 4 - Unknown (1)
      // 4 - null
      fm.skip(32);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 4) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else if (imageFormat == 8) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_SDU_SRSC_SDUTEX] Unknown Image Format: " + imageFormat);
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

  /**
  **********************************************************************************************
  We can't WRITE these files from scratch, but we can REPLACE some of the images with new content  
  **********************************************************************************************
  **/
  public void replace(Resource resourceBeingReplaced, PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;
      Image image = ivp.getImage();
      int width = ivp.getImageWidth();
      int height = ivp.getImageHeight();

      if (width == -1 || height == -1) {
        return;
      }

      // Try to get the existing ImageResource (if it was stored), otherwise build a new one
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();
      if (imageResource == null) {
        imageResource = new ImageResource(image, width, height);
      }

      // Now we need to flip the image, as it's stored upside down in the game archive
      imageResource = ImageFormatReader.flipVertically(imageResource);

      // 4 - Image Width
      // 4 - Image Height
      // 4 - Unknown (128/64)
      // 4 - Bits Per Pixel? (4=DXT1, 8=DXT3)
      // 4 - Unknown (0/4)
      // 4 - Unknown (-1)
      // 4 - Unknown (512/522)
      // 4 - Unknown
      // 4 - Unknown (3/2)
      // 4 - Unknown (2)
      // 4 - Unknown (1)
      // 4 - null

      // Extract the original resource (FIRST 48 BYTES ONLY) into a byte[] array, so we can reference it
      byte[] srcBytes = new byte[48];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Image Width
      fm.writeInt(width);
      src.skip(4);

      // 4 - Image Height
      fm.writeInt(height);
      src.skip(4);

      // 4 - Unknown (128/64)
      fm.writeBytes(src.readBytes(4));

      // 4 - Bits Per Pixel? (4=DXT1, 8=DXT3)
      int bpp = src.readInt();
      fm.writeInt(bpp);

      // 4 - Unknown (0/4)
      // 4 - Unknown (-1)
      // 4 - Unknown (512/522)
      // 4 - Unknown
      // 4 - Unknown (3/2)
      // 4 - Unknown (2)
      // 4 - Unknown (1)
      // 4 - null
      fm.writeBytes(src.readBytes(32));

      // X - Pixels
      if (bpp == 4) {
        ImageFormatWriter.writeDXT1(fm, imageResource);
      }
      else {
        ImageFormatWriter.writeDXT5(fm, imageResource);
      }
      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}