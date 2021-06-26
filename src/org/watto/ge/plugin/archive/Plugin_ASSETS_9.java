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
public class Plugin_ASSETS_9 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASSETS_9() {

    super("ASSETS_9", "Unity3D Engine Resource (Version 9)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("16bit Trader",
        "35mm",
        "A Valley Without Wind",
        "Absconding Zatwor",
        "Action Henk",
        "Agent Awesome",
        "AI War: Fleet Command",
        "Bad Piggies",
        "Between Me And The Night",
        "Blockstorm",
        "Bomb The Monsters!",
        "Break Into Zatwor",
        "Brilliant Bob",
        "Broforce",
        "Car Mechanic Simulator 2014",
        "Car Mechanic Simulator 2015",
        "Caravan",
        "Caveman World: Mountains of Unga Boonga",
        "Cognition",
        "Commando Jack",
        "Construction Simulator 2015",
        "Convoy",
        "Cooking Academy Collection",
        "Crowntakers",
        "Dark Years",
        "Dead Bits",
        "Dead Effect",
        "Dead in Bermuda",
        "Dracula's Legacy",
        "Dread Out",
        "Dreamscapes 2: Nightmare's Heir",
        "Dreamscapes: The Sandman",
        "Dungeonland",
        "Endless Space",
        "Fall of the New Age",
        "Fiends of Imprisonment",
        "Frankenstein: Master Of Death",
        "Frederic: Evil Strikes Back",
        "Frederic: Resurrection of Music",
        "Gunspell",
        "House Of Caravan",
        "Hyperdrive Massacre",
        "Iesabel",
        "IGT Slots Paradise Garden",
        "iO",
        "IWO: Bloodbath In The Bonins",
        "Jammerball",
        "Kairo",
        "Kentucky Route Zero",
        "Kingdom of Aurelia: Mystery of the Poisoned Dagger",
        "Kingdom Rush",
        "Lifeless Planet",
        "Lovely Planet",
        "Magma Tsunami",
        "Max: The Curse of Brotherhood",
        "Millie",
        "Mini Motor Racing EVO",
        "Moebius: Empire Rising",
        "No Time To Explain Remastered",
        "Overcast: Walden and the Werewolf",
        "Particula",
        "Pumped BMX +",
        "Risky Rescue",
        "Robot Squad Simulator 2017",
        "Robowars",
        "Rochard",
        "Shadowrun Returns",
        "Shadowrun: Dragonfall: Director's Cut",
        "Shadowrun: Hong Kong",
        "Shelter 2",
        "Shelter",
        "Sir, You Are Being Hunted",
        "Slender: The Eight Pages",
        "Space Farmers",
        "Space Hulk Ascension",
        "SPACECOM",
        "Sparkle 2 Evo",
        "Sparkle 3 Genesis",
        "Starwhal",
        "Tadpole Treble",
        "Teddy Floppy Ear: Mountain Adventure",
        "Teddy Floppy Ear: The Race",
        "Tesla Effect",
        "Teslagrad",
        "Toren",
        "True Bliss",
        "Unknown Battle",
        "Whispering Willows",
        "Why So Evil 2: Dystopia",
        "Why So Evil",
        "Witch's Pranks: Frog's Fortune",
        "Wolf Simulator",
        "Woodle Tree Adventures",
        "Ziggurat",
        "Zombillie");
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
    else if (fileTypeCode == 90) {
      return ".avatar"; // Avatar 
    }
    else if (fileTypeCode == 95) {
      return ".animator"; // Animator 
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
    else if (fileTypeCode == 142) {
      return ".assetBundle"; // AssetBundle
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
    else if (fileTypeCode == 198) {
      return ".particleSystem"; // ParticleSystem 
    }
    else if (fileTypeCode == 199) {
      return ".particleSystemRenderer"; // ParticleSystemRenderer 
    }
    else if (fileTypeCode == 96) {
      return ".trailRend"; // TrailRenderer 
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

      if (fm.readString(4).equals("Unit")) {
        // Maybe a UnityRaw file?

        // 8 - Header ("UnityRaw")
        String headerString = fm.readString(4);
        if (headerString.equals("yRaw")) {
          rating += 50;
        }

        fm.skip(1);

        // 4 - Version Number (3) (BIG ENDIAN)
        if (IntConverter.changeFormat(fm.readInt()) == 3) {
          rating += 5;
        }

        // X - General Version String (3.x.x)
        if (fm.readString(2).equals("3.")) {
          rating += 5;
        }

        return rating;
      }

      // 4 - Filename Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      //fm.skip(4); // already skipped in the check above

      long arcSize = fm.getLength();

      // 4 - Size of Assets file (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // 4 - Version (9) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 9) {
        rating += 5;
      }

      // 4 - Filename Directory Offset (BIG ENDIAN)
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - null
      fm.skip(4);

      // X - Version String (4.6.0f3)
      // 1 - null Terminator
      if (fm.readString(2).equals("4.")) {
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
      if (extension.equals("resS")) {
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

      boolean unityFS = false;
      int relativeOffset = 0;
      int relativeDataOffset = 0;
      if (fm.readString(8).equals("UnityRaw")) {
        // a UnityRaw file - skip over the header stuff, to reach the real file data
        unityFS = true;

        // 8 - Header ("UnityRaw")
        // 1 - null
        // 4 - Version Number (3) (BIG ENDIAN)
        fm.skip(5);

        // X - General Version String (3.x.x)
        // 1 - null Terminator
        fm.readNullString();

        // X - Version String (3.5.7f6)
        // 1 - null Terminator
        fm.readNullString();

        // 4 - Unknown
        fm.skip(4);

        // 4 - Archive Header 1 Size (60)
        relativeOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(relativeOffset, arcSize);

        // 4 - Unknown (1)
        // 4 - Unknown (1)
        // 4 - Archive Length [+60]
        // 4 - Archive Length [+60]
        // 4 - Archive Length
        fm.skip(20);

        // 4 - Archive Header 2 Length
        relativeOffset += IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(relativeOffset, arcSize);

        // X - Other Stuff
        // X - Unity Archive
        fm.seek(relativeOffset);

        // Now we're reading the first 2 fields of the normal unity file

        // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
        fm.skip(4);

        // 4 - Size of Assets file (BIG ENDIAN)
        relativeDataOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
        FieldValidator.checkOffset(relativeDataOffset, arcSize + 1);// +1 to allow for UnityFS files where the data is all inline
      }

      // 4 - Data Directory Offset [+14 + VersionStringLength] (BIG ENDIAN)
      // 4 - Size of Assets file (BIG ENDIAN)
      // 4 - Version Number (also Number Of Small Offsets) (9) (BIG ENDIAN)
      fm.skip(4); // already read 8 bytes in the check above

      // 4 - Data Directory Offset (BIG ENDIAN)
      int dirOffset = IntConverter.changeFormat(fm.readInt()) + relativeOffset; // +relativeOffset to account for the UnityRaw header, in those files;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      fm.skip(4);

      // X - Version String (3.5.7f6)
      // 1 - null Terminator
      fm.readNullString();

      if (unityFS) {
        // read all the nested property data

        // 4 - Unknown
        fm.skip(4);

        // 4 - Number of Blocks? (2)
        int blockCount = fm.readInt();
        FieldValidator.checkNumFiles(blockCount);

        readTypes(fm, blockCount, "", "", 0);

        // 4 - null
        fm.skip(4);

      }
      else {
        // 4 - Unknown
        // 4 - null
        // 4 - null
        fm.skip(12);
      }

      // 4 - Number of Files
      int numFiles = fm.readInt();
      if (arcSize > 1000000000) {
        // need to allow more files than usual in these large games (eg Dungeonland)
        FieldValidator.checkNumFiles(numFiles / 15);
      }
      else if (arcSize > 300000000) {
        // need to allow more files than usual in these large games (eg Caravan)
        FieldValidator.checkNumFiles(numFiles / 10);
      }
      else {
        FieldValidator.checkNumFiles(numFiles);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // FILES DIRECTORY
      // for each file (20 bytes per entry)
      for (int i = 0; i < numFiles; i++) {
        // 4 - ID Number (incremental from 1)
        fm.skip(4);

        // 4 - File Offset (relative to the start of the Filename Directory) - points to the FilenameLength field
        int offset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Type Code
        int fileTypeCode = fm.readInt();

        if (unityFS && fileTypeCode == 0 && (length < 0 || offset < 0)) {
          // abrupt end of the directory
          numFiles = i;
          resources = resizeResources(resources, numFiles);
          break;
        }

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

        if (fileType.equals(".TextAsset")) {
          // 4 - File Size
          realSize = fm.readInt();
          realOffset = fm.getOffset();
        }
        else if (fileType.equals(".AudioClip")) {
          // 4 - Unknown (2)
          // 4 - Unknown (20)
          // 4 - Unknown (0/1)
          // 4 - Unknown (2=external, 1=internal)
          fm.skip(16);

          // 4 - File Length
          int extSize = fm.readInt();

          long remainingLength = fm.getOffset() - offset - 4;
          if (remainingLength < 8 || (extSize - remainingLength > 100000)) {
            // external file (probably)
            try {
              // 4 - File Offset in External *.resS File
              int extOffset = fm.readInt();

              boolean foundFile = false;

              // Check that the filename is a valid File in the FileSystem
              File externalArchive = new File(resource.getSource() + ".resS");
              if (!unityFS && !externalArchive.exists()) {
                if (extSize - remainingLength > 100000) {
                  // in this case only, assume maybe it's actually in this archive
                  realOffset = fm.getOffset() - 4;
                  realSize = extSize;

                  foundFile = true; // so we can bypass the remaining checks
                }
                else {
                  // in all other cases, assume the external file is missing
                  throw new FileNotFoundException("Plugin_ASSETS_9: External resource " + externalArchive.getName() + " could not be found");
                }
              }
              else if (unityFS) {
                // UnityFS files contain the file data after the end of the unity data
                externalArchive = path;
              }

              if (!foundFile) {

                if (unityFS) {
                  extOffset += relativeDataOffset;
                }

                long externalArcSize = externalArchive.length();

                FieldValidator.checkLength(extSize, externalArcSize);
                FieldValidator.checkOffset(extOffset, externalArcSize);

                // set the externalArchive on the Resource (the other fields set down further)
                resource.setSource(externalArchive);
                realOffset = extOffset;
                realSize = extSize;
              }
            }
            catch (Throwable t) {
              realOffset += 20;
              realSize = extSize;
            }
          }
          else {
            // internal file

            // set the externalArchive on the Resource (the other fields set down further)
            realOffset = fm.getOffset();
            realSize = extSize;
          }

        }
        else if (fileType.equals(".Texture2D")) {
          try {
            // 4 - Width/Height?
            int imageWidth = fm.readInt();

            // 4 - Width/Height?
            int imageHeight = fm.readInt();

            // 4 - File Size
            int imageFileSize = fm.readInt();

            // 4 - Image Format Code
            int imageFormat = fm.readInt();

            // 2 - Unknown (0/1)
            // 2 - Unknown (1)
            // 4 - Unknown (1)
            // 4 - Unknown (2)
            // 4 - Unknown (1)
            // 4 - Unknown (1)
            // 4 - null
            // 4 - Unknown (0/1)
            // 4 - Mipmap Flag? (0=1 mipmap, 3=multiple mipmaps)
            fm.skip(32);

            // 4 - Unknown (0/1) [OPTIONAL]
            if (fm.readInt() != imageFileSize) {
              // If it were equal, we already found the second "file size" field.
              // If it wasn't equal, this optional field exists, and the second "file size" field comes now.
              // This field is sometimes missing in the game Rochard, but seems to exist in most other games!

              // 4 - File Size
              fm.skip(4);
            }

            // X - Image Data
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
            castResource.setMipmapCount(1);

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

      /*
      //
      // In this loop...
      // * Go through all the Type 1 Resources and use them to set the folder names on the referenced Resources
      //
      if (!unityFS) {
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
        FieldValidator.checkFilenameLength(folderNameLength);
      
        // X - Folder Name
        String folderName = fm.readString(folderNameLength);
        FieldValidator.checkFilename(folderName);
      
        // Set the folder name for each referenced file
        for (int i = 0; i < numRefFiles; i++) {
          Resource refResource = refResources[i];
          int refType = refTypes[i];
      
          // Otherwise for other refType files (and for the ref files as well), set the folder name on the file itself.
          refResource.setName(folderName + "\\" + refResource.getName());
      
        }
      
        TaskProgressManager.setValue(r);
      }
      }
      */

      //
      // In this loop...
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
