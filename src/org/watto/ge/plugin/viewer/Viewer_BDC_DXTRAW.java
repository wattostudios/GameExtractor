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
import org.watto.ge.plugin.archive.Plugin_BDC;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BDC_DXTRAW extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BDC_DXTRAW() {
    super("BDC_DXTRAW", "Bioshock 2 Raw DDS Image");
    setExtensions("dxtraw");

    setGames("Bioshock 2");
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
      if (plugin instanceof Plugin_BDC) {
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

      long arcSize = fm.getLength();

      // Guess the width/height from a predefined set of known values
      int width = 1024;
      int height = 1024;

      long curPos = fm.getOffset();
      // see if we can guess Alpha or not
      boolean alpha = false;
      if (fm.readInt() == 0) {
        alpha = true;
      }
      fm.relativeSeek(curPos);

      if (!alpha) {
        // DXT1
        if (arcSize == 32768) {
          width = 512;
          height = 128;
        }
        else if (arcSize == 40960) {
          width = 256;
          height = 256;
        }
        else if (arcSize == 81920) {
          width = 512;
          height = 256;
        }
        else if (arcSize == 131072) {
          width = 2048;
          height = 128;
        }
        else if (arcSize == 163840) {
          width = 1024;
          height = 256;
        }
        else if (arcSize == 172032) {
          width = 512;
          height = 512;
        }
        else if (arcSize == 344064) {
          width = 1024;
          height = 512;
        }
        else if (arcSize == 688128) {
          width = 2048;
          height = 512;
        }
        else if (arcSize == 696320) {
          width = 1024;
          height = 1024;
        }
        else if (arcSize == 1392640) {
          width = 2048;
          height = 1024;
        }
        else if (arcSize == 2793472) {
          width = 2048;
          height = 2048;
        }
      }
      else if (alpha) {
        // DXT5
        if (arcSize == 32768) {
          width = 256;
          height = 128;
        }
        else if (arcSize == 81920) {
          width = 256;
          height = 256;
        }
        else if (arcSize == 131072) {
          width = 1024;
          height = 128;
        }
        else if (arcSize == 163840) {
          width = 512;
          height = 256;
        }
        else if (arcSize == 327680) {
          width = 1024;
          height = 256;
        }
        else if (arcSize == 344064) {
          width = 512;
          height = 512;
        }
        else if (arcSize == 688128) {
          width = 1024;
          height = 512;
        }
        else if (arcSize == 1376256) {
          width = 2048;
          height = 512;
        }
        else if (arcSize == 1392640) {
          width = 1024;
          height = 1024;
        }
        else if (arcSize == 2785280) {
          width = 2048;
          height = 1024;
        }
        else if (arcSize == 5586944) {
          width = 2048;
          height = 2048;
        }

      }

      // X - Pixels
      ImageResource imageResource = null;
      if (alpha) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
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