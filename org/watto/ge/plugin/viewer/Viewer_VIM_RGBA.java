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
import org.watto.ge.plugin.archive.Plugin_DAT_RARC;
import org.watto.ge.plugin.archive.Plugin_VIM;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VIM_RGBA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VIM_RGBA() {
    super("VIM_RGBA", "Broken Sword VIM RGBA Image");
    setExtensions("rgba", "vim");

    setGames("Broken Sword 2: The Smoking Mirror");
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
      if (plugin instanceof Plugin_VIM || plugin instanceof Plugin_DAT_RARC) {
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

      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      ImageResource imageResource = null;

      if (FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase("vim")) {
        // this is a VIM image in a DAT archive

        // 2 - Image Width (drop the top bit)
        //int width = (fm.readShort() & 32767);
        int width = fm.readShort();
        boolean rgb565 = false;
        if (width < 0) {
          width = (width & 32767);
          rgb565 = true;
        }

        // 2 - Image Height
        int height = fm.readShort();

        // 4 - Compressed Length
        int length = fm.readInt();

        // X - ZLib-compressed Data
        int decompLength = width * height;
        if (rgb565) {
          decompLength *= 2;
        }
        else {
          decompLength *= 4; // RGBA
        }

        byte[] extractedData = new byte[decompLength];
        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, length, decompLength);
        for (int i = 0; i < decompLength; i++) {
          exporter.available();
          extractedData[i] = (byte) exporter.read();
        }
        exporter.close();

        // open the decompressed file data for processing
        fm.close();
        fm = new FileManipulator(new ByteBuffer(extractedData));

        // Pixels
        if (rgb565) {
          imageResource = ImageFormatReader.readRGB565(fm, width, height);
        }
        else {
          imageResource = ImageFormatReader.readRGBA(fm, width, height);
        }

      }
      else {
        // this is a RGBA image in a VIM file

        int height = 640;
        int width = 480;

        // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
        try {
          height = Integer.parseInt(resource.getProperty("Height"));
          width = Integer.parseInt(resource.getProperty("Width"));
        }
        catch (Throwable t) {
          //
        }

        // X - Pixels
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }

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