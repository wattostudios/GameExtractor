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
import org.watto.ge.plugin.archive.Plugin_BF_CRYOBF;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BF_CRYOBF_SPR_SPRCRYO extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BF_CRYOBF_SPR_SPRCRYO() {
    super("BF_CRYOBF_SPR_SPRCRYO", "BF_CRYOBF_SPR_SPRCRYO");
    setExtensions("spr");

    setEnabled(false); // TODO NOT ENABLED

    setGames("Echo: Secret of the Lost Cavern");
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
      if (plugin instanceof Plugin_BF_CRYOBF) {
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

      // 8 - Header ("SPRCRYO ")
      String header = fm.readString(8);
      if (header.equals("SPRCRYO ")) {
        rating += 25;
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

      // 8 - Header ("SPRCRYO ")
      // 1 - Unknown (120)
      // 1 - Unknown (0/2)
      // 2 - null
      fm.skip(12);

      int numColors = 256;
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha (reversed)
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = 255 - ByteConverter.unsign(fm.readByte());

        palette[i] = ((a << 24) | (r << 16) | (g << 8) | b);

        System.out.println("Color " + i + "\t" + r + "\t" + g + "\t" + b + "\t" + a);
      }

      // 4 - Number of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // for each image
      // 4 - Image Data Offset [+4] (relative to the start of the DIRECTORY)
      fm.skip(numImages * 4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      System.out.println("Width: " + width);
      System.out.println("Height: " + height);

      // 4 - Unknown (0/-1)
      fm.skip(4);

      long rowRelOffset = fm.getOffset() + (height * 4);

      long[] rowOffsets = new long[height];
      for (int i = 0; i < height; i++) {
        // 4 - Offset to Image Data for the Row (relative to the end of this "for" loop)
        long rowOffset = fm.readInt() + rowRelOffset;
        FieldValidator.checkOffset(rowOffset, arcSize);
        rowOffsets[i] = rowOffset;
      }

      for (int i = 0; i < height - 1; i++) { // TODO TEMP -1 BECAUSE WE CAN'T WORK OUT THE LENGTH OF THE LAST ROW DATA
        // X - RLE encoded palette indexes
        int rowOffset = (int) rowOffsets[i];
        int rowLength = (int) rowOffsets[i + 1] - rowOffset;

        fm.seek(rowOffset);

        String output = "Row " + (i + 1);
        for (int p = 0; p < rowLength; p++) {
          output += "\t" + ByteConverter.unsign(fm.readByte());
        }
        System.out.println(output);
      }

      // TODO TEMP EXIT INSTEAD OF GENERATING AN IMAGE
      return null;

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