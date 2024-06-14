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
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_POD_POD6;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_POD_POD6_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_POD_POD6_TEX() {
    super("POD_POD6_TEX", "Ghostbusters: The Video Game: Remastered TEX Image");
    setExtensions("tex");

    setGames("Ghostbusters: The Video Game: Remastered");
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
      if (plugin instanceof Plugin_POD_POD6) {
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

      // 4 - Unknown (7)
      if (fm.readInt() == 7) {
        rating += 5;
      }

      // 16 - Hash?
      // 4 - null
      // 4 - Image Format (72=DXT5 Swizzled)
      fm.skip(24);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Width/Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      // 4 - Unknown (8)
      fm.skip(4);

      // 4 - Number of Mipmaps [+1] (ie "2" means there are 3 mipmaps)
      if (FieldValidator.checkNumColors(fm.readInt() + 1, 256)) {
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

      // 4 - Unknown (7)
      // 16 - Hash?
      // 4 - null
      fm.skip(24);

      // 4 - Image Format
      int formatCode = fm.readInt();

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (8)
      fm.skip(4);

      // 4 - Number of Mipmaps [+1] (ie "2" means there are 3 mipmaps)
      int mipmapCount = fm.readInt() + 1;
      FieldValidator.checkNumColors(mipmapCount, 256);

      // 8 - null
      fm.skip(8);

      // X - Image Data
      ImageResource imageResource = null;
      if (formatCode == 3 || formatCode == 24) { // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (formatCode == 43) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (formatCode == 47) { // RG
        imageResource = ImageFormatReader.readRG(fm, width, height);
      }
      else if (formatCode == 50) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_POD_POD6_TEX] Unknown Image Format: " + formatCode);
      }

      fm.close();

      if (imageResource == null) {
        return null;
      }

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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
  public void write(PreviewPanel panel, FileManipulator destination) {
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

      // Extract the original resource into a byte[] array, so we can reference it
      byte[] srcBytes = new byte[(int) resourceBeingReplaced.getDecompressedLength()];
      FileManipulator src = new FileManipulator(new ByteBuffer(srcBytes));
      resourceBeingReplaced.extract(src);
      src.seek(0);

      // Build the new file using the src[] and adding in the new image content

      // 4 - Unknown (7)
      // 16 - Hash?
      // 4 - null
      fm.writeBytes(src.readBytes(24));

      // 4 - Image Format
      int formatCode = src.readInt();
      fm.writeInt(formatCode);

      // 4 - Image Width
      src.skip(4);
      fm.writeInt(width);

      // 4 - Image Height
      src.skip(4);
      fm.writeInt(height);

      // 4 - Unknown (8)
      fm.writeBytes(src.readBytes(4));

      // 4 - Number of Mipmaps [+1] (ie "2" means there are 3 mipmaps)
      int mipmapCount = src.readInt();
      fm.writeInt(mipmapCount);

      // 8 - null
      fm.writeBytes(src.readBytes(8));

      mipmapCount++;

      ImageResource[] mipmaps = new ImageManipulator(imageResource).generateMipmaps(mipmapCount);

      // X - Image Data
      for (int m = 0; m < mipmapCount; m++) {
        if (formatCode == 3 || formatCode == 24) { // RGBA
          ImageFormatWriter.writeRGBA(fm, mipmaps[m]);
        }
        else if (formatCode == 43) { // DXT1
          ImageFormatWriter.writeDXT1(fm, mipmaps[m]);
        }
        else if (formatCode == 47) { // RG
          ImageFormatWriter.writeRG(fm, mipmaps[m]);
        }
        else if (formatCode == 50) { // DXT5
          ImageFormatWriter.writeDXT5(fm, mipmaps[m]);
        }
        else {
          ErrorLogger.log("[Viewer_POD_POD6_TEX] Unknown Image Format: " + formatCode);
        }
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}