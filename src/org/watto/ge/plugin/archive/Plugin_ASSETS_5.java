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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.Unity3DHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_Unity3D_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASSETS_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_5() {

    super("ASSETS_5", "Unity3D Engine Resource (Version 5)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("StuntMANIA!Jnr",
        "The Graveyard");
    setExtensions("assets"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(Unity3DHelper.getFileTypes());
    setTextPreviewExtensions("textasset"); // LOWER CASE

  }

  /**
   **********************************************************************************************
   *
   **********************************************************************************************
   **/
  public String convertFileType(int fileTypeCode) {
    return Unity3DHelper.getFileExtension(fileTypeCode);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      // 4 - File Details Directory Length (BIG ENDIAN)
      long dirOffset = arcSize - IntConverter.changeFormat(fm.readInt());
      if (FieldValidator.checkOffset(dirOffset, arcSize)) {
        rating += 5;
      }

      // 4 - Archive Length (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // 4 - Version (5) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 5) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Length of Everything from the TYPES DIRECTORY to the End of the Archive (BIG)
      long dirOffset = arcSize - IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 1 - null
      fm.skip(1);

      // 4 - Number of Blocks? (6)
      int blockCount = fm.readInt();
      FieldValidator.checkNumFiles(blockCount);

      readTypes(fm, blockCount, "", "", 0);

      /*
      for (int b = 0; b < blockCount; b++) {
        // 4 - Number of Types in this Block
        int numTypes = fm.readInt();
        FieldValidator.checkNumFiles(numTypes);
      
        readTypes(fm, numTypes, "", "", 0);
      }
      */

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // FILES DIRECTORY
      // for each file (20 bytes per entry)
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID (incremental from 1)
        fm.skip(4);

        // 4 - File Offset (points to the FilenameLength field for each file)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Type Code
        int fileTypeCode = fm.readInt();

        // 4 - File Type Code
        fm.skip(4);

        String fileType = convertFileType(fileTypeCode);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, fileType, offset, length);

        TaskProgressManager.setValue(i);
      }

      //
      // In this loop...
      // * Get the filenames for each file
      // * If a SND or TEX Resource has its data in an external archive, point to it instead
      //
      TaskProgressManager.setValue(0);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String fileType = resource.getName();

        // Go to the data offset
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength <= 0) {
          resource.setName(Resource.generateFilename(i) + fileType);
          resource.setOriginalName(resource.getName());
          continue;
        }
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        if (fileType.equals(".GameObject")) { // Game Objects don't have real filenames
          filename = Resource.generateFilename(i);
        }
        filename += fileType;

        resource.setName(filename);
        resource.setOriginalName(filename);

        long realOffset = fm.getOffset();
        long realSize = resource.getLength() - (realOffset - offset);

        if (fileType.equals(".AudioClip")) {
          try {
            // 1 - null
            fm.skip(1);

            // 4 - Unknown (5)
            // 4 - m_Format (2)
            // 4 - m_Type (14=OGG, 20=WAV)
            // 2 - m_3D (1)
            // 2 - null Padding
            // 4 - Sound Data Length
            fm.skip(20);

            // X - File Data

            String audioHeader = fm.readString(4);
            if (audioHeader.equals("RIFF")) {
              resource.setExtension("wav");
              resource.setOriginalName(resource.getName());
            }
            else if (audioHeader.equals("OggS")) {
              resource.setExtension("ogg");
              resource.setOriginalName(resource.getName());
            }

            realOffset += 21;
            realSize -= 21;

          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Texture2D")) {
          try {
            // 4 - Width
            int imageWidth = fm.readInt();

            // 4 - Height
            int imageHeight = fm.readInt();

            // 4 - File Size
            fm.skip(4);

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 1 - m_MipMap (1)
            // 4 - m_ImageCount (1)
            // 4 - m_TextureDimension (2)
            // 4 - m_FilterMode (2/1)
            // 4 - m_Aniso (1/0)
            // 4 - m_MipBias
            // 4 - m_WrapMode
            // 4 - Image Data Length

            // X - Image Data
            realOffset += 45;
            realSize -= 45;

            int mipmapCount = 1; // just force it for previewing

            // Convert the Resource into a Resource_Unity3D_TEX
            Resource oldResource = resource;
            resource = new Resource_Unity3D_TEX();
            resource.copyFrom(oldResource); // copy the data from the old Resource to the new Resource
            resources[i] = resource; // stick the new Resource in the array, overwriting the old Resource

            // Set the image-specific properties on the new Resource
            Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;
            castResource.setImageWidth(imageWidth);
            castResource.setImageHeight(imageHeight);
            castResource.setFormatCode(imageFormat);
            castResource.setMipmapCount(mipmapCount);

          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }

        resource.setOffset(realOffset);
        resource.setLength(realSize);
        resource.setDecompressedLength(realSize);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;
      //return realResources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   *
   **********************************************************************************************
   **/
  public void readTypes(FileManipulator fm, int numTypes, String parentClassType, String parentName, int level) {
    for (int t = 0; t < numTypes; t++) {
      if (level == 0) {
        // 4 - Type ID Number
        fm.skip(4);
        //System.out.println("Type ID Number: " + fm.readInt());
      }

      // X - Class/Type (eg "SInt32", "string", "Array")
      String classType = fm.readNullString();

      // X - Name (eg "m_Format", "m_PathName", "m_BitSize")
      String name = fm.readNullString();

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(20);

      // 4 - Nested Property Count
      int nestedCount = fm.readInt();

      /*
      String levelPadding = "";
      for (int p = 0; p < level; p++) {
        levelPadding += "\t";
      }
      System.out.println(levelPadding + classType + "\t" + name);
      */

      readTypes(fm, nestedCount, classType, name, level + 1);
    }

  }

}
