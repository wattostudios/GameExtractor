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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ASSETS_15;
import org.watto.ge.plugin.resource.Resource_Unity3D_TEX;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_Unity3D_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_Unity3D_TEX() {
    super("Unity3D_TEX", "Unity3D TEX Texture Viewer");
    setExtensions("texture2d", "tex"); // LOWERCASE

    setGames("Unity3D Engine");
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

      if (Archive.getReadPlugin() instanceof Plugin_ASSETS_15) {
        // First, try to match to the ArchivePlugin
        rating += 50;
      }
      else {
        // If that fails, see if the selected resource is an instance of Resource_Unity3D_TEX
        //Resource selected = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelectedFile();
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        if (selected != null && selected instanceof Resource_Unity3D_TEX) {
          rating += 50;
        }
        else {
          // Otherwise force-fail
          return 0;
        }
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      // Don't need this bit (in this case), as we already determine suitability by looking for the Resource_Unity3D_TEX type above
      //else {
      //  return 0;
      //}

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

      // Relies on the Resource having the image details contained within it - if not, force-fail it!
      //Resource selected = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getSelectedFile();
      //if (selected == null || !(selected instanceof Resource_Unity3D_TEX)) {
      // not a Preview, maybe a thumbnail generator...
      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected == null || !(selected instanceof Resource_Unity3D_TEX)) {
        return null;
      }
      //}

      Resource_Unity3D_TEX resource = (Resource_Unity3D_TEX) selected;

      int height = resource.getImageHeight();
      int width = resource.getImageWidth();
      int mipMapCount = resource.getMipmapCount();
      int formatCode = resource.getFormatCode();

      // Go to the start of the texture data, just in case
      fm.seek(0);

      // Note all the images are upside-down, so need to be flipped
      ImageResource imageResource = null;
      if (formatCode == 1) {
        // 1 = A8 (8 bits per pixel)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.read8BitPaletted(fm, width, height));
        imageResource.addProperty("ImageFormat", "A8");
      }
      else if (formatCode == 2) {
        // 2 = ARGB4444 (16 bits per pixel - 4 bits per color)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readARGB4444(fm, width, height));
        imageResource.addProperty("ImageFormat", "ARGB4444");
      }
      else if (formatCode == 3) {
        // 3 = RGB24 (24 bits per pixel - 8 bits per color)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readRGB(fm, width, height));
        imageResource.addProperty("ImageFormat", "RGB");
      }
      else if (formatCode == 4) {
        // 4 = RGBA32 Format (32 bits per pixel - 8 bits per color)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readRGBA(fm, width, height));
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else if (formatCode == 5) {
        // 5 = ARGB32 Format (32 bits per pixel - 8 bits per color)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readARGB(fm, width, height));
        imageResource.addProperty("ImageFormat", "ARGB");
      }
      else if (formatCode == 7) {
        // 7 = RGB565 Format (16 bits per pixel)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readRGB565(fm, width, height));
        imageResource.addProperty("ImageFormat", "RGB565");
      }
      else if (formatCode == 10) {
        // 10 = DXT1 (4 bits per pixel)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readDXT1(fm, width, height));
        imageResource.addProperty("ImageFormat", "DXT1");
      }
      else if (formatCode == 12) {
        // 12 = DXT5 (8 bits per pixel)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readDXT5(fm, width, height));
        imageResource.addProperty("ImageFormat", "DXT5");
      }
      else if (formatCode == 13) {
        // 13 = RGBA4444 (TODO: don't know if this is right yet?)
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readRGBA4444(fm, width, height));
        imageResource.addProperty("ImageFormat", "RGBA4444");
      }
      else if (formatCode == 25) {
        // 25 = BC7
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readBC7(fm, width, height));
        imageResource.addProperty("ImageFormat", "BC7");
      }
      else if (formatCode == 28) {
        // 28 = DXT1Crunched
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readDXT1Crunched(fm, width, height));
        imageResource.addProperty("ImageFormat", "DXT1");
        /*
        FileManipulator uncrunchedFM = runCrunch(fm);
        if (uncrunchedFM == null) {
          ErrorLogger.log("[Viewer_Unity3D_Tex]: Failure during unCrunch");
          return null;
        }
        imageResource = new Viewer_DDS_DDS().readThumbnail(uncrunchedFM);
        uncrunchedFM.close();
        if (imageResource != null) {
          imageResource = ImageFormatReader.flipVertically(imageResource);
          imageResource.addProperty("ImageFormat", "DXT1");
        }
        */
      }
      else if (formatCode == 29) {
        // 29 = DXT5Crunched
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readDXT5Crunched(fm, width, height));
        imageResource.addProperty("ImageFormat", "DXT5");
        /*
        FileManipulator uncrunchedFM = runCrunch(fm);
        if (uncrunchedFM == null) {
          ErrorLogger.log("[Viewer_Unity3D_Tex]: Failure during unCrunch");
          return null;
        }
        imageResource = new Viewer_DDS_DDS().readThumbnail(uncrunchedFM);
        uncrunchedFM.close();
        if (imageResource != null) {
          imageResource = ImageFormatReader.flipVertically(imageResource);
          imageResource.addProperty("ImageFormat", "DXT5");
        }
        */
      }
      else if (formatCode == 47) {
        // 47 = ETC2_RGBA8 (TODO: not right yet!)
        int extraSkip = 8;
        fm.skip(extraSkip);
        imageResource = ImageFormatReader.flipVertically(ImageFormatReader.readETC2_RGBA8(fm, width, height));
        imageResource.addProperty("ImageFormat", "ETC2_RGBA8");
      }
      else {
        ErrorLogger.log("[Viewer_Unity3D_Tex]: Unknown Image Format: " + formatCode);
        return null; // unknown image format
      }

      imageResource.addProperty("MipmapCount", "" + mipMapCount);

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