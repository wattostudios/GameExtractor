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
import org.watto.ge.helper.UE4Helper_2;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_UE4_2;
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
public class Viewer_UE4_2_Texture2D extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_UE4_2_Texture2D() {
    super("UE4_2_Texture2D", "Unreal Engine v4 Texture Image");
    setExtensions("texture2d"); // MUST BE LOWER CASE

    setGames("ARK: Survival Evolved");
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
      if (readPlugin instanceof Plugin_UE4_2) {
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

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      // for each property (until Property Name ID = "None")
      //   8 - Property Name ID
      //   8 - Property Type ID
      //   8 - Property Length
      //   X - Property Data
      UnrealProperty[] properties = UE4Helper_2.readProperties(fm);

      // THIS IS THE STRUCTURE (FOR INFO PURPOSES ONLY)
      // 4 - null
      // 2 - Unknown (1)
      // 2 - Unknown (1)
      // 4 - Unknown (1)
      // 8 - Image Format (names[id] = "PF_BC5", for example)
      // 4 - Unknown
      // 4 - Largest Mipmap Image Width?
      // 4 - Largest Mipmap Image Height?
      // 4 - Unknown (1)
      // 4 - Image Format Name Length (including null terminator)
      // X - Image Format Name ("PF_BC5", for example)
      // 1 - null Image Format Name Terminator
      // 4 - Unknown (0/1)
      // 4 - Mipmap Count
      // 4 - Unknown (1)
      // 4 - Largest Mipmap Data Location (128=In External File, 64=In This File)
      // 4 - Largest Mipmap Image Data Length
      // 4 - Largest Mipmap Image Data Length
      // 8 - Largest Mipmap Image Data Offset (relative to the start of this file?) 

      // for each mipmap (EXCEPT FOR THE FIRST ONE, WHICH IS DONE ABOVE)...
      //   4 - Mipmap Image Width?
      //   4 - Mipmap Image Height?
      //   4 - Unknown (1)
      //   4 - Mipmap Data Location (128=In External File, 64=In This File)
      //   4 - Mipmap Image Data Length
      //   4 - Mipmap Image Data Length
      //   if (mipmap stored in this file){
      //     8 - Junk
      //     X - Mipmap Image Data
      //     }
      //   else {
      //     8 - Mipmap Image Data Offset (relative to the start of this file in the external file?) (ignore if this mipmap is stored here)
      //     }

      // X - Unknown

      // Lets check here to see that we have a valid format and width/height for the image data
      String textureFilename = null;
      int numProperties = properties.length;
      for (int p = 0; p < numProperties; p++) {
        UnrealProperty property = properties[p];
        String name = property.getName();

        if (name.equals("BulkDataFilePath")) {
          textureFilename = (String) property.getValue();
        }
      }

      // 4 - null
      // 2 - Unknown (1)
      // 2 - Unknown (1)
      // 4 - Unknown (1)
      // 8 - Image Format (names[id] = "PF_BC5", for example)
      // 4 - Unknown
      fm.skip(24);

      // 4 - Largest Mipmap Image Width?
      int imageWidth = fm.readInt();
      FieldValidator.checkWidth(imageWidth);

      // 4 - Largest Mipmap Image Height?
      int imageHeight = fm.readInt();
      FieldValidator.checkHeight(imageHeight);

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Image Format Name Length (including null terminator)
      int formatLength = fm.readInt() - 1;
      FieldValidator.checkFilenameLength(formatLength);

      // X - Image Format Name ("PF_BC5", for example)
      String imageFormat = fm.readString(formatLength);

      // 1 - null Image Format Name Terminator
      // 4 - Unknown (0/1)
      fm.skip(5);

      // 4 - Number of Mipmaps
      int numMipmaps = fm.readInt();
      FieldValidator.checkRange(numMipmaps, 0, 50);

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Largest Mipmap Data Location (128=In External File, 64=In This File)
      int imageLocation = fm.readInt();

      // 4 - Largest Mipmap Image Data Length
      int extDecompLength = fm.readInt();

      // 4 - Largest Mipmap Image Data Length
      int extCompLength = fm.readInt();

      // 8 - Largest Mipmap Image Data Offset (relative to the start of this file?) 
      long extOffset = fm.readLong();

      if (imageLocation == 64) {
        // in this file

        // X - Pixel data
        byte[] pixels = fm.readBytes(extCompLength);

        // wrap the pixels in a FileManipulator for processing further down
        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixels));
      }
      else {
        // in the external archive

        // Find the external
        Resource selected = (Resource) SingletonManager.get("CurrentResource");
        if (selected == null) {
          return null;
        }

        // Check that we can find the external textureFilename
        File textureFile = new File(FilenameSplitter.getDirectory(selected.getSource()) + File.separator + textureFilename);
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
          ErrorLogger.log("[Viewer_UE4_2_Texture2D] Unsupported compression: " + compressionType);
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
        long startOffset = fm.getOffset();
        for (int d = 0; d < realNumBlocks; d++) {
          int decompBlock = decompBlocks[d];
          int compBlock = compBlocks[d];

          // X - Compressed File Data
          long offset = fm.getOffset();
          if (compressionType == 2) {
            // check ZLib Compression
            if (fm.readString(1).equals("x")) {
              compressionType = 1; // it's actually ZLib, not LZO
            }
            fm.relativeSeek(offset);
          }

          ExporterPlugin exporter = null;
          if (compressionType == 1) {
            exporter = Exporter_ZLib.getInstance();
            ((Exporter_ZLib) exporter).open(fm, compBlock, decompBlock);
          }
          else if (compressionType == 2) {
            exporter = Exporter_LZO_SingleBlock.getInstance();
            ((Exporter_LZO_SingleBlock) exporter).setForceDecompress(true); // LZO is used, want to force it, there are no "raw" blocks in the image
            ((Exporter_LZO_SingleBlock) exporter).open(fm, compBlock, decompBlock);
          }

          for (int b = 0; b < decompBlock; b++) {
            if (exporter.available()) { // make sure we read the next bit of data, if required
              fileData[decompWritePos++] = (byte) exporter.read();
            }
          }

          startOffset += compBlock;
          fm.seek(startOffset);
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
      if (imageFormat.equals("PF_DXT5")) {
        imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_DXT1")) {
        imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_BC5")) {
        imageResource = ImageFormatReader.readBC5(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_G8")) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_A8R8G8B8")) {
        imageResource = ImageFormatReader.readARGB(fm, imageWidth, imageHeight);
      }
      else if (imageFormat.equals("PF_B8G8R8A8")) {
        imageResource = ImageFormatReader.readBGRA(fm, imageWidth, imageHeight);
      }
      else {
        return null;
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