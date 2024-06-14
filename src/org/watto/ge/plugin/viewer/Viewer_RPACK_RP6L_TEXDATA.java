/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RPACK_RP6L;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RPACK_RP6L_TEXDATA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RPACK_RP6L_TEXDATA() {
    super("RPACK_RP6L_TEXDATA", "RPACK_RP6L_TEXDATA Image");
    setExtensions("tex_data");

    setGames("Dying Light: Bad Blood",
        "FIM Speedway Grand Prix 15",
        "Dead Island");
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
      if (plugin instanceof Plugin_RPACK_RP6L) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
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

      // Find the corresponding tex_header file for this image
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      String headerFileIDString = resource.getProperty("tex_header");
      if (headerFileIDString == null || headerFileIDString.equals("")) {
        return null;
      }

      int headerFileID = -1;
      try {
        headerFileID = Integer.parseInt(headerFileIDString);
      }
      catch (Throwable t) {
      }
      if (headerFileID == -1) {
        return null;
      }

      Resource headerResource = Archive.getResource(headerFileID);
      if (headerResource == null) {
        return null;
      }

      // So the thumbnails generate properly, we first need to read in all the data from the file, before extracting the header
      int fileLength = (int) resource.getDecompressedLength();
      byte[] bytes = fm.readBytes(fileLength);

      // now read the header
      int headerLength = (int) headerResource.getLength();

      ByteBuffer buffer = new ByteBuffer(headerLength);
      FileManipulator headerFM = new FileManipulator(buffer);
      headerResource.extract(headerFM);

      headerFM.seek(0); // back to the beginning of the byte array

      // 2 - Image Width
      int width = headerFM.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = headerFM.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown (1)
      // 2 - Unknown (1)
      headerFM.skip(4);

      // 4 - Mipmap Count
      int numMipmaps = headerFM.readInt();
      FieldValidator.checkRange(numMipmaps, 1, 20);

      // 4 - Image Format? (17=DXT1, 18=DXT3, 19=DXT5)
      int imageFormat = headerFM.readInt();

      headerFM.close();

      // now we have read the header, we can go and read the pixel data (using the bytes we have read in at the beginning)
      fm.close();
      buffer = new ByteBuffer(bytes);
      fm = new FileManipulator(buffer);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 17) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat == 18) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat == 19) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat == 2) {
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_RPACK_RP6L_TEXDATA] Unknown Image Format: " + imageFormat);
        return null;
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      //imageResource.addProperty("MipmapCount", "" + numMipmaps);

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

}