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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_RSH_RELICCHUNKY;
import org.watto.ge.plugin.archive.Plugin_SGA_ARCHIVE_2;
import org.watto.ge.plugin.archive.Plugin_SGA_ARCHIVE_3;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************
Can read an image that is standalone, or one from within an SGA archive
**********************************************************************************************
**/
public class Viewer_RSH_RELICCHUNKY_DATA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RSH_RELICCHUNKY_DATA() {
    super("RSH_RELICCHUNKY_DATA", "Relic Chunky Image");
    setExtensions("data", "ptld", "tdat", "dxtc", "rgt");

    setGames("Warhammer 40k: Dawn Of War");
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
      if (plugin instanceof Plugin_RSH_RELICCHUNKY || plugin instanceof Plugin_SGA_ARCHIVE_2 || plugin instanceof Plugin_SGA_ARCHIVE_3) {
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

      if (fm.readString(12).equals("Relic Chunky")) {
        // a Relic archive within an SGA archive
        rating += 25;
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

  int imageWidth = 0;

  int imageHeight = 0;

  int imageFormat = 0;

  int archiveVersion = 1;

  int numImages = 0;

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

      int height = 0;
      int width = 0;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        height = Integer.parseInt(resource.getProperty("Height"));
        width = Integer.parseInt(resource.getProperty("Width"));
      }
      catch (Throwable t) {
        // no width/height stored, so not an image resource
        // But, could be a Relic file within an SGA archive, so don't die yet
        //return null;
      }

      if (height == 0 || width == 0) {

        // see if it's in an SGA archive...
        if (fm.readString(12).equals("Relic Chunky")) {
          // Yep, in a SGA archive, so read the image

          // 14 - Header ("Relic Chunky" + (bytes)13,10)
          fm.skip(2); // already read 12 above

          // 2 - Unknown (26)
          fm.skip(2);

          // 4 - Unknown (1)
          archiveVersion = fm.readInt();
          if (archiveVersion == 3) {
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(12);
          }
          // 4 - Unknown (1)
          fm.skip(4);

          ImageResource[] imageResources = new ImageResource[50]; // max 50 images
          numImages = 0;

          // Read all the chunks, then find the largest image and return it
          long arcSize = fm.getLength();
          while (fm.getOffset() < arcSize && numImages < 50) {
            readChunk(fm, imageResources, "");
          }

          if (numImages == 0) {
            return null;
          }

          ImageResource largestImage = null;
          imageWidth = 0;
          imageHeight = 0;

          for (int i = 0; i < numImages; i++) {
            ImageResource imageResource = imageResources[i];
            if (imageResource == null) {
              break;
            }

            if (imageResource.getWidth() > imageWidth || imageResource.getHeight() > imageHeight) {
              largestImage = imageResource;
              imageWidth = imageResource.getWidth();
              imageHeight = imageResource.getHeight();
            }
          }

          if (largestImage != null) {
            fm.close();

            // Flip the image
            largestImage = ImageFormatReader.flipVertically(largestImage);

            return largestImage;
          }

        }
        else {
          // Nope, so some other issue, so die now.
          return null;
        }
      }

      ImageResource imageResource = null;

      String chunkID = resource.getExtension();
      if (chunkID.equals("DATA")) {

        String imageFormat = "";
        try {
          imageFormat = resource.getProperty("ImageFormat");
        }
        catch (Throwable t) {
          // no image format stored, so not an image resource
          return null;
        }

        if (imageFormat == null) {
          return null;
        }

        // X - Pixels
        if (imageFormat.contentEquals("BGRA")) {
          imageResource = ImageFormatReader.readBGRA(fm, width, height);
        }
        else if (imageFormat.contentEquals("DXT1")) {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
        }
        else if (imageFormat.contentEquals("DXT5")) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
        }
        else {
          ErrorLogger.log("[Viewer_RSH_RELICCHUNKY_DATA] Unknown Image Data format: " + imageFormat);
          return null;
        }
      }
      else if (chunkID.equals("PTLD")) {
        // 4 - Image Number in this Group? (incremental from 0)
        // 4 - Image Data Length
        fm.skip(8);

        // X - Pixels (8bit paletted)
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else if (chunkID.equals("TDAT")) {
        int dxtcOffset = 0;
        int dxtcLength = 0;
        int dxtcDecompLength = 0;
        try {
          dxtcOffset = Integer.parseInt(resource.getProperty("Offset"));
          dxtcLength = Integer.parseInt(resource.getProperty("Length"));
          dxtcDecompLength = Integer.parseInt(resource.getProperty("DecompressedLength"));
        }
        catch (Throwable t) {
          // no image details stored
          return null;
        }

        // check that we have offsets and lengths
        if (dxtcLength == 0 && dxtcDecompLength == 0) {
          return null;
        }

        // X - Image Data (ZLib Compression)
        fm.skip(dxtcOffset);

        byte[] compData = fm.readBytes(dxtcLength);
        byte[] decompData = new byte[dxtcDecompLength];

        FileManipulator compFM = new FileManipulator(new ByteBuffer(compData));
        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(compFM, dxtcLength, dxtcDecompLength);
        for (int b = 0; b < dxtcDecompLength; b++) {
          if (exporter.available()) {
            decompData[b] = (byte) exporter.read();
          }
        }
        exporter.close();
        compFM.close();

        FileManipulator decompFM = new FileManipulator(new ByteBuffer(decompData));

        // 4 - null
        decompFM.skip(4);

        // 4 - Image Width
        imageWidth = decompFM.readInt();
        FieldValidator.checkWidth(imageWidth);

        // 4 - Image Height
        imageHeight = decompFM.readInt();
        FieldValidator.checkHeight(imageHeight);

        // 4 - Image Data Length
        decompFM.skip(4);

        // X - Image Data
        if (imageWidth * imageHeight > dxtcDecompLength) {
          // try for DXT1
          imageResource = ImageFormatReader.readDXT1(decompFM, imageWidth, imageHeight);
        }
        else {
          // this is the normal format
          imageResource = ImageFormatReader.readDXT5(decompFM, imageWidth, imageHeight);
        }

        decompFM.close();
      }
      else if (chunkID.equals("DXTC")) {
        String imageFormat = "";
        try {
          imageFormat = resource.getProperty("ImageFormat");
        }
        catch (Throwable t) {
          // no image format stored, so not an image resource
          return null;
        }

        if (imageFormat == null) {
          return null;
        }

        // X - Pixels
        if (imageFormat.contentEquals("DXT1")) {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
        }
        else if (imageFormat.contentEquals("DXT5")) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
        }
        else {
          ErrorLogger.log("[Viewer_RSH_RELICCHUNKY_DATA] Unknown Image Data format: " + imageFormat);
          return null;
        }
      }

