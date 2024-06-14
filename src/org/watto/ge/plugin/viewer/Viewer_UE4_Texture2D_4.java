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

import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.UE4Helper_4;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PAK_38;
import org.watto.ge.plugin.archive.Plugin_UE4_6;
import org.watto.ge.plugin.archive.datatype.UnrealImportEntry;
import org.watto.ge.plugin.resource.Resource_PAK_38;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.Task_ExportFiles;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_UE4_Texture2D_4 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_UE4_Texture2D_4() {
    super("UE4_Texture2D_4", "Unreal Engine 4 Version 4 Texture Image");
    setExtensions("texture2d", "uasset"); // MUST BE LOWER CASE

    setGames("Unreal Engine 4 Version 4",
        "Dispatcher");
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
  If a related resource is found with the same name and matching fileExtension, it is extracted
  either to a bytebuffer or to a temporary directory
  **********************************************************************************************
  **/
  public FileManipulator extractRelatedResource(FileManipulator fm, String fileExtension, boolean extractToMemory) {
    try {

      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected != null && selected instanceof Resource_PAK_38) {

        String uexpName = selected.getName();
        int dotPos = uexpName.lastIndexOf(".uasset");
        if (dotPos > 0) {
          uexpName = uexpName.substring(0, dotPos + 1) + fileExtension;

          // search for the related file
          Resource[] resources = ((Resource_PAK_38) selected).getRelatedResources();
          int numResources = resources.length;

          for (int i = 0; i < numResources; i++) {
            if (resources[i].getName().equals(uexpName)) {
              // found the uexp file
              Resource relatedResource = resources[i];

              // extract it
              if (extractToMemory) {
                // to a bytebuffer

                fm.close();

                fm = new FileManipulator(relatedResource.getSource(), false);
                fm.seek(relatedResource.getOffset());
                byte[] fileData = fm.readBytes((int) relatedResource.getLength());

                fm.close();
                fm = new FileManipulator(new ByteBuffer(fileData));
                return fm;
              }
              else {
                // to a file
                File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

                Task_ExportFiles exportTask = new Task_ExportFiles(directory, relatedResource);
                exportTask.setShowPopups(false);
                exportTask.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
                exportTask.redo();

                File path = relatedResource.getExportedPath();

                fm.close();
                fm = new FileManipulator(path, false);

                return fm;
              }

            }
          }
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
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      // PAK_38 for the original archive, UE4_6 for exported uasset files
      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_PAK_38 || readPlugin instanceof Plugin_UE4_6) {
        rating += 50;
      }

      // Check for a UE4 archives with Version = 4
      int helperRating = UE4Helper_4.getMatchRating(fm, 4);
      if (helperRating > rating) {
        rating = helperRating;
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

      // FIRST UP, WE NEED TO READ THROUGH TO DISCOVER IF THIS IS A TEXTURE2D UASSET OR NOT.
      // IF NOT, WE RETURN NULL AT THAT POINT, HANDING OVER TO ANOTHER VIEWER PLUGIN

      long arcSize = fm.getLength();

      // 4 - Unreal Header (193,131,42,158)
      // 4 - Version (4) (XOR with 255)
      // 16 - null
      fm.skip(24);

      // 4 - File Directory Offset?
      long filesDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(filesDirOffset, arcSize + 1);

      if (filesDirOffset == 712) {
        System.out.println("HERE");
      }

      // 4 - Unknown (5)
      // 4 - Package Name (None)
      // 4 - null
      // 1 - Unknown (128)
      fm.skip(13);

      // 4 - Number of Names
      int nameCount = fm.readInt();
      FieldValidator.checkNumFiles(nameCount);

      // 4 - Name Directory Offset
      long nameDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Number Of Exports
      // 4 - Exports Directory Offset
      if (fm.readInt() == 0) {
        fm.skip(12);
      }
      else {
        fm.skip(4);
      }

      // 4 - Number Of Imports
      int importCount = fm.readInt();
      FieldValidator.checkNumFiles(importCount);

      // 4 - Import Directory Offset
      long importDirOffset = IntConverter.unsign(fm.readInt());
      FieldValidator.checkOffset(importDirOffset, arcSize);

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 4 - null
      // 16 - GUID Hash
      // 4 - Unknown (1)
      // 4 - Unknown (1/2)
      // 4 - Unknown (Number of Names - again?)
      // 36 - null
      // 4 - Unknown
      // 4 - null
      // 4 - null
      // 4 - File Length [+4] (not always - sometimes an unknown length/offset)
      // 12 - null

      // Read the Names Directory
      fm.relativeSeek(nameDirOffset);
      UE4Helper_4.readNamesDirectory(fm, nameCount);

      // Read the Import Directory
      fm.relativeSeek(importDirOffset);
      UnrealImportEntry[] imports = UE4Helper_4.readImportDirectory(fm, importCount);

      int numFiles = importCount;

      boolean foundTexture2DClass = false;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        UnrealImportEntry entry = imports[i];

        if (entry.getType().equals("Class") && entry.getName().equals("Texture2D")) {
          // found a Class, and its a Texture2D, so we're happy
          foundTexture2DClass = true;
          break;
        }

      }

      // If we haven't found a Texture2D Class, return null and let another ViewerPlugin handle it
      if (!foundTexture2DClass) {
        return null;
      }

      // NOW THAT HE HAVE A TEXTURE2D UASSET, WE CAN GO TO THE FILE DATA, SKIP OVER THE UNREAL PROPERTIES,
      // AND GENERATE THE TEXTURE IMAGE FOR DISPLAY

      // First up, see if the uassets file has the Exports in it, or if it's been put in a separate *.uexp file
      if (filesDirOffset + 8 == arcSize) {
        // probably in a separate *.uexp file - see if we can find one
        FileManipulator extractedFM = extractRelatedResource(fm, "uexp", true);
        if (extractedFM != null) {
          fm = extractedFM;
          filesDirOffset = 0; // so when we seek down further, it goes to the start of the uexp file
          arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
        }
      }

      fm.relativeSeek(filesDirOffset);
      UE4Helper_4.readProperties(fm); // discard all this - we don't need it, we just need to get passed it all to find the image data

      // 4 - null
      fm.skip(4);

      // 2 - Flags (1/3)
      // 2 - Flags (1/0)
      // 4 - Unknown (1)
      fm.skip(8);

      // 8 - Type ID (points to "PF_DXT5" or "PF_B8G8R8A8" for a Texture)
      long typeID = fm.readLong();
      String type = UE4Helper_4.getName(typeID);

      if (type.equals("PF_DXT5") || type.equals("PF_DXT1") || type.equals("PF_DXT3")) {
        // 4 - Offset to Largest Mipmap [+8]
        long largestMipmapOffset = IntConverter.unsign(fm.readInt()) + 8;
        FieldValidator.checkOffset(largestMipmapOffset, arcSize);

        // 4 - Original Image Width/Height
        // 4 - Original Image Width/Height
        // 4 - Unknown (1)
        // 4 - Type Name Length (including null) (8)
        // 7 - Type Name (PF_DXT5)
        // 1 - null Type Name Terminator
        // 4 - null
        fm.skip(28);

        // 4 - Mipmap Count
        int mipmapCount = fm.readInt();

        // 4 - Entry Indicator (1)
        fm.skip(4);

        // 4 - Flags (1025/72)
        int flags = fm.readInt();

        // 4 - Length of Mipmap Data
        int dataLength = fm.readInt();
        //FieldValidator.checkLength(dataLength, arcSize);

        // 4 - Length of Mipmap Data
        // 8 - Relative Offset to this mipmap (relative to the start of the largest mipmap data)
        fm.skip(12);

        if (flags == 72) {
          FieldValidator.checkLength(dataLength, arcSize);

          // X - DXT5/3/1 Pixel Data
          // want to skip over the mipmap data temporarily, so we can grab the width and height
          largestMipmapOffset = fm.getOffset();
          fm.skip(dataLength);
        }

        // 4 - This Mipmap Width/Height
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - This Mipmap Width/Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        // Go to the Largest Mipmap and read it (or, if the file had flag=72, it'll take them back to that pixel data)

        // First up, see if the data has been stored in a separate *.ubulk file
        if (largestMipmapOffset + 4 == arcSize) {
          // probably in a separate *.ubulk file - see if we can find one
          FileManipulator extractedFM = extractRelatedResource(fm, "ubulk", true);
          if (extractedFM != null) {
            fm = extractedFM;
            largestMipmapOffset = 0; // so when we seek down further, it goes to the start of the ubulk file
            arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
          }
        }

        FieldValidator.checkLength(dataLength, arcSize); // delayed from above

        fm.relativeSeek(largestMipmapOffset);

        ImageResource imageResource = null;

        if (type.equals("PF_DXT5")) {
          imageResource = ImageFormatReader.readDXT5(fm, width, height);
          imageResource.setProperty("ImageFormat", "DXT5");
        }
        else if (type.equals("PF_DXT1")) {
          imageResource = ImageFormatReader.readDXT1(fm, width, height);
          imageResource.setProperty("ImageFormat", "DXT1");
        }
        else if (type.equals("PF_DXT3")) {
          imageResource = ImageFormatReader.readDXT3(fm, width, height);
          imageResource.setProperty("ImageFormat", "DXT3");
        }

        imageResource.setProperty("MipmapCount", "" + mipmapCount);

        fm.close();

        return imageResource;
      }
      else if (type.equals("PF_B8G8R8A8")) {
        // 4 - Offset to Largest Mipmap [+8]
        long largestMipmapOffset = IntConverter.unsign(fm.readInt()) + 8;
        FieldValidator.checkOffset(largestMipmapOffset, arcSize);

        // 4 - Original Image Width/Height
        // 4 - Original Image Width/Height
        // 4 - Unknown (1)
        // 4 - Type Name Length (including null) (12)
        // 11 - Type Name (PF_B8G8R8A8)
        // 1 - null Type Name Terminator
        // 4 - null
        fm.skip(32);

        // 4 - Mipmap Count (1)
        int mipmapCount = fm.readInt();

        // 4 - Entry Indicator (1)
        fm.skip(4);

        // 4 - Flags (1025/72)
        int flags = fm.readInt();

        // 4 - Length of Mipmap Data
        int dataLength = fm.readInt();
        FieldValidator.checkLength(dataLength, arcSize);

        // 4 - Length of Mipmap Data
        // 8 - Relative Offset to this mipmap (relative to the start of the largest mipmap data)
        fm.skip(12);

        if (flags == 72) {
          // X - BGRA Pixel Data
          // want to skip over the mipmap data temporarily, so we can grab the width and height
          largestMipmapOffset = fm.getOffset();
          fm.skip(dataLength);
        }

        // 4 - This Mipmap Width/Height
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - This Mipmap Width/Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        // Go to the Largest Mipmap and read it

        // First up, see if the data has been stored in a separate *.ubulk file
        if (largestMipmapOffset + 4 == arcSize) {
          // probably in a separate *.ubulk file - see if we can find one
          FileManipulator extractedFM = extractRelatedResource(fm, "ubulk", true);
          if (extractedFM != null) {
            fm = extractedFM;
            largestMipmapOffset = 0; // so when we seek down further, it goes to the start of the ubulk file
            arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
          }
        }

        fm.relativeSeek(largestMipmapOffset);

        ImageResource imageResource = ImageFormatReader.readBGRA(fm, width, height);
        imageResource.setProperty("ImageFormat", "BGRA");
        imageResource.setProperty("MipmapCount", "" + mipmapCount);

        fm.close();

        return imageResource;
      }
      else if (type.equals("PF_BC5")) {
        // 4 - Offset to Largest Mipmap [+8]
        long largestMipmapOffset = IntConverter.unsign(fm.readInt()) + 8;
        FieldValidator.checkOffset(largestMipmapOffset, arcSize);

        // 4 - Original Image Width/Height
        // 4 - Original Image Width/Height
        // 4 - Unknown (1)
        // 4 - Type Name Length (including null) (8)
        // 6 - Type Name (PF_BC5)
        // 1 - null Type Name Terminator
        // 4 - null
        fm.skip(27);

        // 4 - Mipmap Count
        int mipmapCount = fm.readInt();

        // 4 - Entry Indicator (1)
        // 4 - Flags (1025)
        // 4 - Length of Mipmap Data
        // 4 - Length of Mipmap Data
        // 8 - Relative Offset to this mipmap (relative to the start of the largest mipmap data)
        fm.skip(24);

        // 4 - This Mipmap Width/Height
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - This Mipmap Width/Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        // Go to the Largest Mipmap and read it

        // First up, see if the data has been stored in a separate *.ubulk file
        if (largestMipmapOffset + 4 == arcSize) {
          // probably in a separate *.ubulk file - see if we can find one
          FileManipulator extractedFM = extractRelatedResource(fm, "ubulk", true);
          if (extractedFM != null) {
            fm = extractedFM;
            largestMipmapOffset = 0; // so when we seek down further, it goes to the start of the ubulk file
            arcSize += fm.getLength(); // add the size of this file to the size of the uassets file
          }
        }

        fm.relativeSeek(largestMipmapOffset);

        ImageResource imageResource = ImageFormatReader.readBC5(fm, width, height);
        imageResource.setProperty("ImageFormat", "BC5");
        imageResource.setProperty("MipmapCount", "" + mipmapCount);

        fm.close();

        return imageResource;
      }
      else if (type.equals("PF_G8")) {
        // 4 - Length of Image Data?
        fm.skip(4);

        // 4 - Image Width/Height
        int width = fm.readInt();
        FieldValidator.checkNumFiles(width);

        // 4 - Image Width/Height
        int height = fm.readInt();
        FieldValidator.checkNumFiles(height);

        // 4 - Unknown (1)
        // 4 - Type Name Length (including null) (6)
        // 5 - Type Name (PF_G8)
        // 1 - null Type Name Terminator
        // 4 - null
        fm.skip(18);

        // 4 - Mipmap Count (1)
        int mipmapCount = fm.readInt();

        // 4 - Unknown (1)
        // 4 - Flags? (72)
        // 4 - Length of Mipmap Data
        // 4 - Length of Mipmap Data
        fm.skip(16);

        // 8 - Offset to start of Mipmap Data (absolute offset)
        long mipmapOffset = fm.readLong();
        FieldValidator.checkOffset(mipmapOffset, arcSize);

        // X - Greyscale Pixel Data
        fm.relativeSeek(mipmapOffset);

        ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.setProperty("ImageFormat", "G8");
        imageResource.setProperty("MipmapCount", "" + mipmapCount);

        fm.close();

        return imageResource;
      }
      System.out.println("Viewer_UE4_Texture2D_4: Unknown Texture2D Format: " + type);
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
  }
}