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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_ITEXTURE;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_ITEXTURE_ITEXTURE extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_ITEXTURE_ITEXTURE() {
    super("DAT_ITEXTURE_ITEXTURE", "DAT_ITEXTURE_ITEXTURE Image");
    setExtensions("itexture");

    setGames("Ice and Fire");
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
      if (plugin instanceof Plugin_DAT_ITEXTURE) {
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

      // 2 - Number of Mipmaps
      short numMipmaps = fm.readShort();
      FieldValidator.checkRange(numMipmaps, 1, 20); // guess max

      // 2 - Unknown (0/1)
      fm.skip(2);

      // 2 - Image Width [1<<value]
      int width = (1 << fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Image Height [1<<value]
      int height = (1 << fm.readShort());
      FieldValidator.checkWidth(height);

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

      /*
      if (!PaletteManager.hasPalettes()) {
        // load the palettes from the PALETTE.ITexture file
      
        Resource[] resources = Archive.getResources();
        int numResources = resources.length;
        for (int i = 0; i < numResources; i++) {
          Resource resource = resources[i];
          if (resource.getName().equals("PALETTE.ITexture")) {
            // found the color palette file - need to extract it and read the colors
      
            // Extract it
            int paletteLength = (int) resource.getLength();
            ByteBuffer buffer = new ByteBuffer(paletteLength);
            FileManipulator palFM = new FileManipulator(buffer);
            resource.extract(palFM);
      
            palFM.seek(0); // back to the beginning of the byte array
            palFM.skip(8); // skip the header
      
            int numPalettes = paletteLength / 256;
            for (int p = 0; p < numPalettes; p++) {
              // read in the single palette
              int[] palette = new int[256];
      
              for (int b = 0; b < 256; b++) {
                int currentByte = ByteConverter.unsign(palFM.readByte());
                palette[b] = (255 << 24 | currentByte | currentByte << 8 | currentByte << 16);
              }
              PaletteManager.addPalette(new Palette(palette), true);
            }
      
            palFM.close();
      
            break;
          }
        }
      }
      */

      // 2 - Number of Mipmaps
      short numMipmaps = fm.readShort();
      FieldValidator.checkRange(numMipmaps, 1, 20); // guess max

      // 2 - Unknown (0/1)
      fm.skip(2);

      // 2 - Image Width [1<<value]
      int width = (1 << fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Image Height [1<<value]
      int height = (1 << fm.readShort());
      FieldValidator.checkWidth(height);

      // X - Pixels
      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);

      fm.close();

      imageResource.addProperty("MipmapCount", "" + numMipmaps);

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