      if (imageResource == null) {
        return null;
      }

      fm.close();

      // Flip the image
      imageResource = ImageFormatReader.flipVertically(imageResource);

      return imageResource;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  int dxtcOffset = 0;

  int dxtcLength = 0;

  int dxtcDecompLength = 0;

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void readChunk(FileManipulator fm, ImageResource[] resources, String parentID) {
    try {

      if (numImages >= 50) {
        return; // early break when we have read enough images
      }

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      // 4 - Chunk Type (FOLD, DATA)
      String chunkType = fm.readString(4);

      // 4 - Chunk ID Name
      String chunkID = fm.readString(4);

      // 4 - Version
      //int version = fm.readInt();
      fm.skip(4);

      // 4 - Chunk Length (after the end of the Name field)
      int length = fm.readInt();
      FieldValidator.checkLength(length, fm.getLength());

      // 4 - Chunk Name Length (can be null)
      int nameLength = fm.readInt();

      if (archiveVersion == 3) {
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);
      }

      // X - Chunk Name (optional) (padded with nulls to a multiple of 4 bytes)
      if (nameLength != 0) {
        fm.readNullString(nameLength);
      }

      // X - Chunk Data
      long offset = fm.getOffset();
      if (chunkType.equals("FOLD")) {
        // folder (FOLD) - read nested chunks
        long endOffset = offset + length;
        while (fm.getOffset() < endOffset) {
          readChunk(fm, resources, chunkID);
        }
      }
      else {
        // file (DATA) - store the file

        // Now comes some specific handlers for the chunkID...
        if (chunkID.equals("ATTR") && parentID.equals("IMAG")) {
          // 4 - Image Format
          imageFormat = fm.readInt();

          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);

          if (archiveVersion != 3) {
            // 4 - Mipmap Count
            fm.skip(4);
          }
        }
        else if (chunkID.equals("DATA") && parentID.equals("IMAG")) {
          // this is an image 

          // X - Image Data
          ImageResource imageResource = null;

          if (imageFormat == 0) { // BGRA
            imageResource = ImageFormatReader.readBGRA(fm, imageWidth, imageHeight);
          }
          else if (imageFormat == 8) { // DXT1
            imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
          }
          else if (imageFormat == 11) { // DXT5
            imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
          }
          else {
            fm.skip(length);
          }

          if (imageResource != null) {
            resources[numImages] = imageResource;
            numImages++;
          }

        }
        else if (chunkID.equals("INFO") && parentID.equals("TPAT")) {
          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);
        }
        else if (chunkID.equals("PTLD") && parentID.equals("TPAT")) {
          // this is an image

          // 4 - Image Number in this Group? (incremental from 0)
          // 4 - Image Data Length
          fm.skip(8);

          // X - Pixels (8bit paletted)
          ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, imageWidth, imageHeight);

