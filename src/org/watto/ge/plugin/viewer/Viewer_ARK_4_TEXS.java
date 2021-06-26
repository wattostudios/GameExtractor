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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARK_4;
import org.watto.ge.plugin.archive.Plugin_TEXS_SXET;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARK_4_TEXS extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARK_4_TEXS() {
    super("ARK_4_TEXS", "Split Second TEXS Image");
    setExtensions("texs", "dds");

    setGames("Split Second");
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
      if (plugin instanceof Plugin_ARK_4 || plugin instanceof Plugin_TEXS_SXET) {
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
      if (fm.readString(4).equals("SXET")) {
        rating += 50;
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

      if (FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase("dds")) {
        // 4 - Image Data Length
        fm.skip(4);

        // X - DDS Image
        return new Viewer_DDS_DDS().readThumbnail(fm);
      }

      // 4 - Header (SXET)
      // 4 - Header Length (12)
      // 4 - null
      fm.skip(12);

      // 4 - Image Data Directory Offset [+16]
      int dataOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Number of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // 4 - Unknown (24)
      // 8 - null
      // 4 - Unknown (1)
      // 4 - Unknown
      fm.skip(20);

      // skip the image details
      fm.skip(numImages * 64);

      // 4 - Image Data Directory Length
      // 4 - null
      // 4 - Number of Images
      // 4 - Unknown
      fm.skip(16);

      // 4 - Unknown
      // 4 - null
      fm.skip(8);

      // 4 - Image Data Offset (relative to the start of the Image Data Directory)
      int offset = fm.readInt() + dataOffset;
      FieldValidator.checkOffset(offset, arcSize);

      fm.seek(offset);

      // 4 - Image Data Length
      fm.skip(4);

      // X - DDS Image
      return new Viewer_DDS_DDS().readThumbnail(fm);

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