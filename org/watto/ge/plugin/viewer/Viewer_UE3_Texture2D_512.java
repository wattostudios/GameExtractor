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
import org.watto.ErrorLogger;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.PluginGroup_UE3;
import org.watto.ge.plugin.archive.Plugin_UE3_512;
import org.watto.ge.plugin.archive.datatype.UnrealProperty;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_UE3_Texture2D_512 extends ViewerPlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Viewer_UE3_Texture2D_512() {
    super("UE3_Texture2D_512", "Unreal Engine 3 Version 512 Texture Image");
    setExtensions("texture2d"); // MUST BE LOWER CASE

    setGames("Unreal Engine 3 Version 512",
        "Mass Effect 2");
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_UE3_512) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        rating = 0;
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

  @SuppressWarnings("static-access")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (!(readPlugin instanceof PluginGroup_UE3)) {
        return null;
      }

      PluginGroup_UE3 ue3Plugin = (PluginGroup_UE3) readPlugin;

      // 4 - File ID Number? (incremental from 0) (thie is NOT an ID into the Names Directory!)
      fm.skip(4);

      // for each property (until Property Name ID = "None")
      //   8 - Property Name ID
      //   8 - Property Type ID
      //   8 - Property Length
      //   X - Property Data
      UnrealProperty[] properties = ue3Plugin.readProperties(fm);

      // Lets check here to see that we have a valid format and width/height for the image data
      String imageFormat = null;
      int imageWidth = 0;
      int imageHeight = 0;
      String textureFilename = null;
      int numProperties = properties.length;
      for (int p = 0; p < numProperties; p++) {
        UnrealProperty property = properties[p];
        String name = property.getName();
        if (name.equals("Format")) {
          long formatNameID = (Long) property.getValue();
          String formatName = ue3Plugin.getName(formatNameID);
          if (formatName != null) {
            if (formatName.equals("PF_DXT5")) {
              imageFormat = "DXT5";
            }
            else if (formatName.equals("PF_DXT1")) {
              imageFormat = "DXT1";
            }
            else if (formatName.equals("PF_DXT3")) {
              imageFormat = "DXT3";
            }
            else if (formatName.equals("PF_G8")) {
              imageFormat = "G8";
            }
            else if (formatName.equals("PF_A8R8G8B8")) {
              imageFormat = "ARGB";
            }
            else {
              ErrorLogger.log("[Viewer_UE3_Texture2D_576] Unsupported Image Format: " + formatName);
              return null;
            }
          }
        }
        else if (name.equals("SizeX")) {
          imageWidth = (Integer) property.getValue();
        }
        else if (name.equals("SizeY")) {
          imageHeight = (Integer) property.getValue();
        }
        else if (name.equals("TextureFileCacheName")) {
          textureFilename = (String) property.getValue();
        }
      }

      if (imageFormat == null || imageWidth == 0 || imageHeight == 0) {
        return null;
      }

      // 8 - null
      // 4 - null
      // 4 - Unknown
      fm.skip(16);

      // 4 - Number of Mipmaps (10)
      int numMipmaps = fm.readInt();
      FieldValidator.checkRange(numMipmaps, 0, 50);

      for (int m = 0; m < numMipmaps; m++) {
        // 4 - Location Flag (33/17=External File, 0=This File)
        int imageLocation = fm.readInt();

        // 4 - Decompressed File Length (in External File)
        int extDecompLength = fm.readInt();

        // 4 - Compressed File Length (in External File)
        int extCompLength = fm.readInt();

        // 4 - File Offset (in External File)
        int extOffset = fm.readInt();

        if (imageLocation == 0) {
          // we've found an image, it's in this archive at the current pointer position

          // X - Pixel data
          byte[] pixels = fm.readBytes(extCompLength);

          if (m != 0) {
            // 12 - null
            // 4 - Unknown
            fm.skip(16);

            // 4 - Image Width
            imageWidth = fm.readInt();
            FieldValidator.checkWidth(imageWidth);

            // 4 - Image Height
            imageHeight = fm.readInt();
            FieldValidator.checkHeight(imageHeight);
          }

          // wrap the pixels in a FileManipulator for processing further down
          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixels));

          break; // exit the loop - we have an image to view
        }

        // 12 - null
        // 4 - Unknown
        fm.skip(16);

        // 4 - Image Width
        imageWidth = fm.readInt();
        FieldValidator.checkWidth(imageWidth);

        // 4 - Image Height
        imageHeight = fm.readInt();
        FieldValidator.checkHeight(imageHeight);

        if (extOffset != -1 && extCompLength != -1 && extDecompLength != 0) {
          // we've found an image, it's in the external archive

          // Find the external
          Resource selected = (Resource) SingletonManager.get("CurrentResource");
          if (selected == null) {
            return null;
          }

          // Check that we can find the external textureFilename
          File textureFile = new File(FilenameSplitter.getDirectory(selected.getSource()) + File.separator + textureFilename + ".tfc");
          if (!textureFile.exists()) {
            return null;
          }

          long textureFileLength = textureFile.length();

          FieldValidator.checkLength(extDecompLength);
          FieldValidator.checkLength(extCompLength, textureFileLength);
          FieldValidator.checkOffset(extOffset, textureFileLength);

          // open the TextureFile for processing
          fm.close();
          fm = new FileManipulator(textureFile, false);
          fm.seek(extOffset);

          break; // exit the loop - we have an image to view
        }

      }

      long currentOffset = fm.getOffset();

      if (fm.readByte() == -63 && fm.readByte() == -125 && fm.readByte() == 42 && fm.readByte() == -98) {
        // the Unreal Header - the data is compressed?
        long arcSize = fm.getLength();

        // 2 - null
        fm.skip(2);

        // 2 - Compression Type? (1=ZLib, 2 = LZO, 4=LZX) (https://github.com/gildor2/UModel/blob/master/Unreal/UnCore.h)
        short compressionType = fm.readShort();
        if (compressionType != 1 && compressionType != 2) {
          // unsupported compression
          ErrorLogger.log("[Viewer_UE3_Texture2D_507] Unsupported compression: " + compressionType);
          return null;
        }

        // 4 - Compressed Length
        fm.skip(4);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);
        byte[] fileData = new byte[decompLength];

        // get the decomp and comp lengths for each block
        int decompWritePos = 0;
        int maxNumBlocks = 1000; // hard limit of 1000 blocks for a compressed file
        int realNumBlocks = 0;
        int[] decompBlocks = new int[maxNumBlocks];
        int[] compBlocks = new int[maxNumBlocks];
        while (decompWritePos < decompLength) {
          // 4 - Compressed Block Length
          int compBlock = fm.readInt();
          FieldValidator.checkLength(compBlock, arcSize);
          compBlocks[realNumBlocks] = compBlock;

          // 4 - Decompressed Block Length
          int decompBlock = fm.readInt();
          FieldValidator.checkLength(decompBlock);
          decompBlocks[realNumBlocks] = decompBlock;

          decompWritePos += decompBlock;
          realNumBlocks++;
        }

        // reset the write position for the decompressed data
        decompWritePos = 0;
        for (int d = 0; d < realNumBlocks; d++) {
          int decompBlock = decompBlocks[d];
          int compBlock = compBlocks[d];

          // X - Compressed File Data
          ExporterPlugin exporter = null;
          if (compressionType == 1) {
            exporter = Exporter_ZLib.getInstance();
            ((Exporter_ZLib) exporter).open(fm, compBlock, decompBlock);
          }
          else if (compressionType == 2) {
            exporter = Exporter_LZO_SingleBlock.getInstance();
            ((Exporter_LZO_SingleBlock) exporter).setForceDecompress(true); // always decompress - there are no "raw" blocks
            ((Exporter_LZO_SingleBlock) exporter).open(fm, compBlock, decompBlock);
          }

          for (int b = 0; b < decompBlock; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              fileData[decompWritePos++] = (byte) exporter.read();
            }
          }
        }

        // open the decompressed file data for processing
        fm.close();
        fm = new FileManipulator(new ByteBuffer(fileData));
      }
      else {
        // raw data - we already have this in an FM, so just return the pointer to the start
        fm.seek(currentOffset);
      }

      // Now process the image data into a real image
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("G8")) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("ARGB")) {
        imageResource = ImageFormatReader.readARGB(fm, imageWidth, imageHeight);
      }
      else {
        return null;// should not get here
      }
      imageResource.setProperty("ImageFormat", imageFormat);

      fm.close();

      return imageResource;

    }
    catch (

    Throwable t) {
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