          if (imageResource != null) {
            resources[numImages] = imageResource;
            numImages++;
          }

        }
        else if (chunkID.equals("TMAN") && parentID.equals("DXTC")) {
          dxtcOffset = 0;
          dxtcLength = 0;
          dxtcDecompLength = 0;

          // 4 - Number of Chunks
          fm.skip(4);

          // skip to the biggest chunk
          int numChunks = (length - 4) / 8;
          int largestOffset = 0;
          for (int c = 0; c < numChunks - 1; c++) {
            // 4 - Chunk Decompressed Length
            fm.skip(4);

            // 4 - Chunk Compressed Length
            int chunkLength = fm.readInt();
            FieldValidator.checkLength(chunkLength);
            largestOffset += chunkLength;
          }

          // now we're at the details for the largest chunk

          // 4 - Chunk Decompressed Length
          int largestDecompLength = fm.readInt();
          FieldValidator.checkLength(largestDecompLength);

          // 4 - Chunk Compressed Length
          int largestLength = fm.readInt();
          FieldValidator.checkLength(largestLength);

          dxtcOffset = largestOffset;
          dxtcLength = largestLength;
          dxtcDecompLength = largestDecompLength;
        }
        else if (chunkID.equals("TDAT") && parentID.equals("DXTC")) {
          // this is an image

          // check that we have offsets and lengths
          if (dxtcLength == 0 && dxtcDecompLength == 0) {
            fm.skip(length);
          }
          else {

            // X - Image Data (ZLib Compression)
            fm.skip(dxtcOffset);

            byte[] compData = fm.readBytes(dxtcLength);
            byte[] decompData = new byte[dxtcDecompLength];

            FileManipulator compFM = new FileManipulator(new ByteBuffer(compData));
            Exporter_ZLib exporter = Exporter_ZLib.getInstance();
            exporter.open(compFM, dxtcLength, dxtcDecompLength);
            for (int b = 0; b < dxtcDecompLength; b++) {
              if (exporter.available()) {
                decompData[b] = (byte) exporter.read();
              }
            }
            exporter.close();
            compFM.close();

            FileManipulator decompFM = new FileManipulator(new ByteBuffer(decompData));

            // 4 - null
            decompFM.skip(4);

            // 4 - Image Width
            imageWidth = decompFM.readInt();
            FieldValidator.checkWidth(imageWidth);

            // 4 - Image Height
            imageHeight = decompFM.readInt();
            FieldValidator.checkHeight(imageHeight);

            // 4 - Image Data Length
            decompFM.skip(4);

            // X - Image Data
            ImageResource imageResource = null;
            if (imageWidth * imageHeight > dxtcDecompLength) {
              // try for DXT1
              imageResource = ImageFormatReader.readDXT1(decompFM, imageWidth, imageHeight);
            }
            else {
              // this is the normal format
              imageResource = ImageFormatReader.readDXT5(decompFM, imageWidth, imageHeight);
            }

            decompFM.close();

            if (imageResource != null) {
              resources[numImages] = imageResource;
              numImages++;
            }
          }

        }
        else if (chunkID.equals("DXTC") && parentID.equals("TXTR")) {
          // this is an image - store the image height and width

          // 4 - Image Width
          imageWidth = fm.readInt();
          FieldValidator.checkWidth(imageWidth);

          // 4 - Image Height
          imageHeight = fm.readInt();
          FieldValidator.checkHeight(imageHeight);

          // 4 - Unknown (1)
          // 4 - Unknown (2)
          fm.skip(8);

          // 4 - Image Format (13=DXT1, 15=DXT5)
          int imageFormat = fm.readInt();

          // the largest mipmap is last in the file. So, set the offset at the appropriate place
          int mipmapSize = imageWidth * imageHeight;
          if (imageFormat == 13) {
            mipmapSize /= 2; // DXT1
          }
          int skipSize = length - mipmapSize - 20; // we already read 20 bytes
          FieldValidator.checkLength(skipSize);
          fm.skip(skipSize);

          ImageResource imageResource = null;
          if (imageFormat == 13) {
            imageResource = ImageFormatReader.readDXT1(fm, imageWidth, imageHeight);
          }
          else if (imageFormat == 15) {
            imageResource = ImageFormatReader.readDXT5(fm, imageWidth, imageHeight);
          }
          else {
            fm.skip(mipmapSize);
          }

          if (imageResource != null) {
            resources[numImages] = imageResource;
            numImages++;
          }

        }
        else {
          fm.skip(length);
        }

      }

    }
    catch (Throwable t) {
      logError(t);
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