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
import java.io.FileNotFoundException;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.Unity3DHelper;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.resource.Resource_Unity3D_TEX;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASSETS_14 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_14() {

    super("ASSETS_14", "Unity3D Engine Resource (Version 14)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Prehistoric Tales");
    setExtensions("assets"); // MUST BE LOWER CASE
    setPlatforms("PC");

    /*
    setFileTypes(new FileType("obj", "Game Object", FileType.TYPE_OTHER), // ALL LOWER CASE!!!
        new FileType("ref", "Transform", FileType.TYPE_OTHER),
        new FileType("particleanim", "Particle Animator", FileType.TYPE_OTHER),
        new FileType("particleemit", "Ellipsoid Particle Emitter", FileType.TYPE_OTHER),
        new FileType("camera", "Camera", FileType.TYPE_OTHER),
        new FileType("mat", "Material", FileType.TYPE_OTHER),
        new FileType("renderer", "Renderer", FileType.TYPE_OTHER),
        new FileType("particlerend", "Particle Renderer", FileType.TYPE_OTHER),
        new FileType("tex", "Texture2D", FileType.TYPE_IMAGE),
        new FileType("meshfilter", "Mesh Filter", FileType.TYPE_OTHER),
        new FileType("msh", "Mesh", FileType.TYPE_OTHER),
        new FileType("shader", "Shader", FileType.TYPE_OTHER),
        new FileType("txt", "Document", FileType.TYPE_OTHER),
        new FileType("body", "Rigidbody", FileType.TYPE_OTHER),
        new FileType("boxcoll", "Box Collider", FileType.TYPE_OTHER),
        new FileType("ani", "Animation Clip", FileType.TYPE_OTHER),
        new FileType("worldcoll", "World Particle Collider", FileType.TYPE_OTHER),
        new FileType("audiosrc", "Audio Source", FileType.TYPE_OTHER),
        new FileType("snd", "Audio Clip", FileType.TYPE_AUDIO),
        new FileType("rendtex", "Render Texture", FileType.TYPE_OTHER),
        new FileType("trailrend", "Trail Renderer", FileType.TYPE_OTHER),
        new FileType("textmesh", "Text Mesh", FileType.TYPE_OTHER),
        new FileType("anim", "Animation", FileType.TYPE_OTHER),
        new FileType("script", "Mono Script", FileType.TYPE_OTHER),
        new FileType("ttf", "Font", FileType.TYPE_OTHER),
        new FileType("physic", "Physic Material", FileType.TYPE_OTHER),
        new FileType("spherecoll", "Sphere Collider", FileType.TYPE_OTHER),
        new FileType("capsulecoll", "Capsule Collider", FileType.TYPE_OTHER),
        new FileType("skinmeshrend", "Skinned Mesh Renderer", FileType.TYPE_OTHER),
        new FileType("bin", "Preload Data", FileType.TYPE_OTHER),
        new FileType("joint", "Configurable Joint", FileType.TYPE_OTHER),
        new FileType("behaviour", "Mono Behaviour", FileType.TYPE_OTHER),
        new FileType("assetbundle", "Asset Bundle", FileType.TYPE_OTHER),
        new FileType("avatar", "Avatar", FileType.TYPE_OTHER),
        new FileType("animator", "Animator", FileType.TYPE_OTHER),
        new FileType("particlesystem", "Particle System", FileType.TYPE_OTHER),
        new FileType("particlesystemrenderer", "Particle System Renderer", FileType.TYPE_OTHER));
    */
    setFileTypes(Unity3DHelper.getFileTypes());
    setTextPreviewExtensions("textasset"); // LOWER CASE

  }

  /**
   **********************************************************************************************
   *
   **********************************************************************************************
   **/
  public String convertFileType(int fileTypeCode) {
    /*
    if (fileTypeCode == 4) {
      return ".ref";
    }
    else if (fileTypeCode == 21) {
      return ".mat";
    }
    else if (fileTypeCode == 23) {
      return ".matref";
    }
    else if (fileTypeCode == 28) {
      return ".tex";
    }
    else if (fileTypeCode == 33) {
      return ".mshref";
    }
    else if (fileTypeCode == 43) {
      return ".msh";
    }
    else if (fileTypeCode == 48) {
      return ".shader";
    }
    else if (fileTypeCode == 49) {
      return ".txt"; // Document - sometimes plain text, sometimes XML, sometimes HTML, sometimes JSON...
    }
    else if (fileTypeCode == 74) {
      return ".ani";
    }
    else if (fileTypeCode == 83) {
      return ".snd"; // Sound - can be ogg, wav, mp3, aif, ...
    }
    else if (fileTypeCode == 115) {
      return ".script";
    }
    else if (fileTypeCode == 128) {
      return ".ttf";
    }
    else if (fileTypeCode == 150) {
      return ".bin";
    }
    else if (fileTypeCode == 152) {
      return ".ogm";
    }
    else if (fileTypeCode == 156) {
      return ".ter";
    }
    else if (fileTypeCode == 184) {
      return ".sbam";
    }
    else if (fileTypeCode == 194) {
      return ".tes";
    }
    
    return "." + fileTypeCode;
    */
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

        fm.skip(4); // for the next field
      }
      else if (FieldValidator.checkExtension(fm, "resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(fm.getFile(), "assets");
        if (dirFile != null && dirFile.exists()) {
          rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
          return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
        }
      }
      else if (FieldValidator.checkExtension(fm, "ress")) { // "resS", but all lowercase for the comparison
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = fm.getFilePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
            return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
          }
        }
      }
      else if (FilenameSplitter.getExtension(fm.getFile()).equals("")) {
        // no extension, like one of the "level" files
        rating += 20;

        fm.skip(4); // for the next field
      }
      else if (FilenameSplitter.getExtension(fm.getFile()).indexOf("split") == 0) {
        // a split archive

        // check to see that the extension is actually filename.assets.split## or filename.resource.split##
        String pathName = fm.getFilePath();
        if (pathName.indexOf(".assets.split") > 0 || pathName.indexOf(".resource.split") > 0) {
          rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
          return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
        }
      }
      else if (fm.readString(4).equals("Unit")) {
        // No extension, so maybe a UnityFS file?

        // 8 - Header ("UnityFS" + null)
        int versionCheck = 6;
        String headerString = fm.readString(3);
        int headerByte = fm.readByte();
        if (headerString.equals("yFS") && headerByte == 0) {
          rating += 50;
          versionCheck = 6;
        }
        else if (headerString.equals("yRa") && headerByte == 119) { // 119 = 'w'
          rating += 50;
          versionCheck = 3;
        }

        // 4 - Version Number (6) (BIG ENDIAN)
        if (IntConverter.changeFormat(fm.readInt()) == versionCheck) {
          rating += 5;
        }

        // X - General Version String (5.x.x)
        if (fm.readString(2).equals("5.")) {
          rating += 5;
        }

        return rating;
      }

      // 4 - Filename Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      // fm.skip(4); // already skipped in the check above

      long arcSize = fm.getLength();

      // 4 - Size of Assets file (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // 4 - Version (14) (BIG ENDIAN)
      int version = IntConverter.changeFormat(fm.readInt());
      if (version == 14) {
        rating += 5;
      }

      // 4 - Filename Directory Offset (BIG ENDIAN)
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - null
      fm.skip(4);

      // X - Version String (5.0.0f4)
      // 1 - null Terminator
      if (fm.readString(2).equals("5.")) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // First up, if they clicked on the "resource" or the "resS" file, point to the ASSETS file instead
      String extension = FilenameSplitter.getExtension(path);
      if (extension.equals("resource")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        File dirFile = getDirectoryFile(path, "assets");
        if (dirFile != null && dirFile.exists()) {
          path = dirFile;
        }
      }
      else if (extension.equals("resS")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = path.getAbsolutePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 5) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 5));
          if (dirFile != null && dirFile.exists()) {
            path = dirFile;
          }
        }
      }
      // Now we know we're pointing to the ASSETS file

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      // 4 - Size of Assets file (BIG ENDIAN)
      // 4 - Version Number (also Number Of Small Offsets) (15) (BIG ENDIAN)
      fm.skip(12);

      // 4 - Data Directory Offset (BIG ENDIAN)
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      fm.skip(4);

      // X - Version String (5.0.0f4)
      // 1 - null Terminator
      fm.readNullString();

      // 4 - Unknown
      // 1 - null
      fm.skip(5);

      // 4 - Number of Bases?
      int numBases = fm.readInt();
      FieldValidator.checkNumFiles(numBases);

      // BASES DIRECTORY
      // for each Base...
      for (int i = 0; i < numBases; i++) {
        // 4 - Unknown ID
        int ID = fm.readInt();

        if (ID < 0) {
          // 32 - Unknown
          fm.skip(32);
        }
        else {
          // 16 - Unknown
          fm.skip(16);
        }
      }

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 3);

      // 3 - null
      //fm.skip(3);
      // 0-3 - null to a multiple of 4 bytes
      fm.skip(calculatePadding(fm.getOffset(), 4));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // FILES DIRECTORY
      // for each file (24 bytes per entry)
      for (int i = 0; i < numFiles; i++) {
        // 4 - ID Number (incremental from 1)
        fm.skip(4);

        // 4 - null
        int null1 = fm.readInt();

        // 4 - File Offset (relative to the start of the Filename Directory) - points to the FilenameLength field
        int offset = fm.readInt() + dirOffset;
        //FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        //FieldValidator.checkLength(length, arcSize);

        // 4 - File Type Code
        int fileTypeCode = fm.readInt();

        // 2 - File Type Code
        int fileTypeCodeSmall = fm.readShort();

        // 2 - Unknown (-1)
        int unknownSmall = fm.readShort();

        // else, do checking as per normal
        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        if (fileTypeCode < 0) {
          fileTypeCode = fileTypeCodeSmall;
        }
        String fileType = convertFileType(fileTypeCode);

        /*
        try {
          Integer.parseInt(fileType.substring(1));
          // output the details for further analysis
          System.out.println(null1 + "\t" + offset + "\t" + length + "\t" + fileTypeCode + "\t" + fileTypeCodeSmall + "\t" + unknownSmall + "\t" + null2);
        }
        catch (Throwable t) {
        }
        */

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, fileType, offset, length);

        TaskProgressManager.setValue(i);
      }

      //
      // In this loop...
      // * Get the filenames for each file
      // * Detect all the Type1 Resources
      // * If a SND or TEX Resource has its data in an external archive, point to it instead
      //
      TaskProgressManager.setValue(0);

      Resource[] type1Resources = new Resource[numFiles];
      int numType1Resources = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String fileType = resource.getName();
        if (fileType.equals(".GameObject")) {
          // not a real file - just a folder structure or something
          // store it for analysis further down
          type1Resources[numType1Resources] = resource;
          numType1Resources++;
          continue;
        }

        // Go to the data offset
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength <= 0) {
          resource.setName(Resource.generateFilename(i) + fileType);
          continue;
        }
        try {
          FieldValidator.checkFilenameLength(filenameLength);
        }
        catch (Throwable t) {
          resource.setName(Resource.generateFilename(i) + fileType);
          continue;
        }

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to 4 bytes
        int paddingSize = calculatePadding(filenameLength, 4);
        fm.skip(paddingSize);

        resource.setName(filename + fileType);

        long realOffset = fm.getOffset();
        long realSize = resource.getLength() - (realOffset - offset);

        if (fileType.equals(".TextAsset")) {
          // 4 - File Size
          realSize = fm.readInt();
          realOffset = fm.getOffset();
        }
        else if (fileType.equals(".AudioClip")) {
          try {
            // 4 - Unknown (0/1)
            // 4 - Number of Channels? (1/2)
            // 4 - Sample Rate? (44100)
            // 4 - Bitrate? (16)
            // 4 - Unknown
            // 4 - null
            // 4 - null
            // 2 - Unknown (1)
            // 2 - Unknown (1)
            fm.skip(32);

            // 4 - External Archive Filename Length
            int externalFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(externalFilenameLength);

            // X - External Archive Filename
            String externalFilename = fm.readString(externalFilenameLength);
            FieldValidator.checkFilename(externalFilename);

            // 0-3 - null Padding to 4 bytes
            int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
            fm.skip(externalPaddingSize);

            // Check that the filename is a valid File in the FileSystem
            File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
            if (!externalArchive.exists()) {
              throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
            }

            long externalArcSize = externalArchive.length();

            // 4 - File Offset
            int extOffset = fm.readInt();
            FieldValidator.checkOffset(extOffset, externalArcSize);

            // 4 - null
            fm.skip(4);

            // 4 - File Length
            int extSize = fm.readInt();
            FieldValidator.checkLength(extSize, externalArcSize);

            // 4 - null
            // 4 - Unknown (1)

            // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
            resource.setSource(externalArchive);
            realOffset = extOffset;
            realSize = extSize;

            //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
          }
          catch (Throwable t) {
            // not in an external archive, or some other error
            ErrorLogger.log(t);
          }
        }
        else if (fileType.equals(".Texture2D")) {
          try {
            // 4 - Width/Height? (1024)
            int imageWidth = fm.readInt();

            // 4 - Width/Height? (1024/512)
            int imageHeight = fm.readInt();

            // 4 - File Size
            int imageFileSize = fm.readInt();

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 4 - Mipmap Count
            int mipmapCount = fm.readInt();

            if (resource.getLength() - imageFileSize > 0) {
              // This file is in the existing archive, not a separate archive file

              // 4 - Unknown (256)
              // 4 - Unknown (1)
              // 4 - Unknown (2)
              // 4 - Unknown (2/1)
              // 4 - Unknown (1/0)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null

              //fm.skip(40);
              fm.skip(32);

              if (fm.readInt() == imageFileSize) {
                // the image comes now
              }
              else {
                // another 4 bytes of something
                fm.skip(4);
              }

              realOffset = fm.getOffset();
              realSize = imageFileSize;

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

              //System.out.println("This is an internal resource --> Resource " + resource.getName());
            }
            else {
              // This file is in an external archive, not the current one

              // 4 - Unknown (256)
              // 4 - Unknown (1)
              // 4 - Unknown (2)
              // 4 - Unknown (2/1)
              // 4 - Unknown (1/0)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null
              // 4 - Unknown (1)
              // 4 - null

              fm.skip(40);
              /*
              String output = "";
              
              for (int f = 0; f < 10; f++) {
                output += "Field" + (f + 1) + "=" + fm.readInt() + "\t";
              }
              System.out.println("Width=" + imageWidth + "\tHeight=" + imageHeight + "\tType=" + imageFormat + "\tMipMaps=" + mipmapCount + "\tFileSize=" + imageFileSize + "\t" + output + resource.getName());
              */

              // 4 - File Offset
              int extOffset = fm.readInt();

              // 4 - File Length
              int extSize = fm.readInt();

              // 4 - External Archive Filename Length
              int externalFilenameLength = fm.readInt();
              FieldValidator.checkFilenameLength(externalFilenameLength);

              // X - External Archive Filename
              String externalFilename = fm.readString(externalFilenameLength);
              FieldValidator.checkFilename(externalFilename);

              // 0-3 - null Padding to 4 bytes
              int externalPaddingSize = calculatePadding(externalFilenameLength, 4);
              fm.skip(externalPaddingSize);

              // Check that the filename is a valid File in the FileSystem
              File externalArchive = new File(resource.getSource().getParent() + File.separatorChar + externalFilename);
              if (!externalArchive.exists()) {
                // the file doesn't exist - maybe it's a split archive that needs to be rebuilt?
                throw new FileNotFoundException("External resource " + externalFilename + " could not be found");
              }

              // Now check the offsets and sizes
              long externalArcSize = externalArchive.length();

              FieldValidator.checkOffset(extOffset, externalArcSize);
              FieldValidator.checkLength(extSize, externalArcSize);

              // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
              resource.setSource(externalArchive);
              realOffset = extOffset;
              realSize = extSize;

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

              //System.out.println("Setting External Resource " + externalFilename + " on Resource " + resource.getName());
            }
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

      //
      // In this loop...
      // * Go through all the Type 1 Resources and use them to set the folder names on the referenced Resources
      //
      TaskProgressManager.setValue(0);

      for (int r = 0; r < numType1Resources; r++) {
        Resource resource = type1Resources[r];

        // Go to the data offset
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Number of Referenced Files
        int numRefFiles = fm.readInt();
        FieldValidator.checkNumFiles(numRefFiles);

        // for each referenced file...
        Resource[] refResources = new Resource[numRefFiles];
        int[] refTypes = new int[numRefFiles];
        for (int i = 0; i < numRefFiles; i++) {
          // 4 - File Type of Referenced File (4/23/33/82/114)
          int refType = fm.readInt();
          refTypes[i] = refType;

          // 4 - null
          fm.skip(4);

          // 4 - File ID of Referenced File
          int refFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
          FieldValidator.checkRange(refFileID, 0, numFiles);
          refResources[i] = resources[refFileID];

          // 4 - null
          fm.skip(4);
        }

        // 4 - null
        fm.skip(4);

        // 4 - Folder Name Length
        int folderNameLength = fm.readInt();
        if (folderNameLength == 0) {
          continue; // don't worry about setting this folder name, if it's empty
        }
        FieldValidator.checkFilenameLength(folderNameLength);

        // X - Folder Name
        String folderName = fm.readString(folderNameLength);
        FieldValidator.checkFilename(folderName);

        // Set the folder name for each referenced file
        for (int i = 0; i < numRefFiles; i++) {
          Resource refResource = refResources[i];
          int refType = refTypes[i];

          /*
          // If the refType is 23 (matref) or 33 (mshref), we want to go in to those ref files to get the actual mesh/mat
          // being referenced and set the folder name on there rather than on the 23/33 file itself.
          if (refType == 23) { // matref file
          // go to the file offset, and skip over some unrequired data
          fm.seek(refResource.getOffset() + 56);
          
          // 4 - Number of Material References
          int numMatRef = fm.readInt();
          FieldValidator.checkNumFiles(numMatRef);
          
          // for each Material Reference
          for (int m = 0; m < numMatRef; m++) {
            // 4 - null
            fm.skip(4);
          
            // 4 - ID of 21 File (material file)
            int matFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
            FieldValidator.checkRange(matFileID, 0, numFiles);
            Resource matResource = resources[matFileID];
            System.out.println(">> Want to set on MATERIAL " + matFileID + " <Level " + (m + 1) + ">" + " --> " + folderName);
            //matResource.setName(folderName + "\\" + matResource.getName());
          
            // 4 - null
            fm.skip(4);
          }
          
          }
          else if (refType == 33) { // mshref file
          // go to the file offset, and skip over some unrequired data
          fm.seek(refResource.getOffset() + 12);
          
          // 4 - Unknown (0/3)
          int meshExists = fm.readInt();
          
          if (meshExists == 0) {
            // 4 - ID of 43 File (mesh file) (if previous field is null) or numFilesInWholeArchive (if previous field is =3)
            int mshFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
            FieldValidator.checkRange(mshFileID, 0, numFiles);
            Resource mshResource = resources[mshFileID];
            //System.out.println(">> Want to set on MESH " + mshFileID + " --> " + folderName);
            mshResource.setName(folderName + "\\" + mshResource.getName());
          }
          
          }*/

          // Otherwise for other refType files (and for the ref files as well), set the folder name on the file itself.
          refResource.setName(folderName + "\\" + refResource.getName());

        }

        TaskProgressManager.setValue(r);
      }

      //
      // In this loop...
      // * Remove all the Type1 Resources from the File List --> they're not real files
      // * Clear all the renames/replaced flags on the Resource, caused by setting the name, changing the file size, etc.
      // * Set the forceNotAdded flag on the Resources to override the "added" icons
      //
      int realNumFiles = numFiles - numType1Resources;
      Resource[] realResources = new Resource[realNumFiles];
      int realArrayPos = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.getExtension().equals("GameObject")) {
          continue;
        }

        resource.setReplaced(false);
        resource.setOriginalName(resource.getName());
        resource.forceNotAdded(true);

        realResources[realArrayPos] = resource;
        realArrayPos++;
      }

      /*
      // FOR ANALYSIS, READ THROUGH ALL THE *.1, *.4, *.114 FILES AND PRINT OUT THEIR DETAILS
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String extension = resource.getExtension();
        if (extension.equals("1")) {
          //analyse1File(resource, fm, resources);
        }
        else if (extension.equals("4")) {
          //analyse4File(resource, fm, resources);
        }
        else if (extension.equals("23")) {
          //analyse23File(resource, fm, resources);
        }
        else if (extension.equals("33")) {
          //analyse33File(resource, fm, resources);
        }
        else if (extension.equals("114")) {
          //analyse114File(resource, fm, resources);
        }
      }
      */

      fm.close();

      //return resources;
      return realResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }
}
