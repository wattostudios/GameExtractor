/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

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
import org.watto.ge.plugin.archive.Plugin_DAT_121;
import org.watto.ge.plugin.archive.Plugin_WAD_21;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_21_SPR extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WAD_21_SPR() {
    super("WAD_21_SPR", "WAD_21_SPR Image");
    setExtensions("spr");

    setGames("Spec Ops: Covert Assault");
    setPlatforms("PSX");
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
      if (plugin instanceof Plugin_WAD_21 || plugin instanceof Plugin_DAT_121) {
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

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.getLength() % 24 == 0) {
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

      // see if we can find the matching PSX file first, which contains the imagedata
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      String filename = resource.getName();
      int dotPos = filename.lastIndexOf('.');
      if (dotPos <= 0) {
        return null;
      }
      filename = filename.substring(0, dotPos) + ".PSX";

      Resource psxResource = null;

      Resource[] resources = Archive.getResources();
      int numResources = resources.length;
      for (int i = 0; i < numResources; i++) {
        Resource currentResource = resources[i];
        if (currentResource.getName().equals(filename)) {
          // found the PSX file
          psxResource = currentResource;
          break;
        }
      }

      if (psxResource == null) {
        return null;
      }

      long arcSize = fm.getLength();
      int numImages = (int) (arcSize / 24);

      arcSize = psxResource.getDecompressedLength();

      int[] offsets = new int[numImages];
      int[] widths = new int[numImages];
      int[] heights = new int[numImages];
      for (int i = 0; i < numImages; i++) {
        // 4 - Image Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1); // +1 to allow empty images at EOF

        // 4 - null
        // 2 - null
        fm.skip(6);

        // 1 - Image Width
        int width = ByteConverter.unsign(fm.readByte());

        // 1 - Image Height
        int height = ByteConverter.unsign(fm.readByte());

        if (offset == arcSize) {
          width = 0;
          height = 0;
        }

        // 4 - Unknown
        // 4 - null
        // 4 - Unknown
        fm.skip(12);

        offsets[i] = offset;
        widths[i] = width;
        heights[i] = height;
      }

      // Close the SPR, open the PSX
      fm.close();

      ByteBuffer buffer = new ByteBuffer((int) psxResource.getDecompressedLength());
      fm = new FileManipulator(buffer);
      psxResource.extract(fm);

      fm.seek(0);

      // Read the palette
      int[] palette = null;
      int imageFormat = 8;

      int firstOffset = offsets[0];
      if (firstOffset == 32) {
        // 4-bit
        imageFormat = 4;
        palette = ImageFormatReader.readABGR1555(fm, 16, 1).getImagePixels();
      }
      else { // 512
        // assume 8-bit
        imageFormat = 8;
        palette = ImageFormatReader.readABGR1555(fm, 256, 1).getImagePixels();
      }

      // read the images
      ImageResource[] images = new ImageResource[numImages];
      for (int i = 0; i < numImages; i++) {
        fm.relativeSeek(offsets[i]);

        // X - Pixels
        if (imageFormat == 4) {
          images[i] = ImageFormatReader.read4BitPaletted(fm, widths[i], heights[i], palette);
        }
        else if (imageFormat == 8) {
          images[i] = ImageFormatReader.read8BitPaletted(fm, widths[i], heights[i], palette);
        }
      }

      fm.close();

      // set the prev/next frames
      if (numImages > 1) {
        for (int i = 0; i < numImages; i++) {
          ImageResource image = images[i];
          if (i == 0) {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[numImages - 1]);
          }
          else if (i == numImages - 1) {
            image.setNextFrame(images[0]);
            image.setPreviousFrame(images[i - 1]);
          }
          else {
            image.setNextFrame(images[i + 1]);
            image.setPreviousFrame(images[i - 1]);
          }
        }

        images[0].setManualFrameTransition(true);
      }

      return images[0];

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