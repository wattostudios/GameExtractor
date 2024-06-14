/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_WLD_WRLD;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WLD_WRLD_TEXP extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WLD_WRLD_TEXP() {
    super("WLD_WRLD_TEXP", "WLD_WRLD_TEXP Image");
    setExtensions("texp");

    setGames("The Sting!");
    setPlatforms("PC");
    setStandardFileFormat(false);
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
      if (plugin instanceof Plugin_WLD_WRLD) {
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

      // 4 - Unknown (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Number of Sprites
      if (FieldValidator.checkRange(fm.readInt(), 1, 100)) { // guess
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

      // 4 - Unknown (2) (LITTLE ENDIAN)
      fm.skip(4);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (1) (LITTLE ENDIAN)
      fm.skip(4);

      // 4 - Number of Sprites within this Image (LITTLE ENDIAN)
      int numSprites = fm.readInt();
      FieldValidator.checkRange(numSprites, 1, 100);//guess

      for (int i = 0; i < numSprites; i++) {
        // X - Source Filename
        // 1 - null Source Filename Terminator
        String filename = fm.readNullString();

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(ArchivePlugin.calculatePadding(filename.length() + 1, 4));

        // 4 - Sprite Start Position X (LITTLE ENDIAN)
        // 4 - Sprite Start Position Y (LITTLE ENDIAN)
        // 4 - Sprite End Position X (LITTLE ENDIAN)
        // 4 - Sprite End Position Y (LITTLE ENDIAN)
        // 8 - null
        // 4 - Number of Colors? (256) (LITTLE ENDIAN)
        // 4 - Unknown (LITTLE ENDIAN)
        fm.skip(32);
      }

      // 4 - Header (TXPG)
      // 4 - null
      fm.skip(8);

      // X - Image Data (RGBA5551)
      ImageResource imageResource = ImageFormatReader.readRGBA5551(fm, width, height);

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
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
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

      // 4 - Unknown (2) (LITTLE ENDIAN)
      fm.writeBytes(src.readBytes(4));

      // 4 - Image Width (LITTLE ENDIAN)
      fm.writeInt(width);
      src.skip(4);

      // 4 - Image Height (LITTLE ENDIAN)
      fm.writeInt(height);
      src.skip(4);

      // 4 - Unknown (1) (LITTLE ENDIAN)
      fm.writeBytes(src.readBytes(4));

      // 4 - Number of Sprites within this Image (LITTLE ENDIAN)
      int numSprites = src.readInt();
      fm.writeInt(numSprites);

      for (int s = 0; s < numSprites; s++) {
        // X - Source Filename
        // 1 - null Source Filename Terminator
        String filename = src.readNullString();
        fm.writeString(filename);
        fm.writeByte(0);

        // 0-3 - null Padding to a multiple of 4 bytes
        int paddingSize = ArchivePlugin.calculatePadding(filename.length() + 1, 4);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        // 4 - Sprite Start Position X (LITTLE ENDIAN)
        // 4 - Sprite Start Position Y (LITTLE ENDIAN)
        // 4 - Sprite End Position X (LITTLE ENDIAN)
        // 4 - Sprite End Position Y (LITTLE ENDIAN)
        // 8 - null
        // 4 - Number of Colors? (256) (LITTLE ENDIAN)
        // 4 - Unknown (LITTLE ENDIAN)
        fm.writeBytes(src.readBytes(32));
      }

      // 4 - Header (TXPG)
      // 4 - null
      fm.writeBytes(src.readBytes(8));

      // X - Image Data (RGBA5551)
      ImageFormatWriter.writeGBAR5551(fm, imageResource);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}