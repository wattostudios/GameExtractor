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

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DV2;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_NIF_GAMEBYRO_NiPersistentSrcTextureRendererData_20_3_0_9 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_NIF_GAMEBYRO_NiPersistentSrcTextureRendererData_20_3_0_9() {
    super("NIF_GAMEBYRO_NiPersistentSrcTextureRendererData_20_3_0_9", "NIF_GAMEBYRO_NiPersistentSrcTextureRendererData_20_3_0_9");
    setExtensions("nif");

    setGames("Divinity 2");
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
      if (plugin instanceof Plugin_DV2) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      fm.skip(30);

      // Version Number
      if (fm.readString(8).equals("20.3.0.9")) {
        rating += 5;
      }

      fm.skip(20);

      // 34 - Object Type (NiPersistentSrcTextureRendererData)
      if (fm.readString(34).equals("NiPersistentSrcTextureRendererData")) {
        rating += 5;
      }
      else {
        rating = 0; // force to only allow this object type (NiPersistentSrcTextureRendererData)
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

      // 40 - Header ("Gamebryo File Format, Version 20.3.0.9" + (byte)10,9)
      // 2 - Unknown (768)
      // 4 - Unknown (276)
      // 2 - Unknown (3)
      // 4 - Number Of Files (1)
      // 2 - Number Of Files (1)

      // 4 - Object Type Name Length (34)
      // 34 - Object Type (NiPersistentSrcTextureRendererData)
      // 2 - null Padding to a multiple of 4 bytes

      // 4 - File Length (not including the IMAGE HEADER)
      // 12 - null
      fm.skip(110);

      // 4 - Image Format (6=DXT5)
      int imageType = fm.readInt();
      String imageFormat = "DXT5";
      if (imageType == 5 || imageType == 6) {
        imageFormat = "DXT5";
      }
      else if (imageType == 4) {
        imageFormat = "DXT1";
      }
      //else if (imageType == 2){
      //  imageFormat = "8bitPaletted"; // Not supported
      //}
      else if (imageType == 1) {
        imageFormat = "RGBA";
      }
      else if (imageType == 0) {
        imageFormat = "RGB";
      }
      else {
        ErrorLogger.log("[NIF_GAMEBYRO_NiPersistentSrcTextureRendererData_20_3_0_9]: Unsupported Image Format: " + imageType);
      }

      // 1 - Bits Per Pixel? (0)
      // 4 - Unknown (-1)
      // 4 - Unknown (0)

      // 1 - Flags (1)
      // 4 - Unknown (0)
      // 1 - Unknown (0)
      fm.skip(15);

      // for each Channel (4)
      // 4 - Channel Type (4=Compressed, 19=Empty)
      // 4 - Channel Convention (4=Compressed, 5=Empty)
      // 1 - Bits per Channel (0)
      // 1 - Unknown (0)
      fm.skip(10 * 4);

      // 4 - Color Palette Offset? (-1)
      fm.skip(4);

      // 4 - Number of Mipmaps
      int mipmapCount = fm.readInt();
      FieldValidator.checkRange(mipmapCount, 0, 50);

      // 4 - Bytes per Pixel? (0)
      fm.skip(4);

      // for each mipmap
      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Offset to the Mipmap Data (relative to the start of the Image Data)
      fm.skip(4);

      fm.skip((mipmapCount - 1) * 12); // skip the details of the other mipmaps

      // 4 - Image Data Length (total for all mipmaps)
      FieldValidator.checkLength(fm.readInt(), arcSize);

      // 4 - Image Data Length (total for all mipmaps)
      FieldValidator.checkLength(fm.readInt(), arcSize);

      // 4 - Number of Faces? (1)
      // 4 - Unknown
      fm.skip(8);

      // X - Image Data
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT5")) { // DXT5
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (imageFormat.equals("RGB")) { // RGB
        imageResource = ImageFormatReader.readRGB(fm, width, height);
      }
      else if (imageFormat.equals("RGBA")) { // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat.equals("DXT1")) { // DXT1
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        return null; // unknown (or other) image format
      }

      imageResource.addProperty("ImageFormat", imageFormat);
      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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