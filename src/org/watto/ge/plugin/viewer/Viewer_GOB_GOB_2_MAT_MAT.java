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
import org.watto.ge.helper.PaletteGenerator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_GOB_GOB_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GOB_GOB_2_MAT_MAT extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_GOB_GOB_2_MAT_MAT() {
    super("GOB_GOB_2_MAT_MAT", "Jedi Knight: Mysteries Of The Sith MAT Image");
    setExtensions("mat");

    setGames("Jedi Knight: Mysteries Of The Sith");
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
      if (plugin instanceof Plugin_GOB_GOB_2) {
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
      if (fm.readString(4).equals("MAT ")) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      fm.skip(8);

      // 4 - Number Of Images
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header (MAT )
      // 4 - Unknown (50)
      // 4 - Unknown (2)
      fm.skip(12);

      // 4 - Number Of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Images
      // 4 - null
      // 4 - Bits Per Pixel? (8)
      // 4 - Unknown
      // 4 - Unknown (64)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (64)
      // 4 - Unknown
      // 4 - Unknown (64)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (64)
      // 4 - Unknown
      fm.seek(76 + (numFiles * 40));

      // 4 - Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 12 - null
      fm.skip(12);

      // 4 - Number Of Mipmaps
      int mipmapCount = fm.readInt();

      int[] palette = PaletteGenerator.getGrayscalePalette().getPalette();

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      for (int p = 0; p < numPixels; p++) {
        // 1 - Color Palette Index
        pixels[p] = palette[ByteConverter.unsign(fm.readByte())];
      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

      fm.close();

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
  public void write(PreviewPanel preview, FileManipulator fm) {

  }

}