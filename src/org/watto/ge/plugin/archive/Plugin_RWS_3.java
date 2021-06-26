/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RWS_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RWS_3() {

    super("RWS_3", "RWS_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Beat Down: Fists Of Vengeance");
    setExtensions("rws"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // First Chunk Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  long arcSize = 0;

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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {
        // 4 - Chunk ID
        int chunkID = fm.readInt();

        // 4 - Chunk Length
        int chunkLength = fm.readInt();
        FieldValidator.checkLength(chunkLength, arcSize);

        // 4 - Library ID Stamp (contains RenderWare Version Number in it)
        fm.skip(4);

        // process the chunks
        if (chunkID == 16) { // Clump
          process16(fm, resources, path, chunkLength);
        }
        else if (chunkID == 22) { // Texture Dictionary
          process22(fm, resources, path, chunkLength);
        }
        else if (chunkID == 27) { // Anim Animation
          process27(fm, resources, path, chunkLength);
        }
        else {
          String filename = Resource.generateFilename(realNumFiles) + getExtensionForChunk(chunkID);

          long offset = fm.getOffset();
          fm.skip(chunkLength);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, chunkLength);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }

      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

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

  public void process16(FileManipulator fm, Resource[] resources, File path, int thisLength) {
    // Clump
    long startOffset = fm.getOffset();
    long endOffset = startOffset + thisLength;

    try {
      // ROOT STRUCT
      // 4 - Chunk ID (1)
      // 4 - Chunk Length (8)
      // 4 - Library ID Stamp

      // 4 - Number of Atomics
      // 4 - Number of Lights
      // 4 - Number of Cameras

      // FRAME LIST

      // GEOMETRY LIST

      // ATOMICS

      // LIGHTS

      // CAMERAS

      String filename = Resource.generateFilename(realNumFiles) + getExtensionForChunk(16);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, startOffset, thisLength);
      realNumFiles++;

      TaskProgressManager.setValue(endOffset);

    }
    catch (Throwable t) {
      logError(t);
    }

    fm.relativeSeek(endOffset);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public void process21(FileManipulator fm, Resource[] resources, File path, int thisLength) {
    // Raster (Texture)
    long startOffset = fm.getOffset();
    long endOffset = startOffset + thisLength;

    try {
      // ROOT STRUCT
      // 4 - Chunk ID (1)
      fm.skip(4);

      // 4 - Chunk Length (8)
      int structLength = fm.readInt();

      // 4 - Library ID Stamp
      fm.skip(4);

      // 4 - Platform Name ("PS2" + null)
      // 4 - Unknown
      fm.skip(structLength);

      // NAME STRING
      // 4 - Chunk ID (2)
      fm.skip(4);

      // 4 - Chunk Length
      int nameLength = fm.readInt();

      // 4 - Library ID Stamp
      fm.skip(4);

      // X - Image Name
      // 1 - null Name Terminator
      // 0-3 null Padding to a multiple of 4 bytes
      String filename = fm.readNullString(nameLength) + getExtensionForChunk(21);

      // ALPHA MASK STRING
      // 4 - Chunk ID (2)
      // 4 - Chunk Length
      // 4 - Library ID Stamp

      // X - Alpha Mask Name
      // 1 - null Alpha Mask Terminator
      // 0-3 null Padding to a multiple of 4 bytes

      // IMAGE DETAILS STRUCT
      // 4 - Chunk ID (1)
      // 4 - Chunk Length
      // 4 - Library ID Stamp

      // 4 - Image Width
      // 4 - Image Height
      // 4 - Image Depth
      // 4 - Image Format
      // 8 - PS2 TEX0 GS register
      // 8 - PS2 TEX1 GS register
      // 8 - PS2 MIPTBP1 GS register
      // 8 - PS2 MIPTBP2 GS register

      // 4 - Mipmap Data Length
      // 4 - Palette Data Length
      // 4 - gpuDataAlignedSize;      // memory span of the texture mipmap data on the GS (aligned to pages/2048)
      // 4 - skyMipmapVal;            // always 4032

      // PIXEL DATA
      // X - Pixel Data

      // PALETTE STRUCT
      // X - Palette Data

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, startOffset, thisLength);
      realNumFiles++;

      TaskProgressManager.setValue(endOffset);

    }
    catch (Throwable t) {
      logError(t);
    }

    fm.relativeSeek(endOffset);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public void process22(FileManipulator fm, Resource[] resources, File path, int thisLength) {
    // Texture Dictionary
    long startOffset = fm.getOffset();
    long endOffset = startOffset + thisLength;

    try {
      // 4 - Chunk ID (1)
      // 4 - Chunk Length (4)
      // 4 - Library ID Stamp
      fm.skip(12);

      // 2 - Number of Textures
      short numImages = fm.readShort();
      FieldValidator.checkNumFiles(numImages);

      // 2 - Device ID (6=PS2, 8=XBox)
      fm.skip(2);

      for (int i = 0; i < numImages; i++) {
        // 4 - Chunk ID
        int chunkID = fm.readInt();

        // 4 - Chunk Length
        int chunkLength = fm.readInt();
        FieldValidator.checkLength(chunkLength, arcSize);

        // 4 - Library ID Stamp (contains RenderWare Version Number in it)
        fm.skip(4);

        // process the chunks
        if (chunkID == 21) { // Raster (Texture)
          process21(fm, resources, path, chunkLength);
        }
        else {
          ErrorLogger.log("[RWS_3] Unknown Chunk ID within process22: " + chunkID);
          fm.skip(chunkLength);
        }

      }

    }
    catch (Throwable t) {
      logError(t);
    }

    fm.relativeSeek(endOffset);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public void process27(FileManipulator fm, Resource[] resources, File path, int thisLength) {
    // Anim Animation
    long startOffset = fm.getOffset();
    long endOffset = startOffset + thisLength;

    try {
      // 4 - Version
      // 4 - Frame Format
      // 4 - Number of Frames
      // 4 - Flags
      // 4 - Animation Duration (Float)

      // FRAME DATA

      String filename = Resource.generateFilename(realNumFiles) + getExtensionForChunk(27);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, startOffset, thisLength);
      realNumFiles++;

      TaskProgressManager.setValue(endOffset);

    }
    catch (Throwable t) {
      logError(t);
    }

    fm.relativeSeek(endOffset);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public String getExtensionForChunk(int chunkID) {
    if (chunkID == 1) {
      return ".struct";
    }
    else if (chunkID == 2) {
      return ".string";
    }
    else if (chunkID == 3) {
      return ".extension";
    }
    else if (chunkID == 5) {
      return ".camera";
    }
    else if (chunkID == 6) {
      return ".texture";
    }
    else if (chunkID == 7) {
      return ".material";
    }
    else if (chunkID == 8) {
      return ".material_list";
    }
    else if (chunkID == 9) {
      return ".atomic_section";
    }
    else if (chunkID == 10) {
      return ".plane_section";
    }
    else if (chunkID == 11) {
      return ".world";
    }
    else if (chunkID == 12) {
      return ".spline";
    }
    else if (chunkID == 13) {
      return ".matrix";
    }
    else if (chunkID == 14) {
      return ".frame_list";
    }
    else if (chunkID == 15) {
      return ".geometry";
    }
    else if (chunkID == 16) {
      return ".clump";
    }
    else if (chunkID == 18) {
      return ".light";
    }
    else if (chunkID == 19) {
      return ".unicode_string";
    }
    else if (chunkID == 20) {
      return ".atomic";
    }
    else if (chunkID == 21) {
      return ".raster"; // texture
    }
    else if (chunkID == 22) {
      return ".texture_dictionary";
    }
    else if (chunkID == 23) {
      return ".animation_database";
    }
    else if (chunkID == 24) {
      return ".image";
    }
    else if (chunkID == 25) {
      return ".skin_animation";
    }
    else if (chunkID == 26) {
      return ".geometry_list";
    }
    else if (chunkID == 27) {
      return ".anim_animation";
    }
    else if (chunkID == 28) {
      return ".team";
    }
    else if (chunkID == 29) {
      return ".crowd";
    }
    else if (chunkID == 30) {
      return ".delta_morph_animation";
    }
    else if (chunkID == 31) {
      return ".right_to_render";
    }
    else if (chunkID == 32) {
      return ".multitexture_effect_native";
    }
    else if (chunkID == 33) {
      return ".multitexture_effect_dictionary";
    }
    else if (chunkID == 34) {
      return ".team_dictionary";
    }
    else if (chunkID == 35) {
      return ".platform_indep_texture_dictionary";
    }
    else if (chunkID == 36) {
      return ".table_of_contents";
    }
    else if (chunkID == 37) {
      return ".particle_standard_global";
    }
    else if (chunkID == 38) {
      return ".altpipe";
    }
    else if (chunkID == 39) {
      return ".platform_indep_peds";
    }
    else if (chunkID == 40) {
      return ".patch_mesh";
    }
    else if (chunkID == 41) {
      return ".chunk_group_start";
    }
    else if (chunkID == 42) {
      return ".chunk_group_end";
    }
    else if (chunkID == 43) {
      return ".uv_animation_dictionary";
    }
    else if (chunkID == 44) {
      return ".coll_tree";
    }
    else {
      return ".chunk" + chunkID;
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
