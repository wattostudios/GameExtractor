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
public class Plugin_ASSETS_6 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_6() {

    super("ASSETS_6", "Unity3D Engine Resource (Version 6)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Fract",
        "Max And The Magic Marker",
        "StuntMANIA");
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

      // 4 - Version (6) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 6) {
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

        // 0-3 - null Padding to 4 bytes
        int paddingSize = calculatePadding(filenameLength, 4);
        fm.skip(paddingSize);

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
            // 4 - m_Format (2)
            // 4 - m_Type (14=OGG, 20=WAV)
            // 2 - m_3D (1)
            // 2 - null Padding
            // 4 - Sound Data Length
            fm.skip(16);

            // X - File Data
            boolean found = false;

            String audioHeader = fm.readString(4);
            if (audioHeader.equals("RIFF")) {
              resource.setExtension("wav");
              resource.setOriginalName(resource.getName());
              found = true;
            }
            else if (audioHeader.equals("OggS")) {
              resource.setExtension("ogg");
              resource.setOriginalName(resource.getName());
              found = true;
            }

            realOffset += 16;
            realSize -= 16;

            if (!found) {
              // 4 - Sound Data Length
              fm.skip(4);

              // X - File Data
              audioHeader = fm.readString(4);

              if (audioHeader.equals("RIFF")) {
                resource.setExtension("wav");
                resource.setOriginalName(resource.getName());
                found = true;
              }
              else if (audioHeader.equals("OggS")) {
                resource.setExtension("ogg");
                resource.setOriginalName(resource.getName());
                found = true;
              }

              if (found) {
                realOffset += 8;
                realSize -= 8;
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
            // 4 - Width
            int imageWidth = fm.readInt();

            // 4 - Height
            int imageHeight = fm.readInt();

            // 4 - File Size
            fm.skip(4);

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 2 - m_MipMap (1)
            // 2 - m_IsReadable (1)
            // 4 - m_ImageCount (1)
            // 4 - m_TextureDimension (2)
            // 4 - m_FilterMode (2/1)
            // 4 - m_Aniso (1/0)
            // 4 - m_MipBias
            // 4 - m_WrapMode
            // 4 - Image Data Length

            // X - Image Data
            realOffset += 48;
            realSize -= 48;

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
    catch (Throwable t) {
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
