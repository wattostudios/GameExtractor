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
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_MBUNDLE_BPLIST00;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_MBUNDLE_BPLIST00_TEXTURE_RES extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_MBUNDLE_BPLIST00_TEXTURE_RES() {
    super("MBUNDLE_BPLIST00_TEXTURE_RES", "MBUNDLE TEXTURE Image");
    setExtensions("texture");

    setGames("Rune Factory 4 Special");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_MBUNDLE_BPLIST00) {
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

      // 4 - Header
      if (fm.readString(3).equals("RES") && fm.readByte() == 0) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(8);

      // 4 - File Length
      if (fm.readInt() == fm.getLength()) {
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

      long arcSize = fm.getLength();

      // 4 - Header ("RES" + null)
      // 8 - null
      // 4 - File Length
      // 4 - File Length [+128]
      // 4 - TEXT Block Offset
      // 4 - Unknown
      // 8 - File Length
      // 4 - Unknown (1)
      fm.skip(40);

      // 8 - TEXT Block Offset [+8]
      long pixlBlockOffset = fm.readLong();
      FieldValidator.checkOffset(pixlBlockOffset, arcSize);

      // 8 - DESC Block Offset
      long descBlockOffset = fm.readLong();
      FieldValidator.checkOffset(descBlockOffset, arcSize);

      fm.relativeSeek(pixlBlockOffset);

      // 8 - PIXL Block Offset
      pixlBlockOffset = fm.readLong();
      FieldValidator.checkOffset(pixlBlockOffset, arcSize);

      fm.relativeSeek(pixlBlockOffset);

      // 4 - Header (PIXL)
      // 4 - Block Length (including these header fields)
      // 8 - Block Length (including these header fields)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - Unknown (32)
      fm.skip(36);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      fm.relativeSeek(descBlockOffset);

      // 4 - Header (DESC)
      // 4 - null
      // 8 - Block Length (including this 16-byte header)
      fm.skip(16);

      // X - Pixel Data (BC7)
      ImageResource imageResource = ImageFormatReader.readBC7(fm, width, height);

      // rotate left 90 degrees
      imageResource = ImageFormatReader.rotateLeft(imageResource);

      // swap red and blue
      imageResource = ImageFormatReader.swapRedAndBlue(imageResource);

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