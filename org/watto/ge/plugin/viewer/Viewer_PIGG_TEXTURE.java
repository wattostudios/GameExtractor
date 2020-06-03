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

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import javax.imageio.ImageIO;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PIGG;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_PIGG_TEXTURE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_PIGG_TEXTURE() {
    super("PIGG_TEXTURE", "City of Heroes TEXTURE Image");
    setExtensions("texture");

    setGames("City of Heroes");
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
      if (plugin instanceof Plugin_PIGG) {
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

      long arcSize = fm.getLength();

      // 4 - Header Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(21);

      // 4 - TX2 Header (null + "TX2")
      if (fm.readString(3).equals("TX2")) {
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

      // 4 - Header Length
      int headerLength = fm.readInt() - 4;
      FieldValidator.checkLength(headerLength, arcSize);

      // 4 - Data Length
      // 4 - Source Image Width
      // 4 - Source Image Height
      // 12 - null
      // 4 - TX2 Header (null + "TX2")
      // X - Source Filename
      // 1 - null Source Filename Terminator
      fm.skip(headerLength);

      // 4 - DDS Header ("DDS ")
      long offset = fm.getOffset();
      byte[] imageHeader = fm.readBytes(4);

      if (ByteConverter.unsign(imageHeader[0]) == 255 && ByteConverter.unsign(imageHeader[1]) == 216) {
        // JPEG image - read it here and return it
        fm.relativeSeek(offset);

        BufferedImage image = ImageIO.read(new ManipulatorInputStream(fm));
        int width = image.getWidth();
        int height = image.getHeight();

        PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, false);
        pixelGrabber.grabPixels();

        // get the pixels, and convert them to positive values in an int[] array
        int[] pixels = (int[]) pixelGrabber.getPixels();

        return new ImageResource(pixels, width, height);
      }

      // 4 - Unknown (124)
      // 4 - Unknown (4111)
      fm.skip(8);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Flags? (512/2048)
      // 44 - null
      // 4 - NVTT Header (NVTT)
      // 4 - Unknown (2309)
      // 4 - Unknown (32)
      // 4 - Unknown (65)
      fm.skip(64);

      // 4 - null (or DXT1 or DXT5)
      String imageFormat = fm.readString(4);

      // 4 - Unknown (32)
      // 4 - Flags?
      // 4 - Unknown (65280)
      // 4 - Unknown (255)
      // 4 - Flags?
      // 4 - Unknown (4096)
      // 16 - null
      fm.skip(40);

      // X - Image Data (BGRA)
      ImageResource imageResource = null;
      if (imageFormat.contentEquals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.contentEquals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.contentEquals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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