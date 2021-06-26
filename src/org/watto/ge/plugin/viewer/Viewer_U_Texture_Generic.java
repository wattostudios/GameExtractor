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

import java.io.File;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.PluginGroup_U;
import org.watto.ge.plugin.archive.Plugin_U_Generic;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.resource.Resource_Unreal;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_U_Texture_Generic extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_U_Texture_Generic() {
    super("U_Texture_Generic", "Unreal Engine Paletted Texture Image");
    setExtensions("texture"); // MUST BE LOWER CASE

    setGames("Unreal Engine",
        "Adventure Pinball: Forgotten Island",
        "Clive Barker's Undying",
        "Deus Ex",
        "Harry Potter And The Chamber Of Secrets",
        "Mobile Forces",
        "Nerf ArenaBlast",
        "Rune",
        "Star Trek: Deep Space Nine: The Fallen: Maximum Warp",
        "Star Trek: The Next Generation: Klingon Honor Guard",
        "Unreal",
        "Unreal Tournament",
        "Virtual Reality Notre Dame: A Real Time Construction",
        "Wheel Of Time",
        "X-Com Enforcer");
    setPlatforms("PC");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("static-access")
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (Archive.getReadPlugin() instanceof PluginGroup_U) {
        rating += 50;
      }

      if (Archive.getReadPlugin() instanceof Plugin_U_Generic) {
        rating += 10;
      }

      File paletteFile = Archive.getReadPlugin().getDirectoryFile(fm.getFile(), "Palette", false);
      if (paletteFile != null && paletteFile.exists()) {
        rating += 11;
      }
      else {
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        if (selected != null && selected instanceof Resource_Unreal) {

          Resource_Unreal resource = (Resource_Unreal) selected;
          //resource.setUnrealProperties(readPlugin.readProperties(readSource));
          UnrealProperty property = resource.getUnrealProperty("Palette");
          if (property != null) {
            rating += 22; // so the -11 actually makes this just +11
          }
        }
        rating -= 11;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        rating = 0;
      }

      // 1 - Number Of Mipmaps (9)
      if (ByteConverter.unsign(fm.readByte()) < 50) {
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
   * Reads a DXT image
   **********************************************************************************************
   **/
  public ImageResource loadPaletted(FileManipulator fm, int width, int height, int[] palette) throws Exception {

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int byteValue = ByteConverter.unsign(fm.readByte());
        //data[x+y*width] = ((byteValue << 16) | (byteValue << 8) | byteValue | (255 << 24));
        data[x + y * width] = palette[byteValue];
      }
    }

    return new ImageResource(data, width, height);

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
  
  **********************************************************************************************
  **/
  public int[] readPalette(File path) {
    return readPalette(new FileManipulator(path, false));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] readPalette(FileManipulator fm) {
    try {
      // 1-5 - Number Of Colors
      int numColors = (int) PluginGroup_U.readIndex(fm);
      if (numColors == 0) {
        numColors = (int) PluginGroup_U.readIndex(fm);
        if (numColors == 0) {
          return null;
        }
      }

      if (fm.getLength() - (numColors * 4) > 50) {
        // Try again
        numColors = (int) PluginGroup_U.readIndex(fm);
        if (numColors == 0) {
          return null;
        }

        if (fm.getLength() - (numColors * 4) > 50) {
          throw new WSPluginException("NumColors is incorrect");
        }
      }

      // X - Palette
      int[] palette = new int[numColors];
      for (int i = 0; i < numColors; i++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = ByteConverter.unsign(fm.readByte());
        a = 255;

        palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      fm.close();

      return palette;
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

      int numMipmaps = 1;
      try {
        // 1 - Number Of Mipmaps (9)
        numMipmaps = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkLength(numMipmaps, 20);
      }
      catch (Throwable t) {
        // skip another 3 bytes and try again
        fm.skip(3);

        numMipmaps = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkLength(numMipmaps, 20);
      }

      // for each mipmap
      for (int i = 0; i < numMipmaps; i++) {
        if (/* readPlugin instanceof Plugin_U_Generic && */PluginGroup_U.getVersion() >= 63) {
          // 4 - Unknown
          fm.skip(4);
        }

        // 1-5 - Image Data Length
        int dataLength = (int) PluginGroup_U.readIndex(fm);
        FieldValidator.checkLength(dataLength, fm.getLength());

        // X - Texture Data (DXT1=W*H/2, DXT3/5=W*H)
        byte[] textureData = fm.readBytes(dataLength);

        // 4 - Width
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        if ((width * height) != dataLength) {
          return null; // not enough data - probably not actually paletted, might be DXT
        }

        // 1 - ID
        // 1 - ID
        fm.skip(2);

        if (dataLength > 0) {
          //fm.seek(fm.getOffset() - 10 - dataLength); // not needed, as we have already read the textureData into a byte[]

          int[] palette = null;

          File paletteFile = PluginGroup_U.getDirectoryFile(fm.getFile(), "Palette", false);
          if (paletteFile != null && paletteFile.exists()) {
            palette = readPalette(paletteFile);
          }
          else {
            Resource selected = (Resource) SingletonManager.get("CurrentResource");
            if (selected != null && selected instanceof Resource_Unreal) {

              Resource_Unreal resource = (Resource_Unreal) selected;
              //resource.setUnrealProperties(readPlugin.readProperties(readSource));
              UnrealProperty property = resource.getUnrealProperty("Palette");
              if (property != null) {
                // found the palette property - so lets extract the palette too
                try {
                  long resourceNumber = ((Long) property.getValue()).longValue();
                  if (resourceNumber > 0) {
                    resourceNumber--;
                    Resource paletteResource = Archive.getResource((int) resourceNumber);

                    FileManipulator byteBuffer = new FileManipulator(new ByteBuffer((int) paletteResource.getLength()));
                    paletteResource.extract(byteBuffer);
                    byteBuffer.seek(0);
                    palette = readPalette(byteBuffer);
                    byteBuffer.close();
                  }
                }
                catch (Throwable t) {
                  //t.printStackTrace();
                }
              }
            }

          }

          if (palette == null) {
            throw new WSPluginException("Not a paletted image");
          }

          // Now open the already-read texture data from above, so we can read the image
          fm.close();
          fm = new FileManipulator(new ByteBuffer(textureData));

          ImageResource imageResource = loadPaletted(fm, width, height, palette);

          fm.close();

          imageResource.setProperty("MipmapCount", "" + numMipmaps);
          imageResource.setProperty("Version", "" + PluginGroup_U.getVersion());

          return imageResource;

        }

      }

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);
      // Paletted, 256 colors
      im.convertToPaletted();
      im.changeColorCount(256);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // Generate all the PALETTED mipmaps of the image
      ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
      int mipmapCount = mipmaps.length;

      // Set some property defaults in case we're doing a conversion (and thus there probably isn't any properties set)
      int version = 61;

      // Now try to get the property values from the ImageResource, if they exist
      ImageResource imageResource = ((PreviewPanel_Image) preview).getImageResource();

      if (imageResource != null) {
        version = imageResource.getProperty("Version", 61);
        mipmapCount = imageResource.getProperty("MipmapCount", 1);
      }

      if (mipmapCount > mipmaps.length) {
        mipmapCount = mipmaps.length;
      }

      // 1 - Number Of MipMaps
      fm.writeByte(mipmapCount);

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageManipulator mipmap = mipmaps[i];

        int width = mipmap.getWidth();
        int height = mipmap.getHeight();

        int length = width * height;

        if (version >= 63) {
          // 4 - Unknown
          fm.writeInt(length);
        }

        // 1-5 - Image Data Length
        PluginGroup_U.writeIndex(fm, length);

        // X - Texture Data (DXT1=W*H/2, DXT3/5=W*H, RGBA=W*H*4)
        int[] pixels = im.getPixels();
        for (int p = 0; p < length; p++) {
          fm.writeByte(pixels[p]);
        }

        // 4 - Width
        fm.writeInt(width);

        // 4 - Height
        fm.writeInt(height);

        // 1 - ID
        fm.writeByte(2);

        // 1 - ID
        fm.writeByte(2);

      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}