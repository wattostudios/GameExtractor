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
public class Plugin_ASSETS_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_8() {

    super("ASSETS_8", "Unity3D Engine Resource (Version 8)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("English Country Tune",
        "Rapscallion",
        "Sea Legends: Phantasmal Light",
        "Sonic Fan Remix",
        "Tech Ships: Waves of Victory",
        "Thomas Was Alone");
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
    */

    /*
    if (fileTypeCode == 1) {
      return ".obj"; // GameObject
    }
    else if (fileTypeCode == 4) {
      return ".ref"; // Transform
    }
    else if (fileTypeCode == 12) {
      return ".particleAnim"; // ParticleAnimator
    }
    else if (fileTypeCode == 15) {
      return ".particleEmit"; // EllipsoidParticleEmitter
    }
    else if (fileTypeCode == 20) {
      return ".camera"; // Camera
    }
    else if (fileTypeCode == 21) {
      return ".mat"; // Material
    }
    else if (fileTypeCode == 23) {
      return ".renderer"; // Renderer
    }
    else if (fileTypeCode == 26) {
      return ".particleRend"; // ParticleRenderer
    }
    else if (fileTypeCode == 28) {
      return ".tex"; // Texture2D
    }
    else if (fileTypeCode == 33) {
      return ".meshFilter"; // MeshFilter
    }
    else if (fileTypeCode == 43) {
      return ".msh"; // Mesh
    }
    else if (fileTypeCode == 48) {
      return ".shader"; // Shader
    }
    else if (fileTypeCode == 49) {
      return ".txt"; // Document - sometimes plain text, sometimes XML, sometimes HTML, sometimes JSON...
    }
    else if (fileTypeCode == 54) {
      return ".body"; // Rigidbody
    }
    else if (fileTypeCode == 65) {
      return ".boxColl"; // BoxCollider
    }
    else if (fileTypeCode == 74) {
      return ".ani"; // AnimationClip
    }
    else if (fileTypeCode == 76) {
      return ".worldColl"; // WorldParticleCollider
    }
    else if (fileTypeCode == 82) {
      return ".audioSrc"; // AudioSource 
    }
    else if (fileTypeCode == 83) {
      return ".snd"; // AudioClip // Sound - can be ogg, wav, mp3, aif, ... 
    }
    else if (fileTypeCode == 84) {
      return ".rendTex"; // RenderTexture 
    }
    else if (fileTypeCode == 96) {
      return ".trailRend"; // TrailRenderer 
    }
    else if (fileTypeCode == 102) {
      return ".textMesh"; // TextMesh 
    }
    else if (fileTypeCode == 111) {
      return ".anim"; // Animation 
    }
    else if (fileTypeCode == 115) {
      return ".script"; // MonoScript
    }
    else if (fileTypeCode == 128) {
      return ".ttf"; // Font
    }
    else if (fileTypeCode == 134) {
      return ".physic"; // PhysicMaterial
    }
    else if (fileTypeCode == 135) {
      return ".sphereColl"; // SphereCollider
    }
    else if (fileTypeCode == 136) {
      return ".capsuleColl"; // CapsuleCollider
    }
    else if (fileTypeCode == 137) {
      return ".skinMeshRend"; // SkinnedMeshRenderer
    }
    else if (fileTypeCode == 150) {
      return ".bin"; // PreloadData
    }
    else if (fileTypeCode == 152) {
      return ".ogm";
    }
    else if (fileTypeCode == 153) {
      return ".joint"; // ConfigurableJoint
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
    else if (fileTypeCode < 0 && fileTypeCode >= -31) { //-1 to -31
      return ".behaviour"; // MonoBehaviour
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
      else if (FieldValidator.checkExtension(fm, "streamingresourceimage")) { // "streamingResourceImage", but all lowercase for the comparison
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = fm.getFilePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 23) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 23));
          if (dirFile != null && dirFile.exists()) {
            rating += 50; // higher than 25, as have confirmed both an ASSETS file AND a RESOURCE file - quite unique
            return rating; // otherwise the remaining checks will throw Exceptions and kill this plugin as a candidate
          }
        }
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

      // 4 - Version (8) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 8) {
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
      else if (extension.equals("streamingResourceImage")) {
        // clicked on a referenced archive - check to see if the real ASSETS file exists
        String pathName = path.getAbsolutePath();
        int pathNameLength = pathName.length();
        if (pathNameLength > 23) {
          File dirFile = new File(pathName.substring(0, pathNameLength - 23));
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

      // 4 - File Details Directory Length (BIG ENDIAN)
      long dirOffset = arcSize - IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 1 - null
      fm.skip(1);

      // X - Version String (3.3.0f4)
      // 1 - null Terminator
      fm.readNullString();

      // 4 - Unknown (5)
      // 8 - null
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // FILES DIRECTORY
      // for each file (20 bytes per entry)
      for (int i = 0; i < numFiles; i++) {
        // 4 - ID Number (incremental from 1)
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
      // * Detect all the Type1 Resources
      // * If a SND or TEX Resource has its data in an external archive, point to it instead
      //
      TaskProgressManager.setValue(0);

      Resource[] type1Resources = new Resource[numFiles];
      int numType1Resources = 0;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        String fileType = resource.getName();
        //if (fileType.equals(".1")) {
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
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to 4 bytes
        int paddingSize = calculatePadding(filenameLength, 4);
        fm.skip(paddingSize);

        resource.setName(filename + fileType);

        long realOffset = fm.getOffset();
        long realSize = resource.getLength() - (realOffset - offset);

        if (fileType.equals(".AudioClip")) {
          try {
            // 4 - Unknown (2)
            // 4 - Audio Format? (14=OGG, 20=WAV)
            // 4 - Unknown (1)
            // 4 - Unknown (1) (Field Doesn't Exist for Sonic Fan Remix files)
            fm.skip(16);

            if (realSize == 24) {
              // ref to an external file

              // Check that the filename is a valid File in the FileSystem
              File externalArchive = new File(path + ".streamingResourceImage");
              if (!externalArchive.exists()) {
                throw new FileNotFoundException("External resource \"streamingResourceImage\" could not be found");
              }

              long externalArcSize = externalArchive.length();

              // 4 - External Length
              int extSize = fm.readInt();
              FieldValidator.checkLength(extSize, externalArcSize);

              // 4 - External Offset
              int extOffset = fm.readInt();
              FieldValidator.checkOffset(extOffset, externalArcSize);

              // Now that we reached the end successfully, set the externalArchive on the Resource (the other fields set down further)
              resource.setSource(externalArchive);
              realOffset = extOffset;
              realSize = extSize;
            }
            else {

              // 4 - Sound Data Length
              // test for Sonic files...
              String audioHeader = fm.readString(4);
              if (audioHeader.equals("RIFF")) {
                resource.setExtension("wav");

                realOffset += 16;
                realSize -= 16;
              }
              else if (audioHeader.equals("OggS")) {
                resource.setExtension("ogg");

                realOffset += 16;
                realSize -= 16;
              }
              else {
                // not a Sonic file - so the 4 bytes above were the sound data length field, and now comes the audio header

                // X - File Data
                audioHeader = fm.readString(4);
                if (audioHeader.equals("RIFF")) {
                  resource.setExtension("wav");
                }
                else if (audioHeader.equals("OggS")) {
                  resource.setExtension("ogg");
                }

                realOffset += 20;
                realSize -= 20;
              }
            }

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
            fm.skip(4);

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 2 - Unknown (1)
            // 2 - Unknown (1)
            // 4 - Unknown (1)
            // 4 - Unknown (2)
            // 4 - Unknown (2/1)
            // 4 - Unknown (1/0)
            // 4 - null
            // 4 - null
            // 4 - Unknown (3/0)
            // 4 - Image Data Length

            // X - Image Data
            realOffset += 52;
            realSize -= 52;

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

        // 4 - Unknown (4)
        fm.skip(4);

        // for each referenced file...
        Resource[] refResources = new Resource[numRefFiles];
        int[] refTypes = new int[numRefFiles];
        for (int i = 0; i < numRefFiles; i++) {
          // 4 - null
          fm.skip(4);

          // 4 - File ID of Referenced File
          int refFileID = fm.readInt() - 1; // -1 because fileID numbers start at 1, not 0
          FieldValidator.checkRange(refFileID, 0, numFiles);
          refResources[i] = resources[refFileID];

          // 4 - File Type of Referenced File (4/23/33/82/114)
          int refType = fm.readInt();
          refTypes[i] = refType;
        }

        // 4 - Folder Name Length
        int folderNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(folderNameLength);

        // X - Folder Name
        String folderName = fm.readString(folderNameLength);
        FieldValidator.checkFilename(folderName);

        // Set the folder name for each referenced file
        for (int i = 0; i < numRefFiles; i++) {
          Resource refResource = refResources[i];

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
        //if (resource.getExtension().equals("1")) {
        if (resource.getExtension().equals("GameObject")) {
          continue;
        }

        resource.setReplaced(false);
        resource.setOriginalName(resource.getName());
        resource.forceNotAdded(true);

        realResources[realArrayPos] = resource;
        realArrayPos++;
      }

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
