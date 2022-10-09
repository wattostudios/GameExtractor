/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZBD_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZBD_2() {

    super("ZBD_2", "ZBD_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Crimson Skies");
    setExtensions("zbd"); // MUST BE LOWER CASE
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

      // 4 - Header ((bytes)34,18,151,2)
      if (fm.readInt() == 43455010) {
        rating += 50;
      }

      // 4 - Version (42)
      if (fm.readInt() == 42) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Number of Textures
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Texture Directory Offset (40)
      if (fm.readInt() == 40) {
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ((bytes)34,18,151,2)
      // 4 - Version (42)
      // 4 - Date?
      fm.skip(12);

      // 4 - Number of Textures
      int numTextures = fm.readInt();
      FieldValidator.checkNumFiles(numTextures);

      // 4 - Texture Directory Offset
      int textureDirOffset = fm.readInt();
      FieldValidator.checkOffset(textureDirOffset, arcSize);

      // 4 - Material Directory Offset
      int materialDirOffset = fm.readInt();
      FieldValidator.checkOffset(materialDirOffset, arcSize);

      // 4 - Mesh Directory Offset
      int meshDirOffset = fm.readInt();
      FieldValidator.checkOffset(meshDirOffset, arcSize);

      // 4 - Node Array Size
      fm.skip(4);

      // 4 - Node Array Count
      int numNodes = fm.readInt();
      FieldValidator.checkNumFiles(numNodes);

      // 4 - Node Directory Offset
      int nodeDirOffset = fm.readInt();
      FieldValidator.checkOffset(nodeDirOffset, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;

      //
      // TEXTURE DIRECTORY
      //
      fm.relativeSeek(textureDirOffset);

      // Loop through directory
      for (int i = 0; i < numTextures; i++) {
        long offset = fm.getOffset();

        // 4 - Unknown
        // 8 - null
        fm.skip(12);

        // 20 - Filename (null terminator between filename and extension, and also null terminated after the extension, with nulls to fill)
        String filename = fm.readNullString(20) + ".texture";
        //System.out.println(filename);

        // 4 - Texture Usage? (1)
        // 4 - null
        // 4 - Unknown (-1)
        fm.skip(12);

        int length = 44;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      //
      // MATERIAL DIRECTORY
      //
      fm.relativeSeek(materialDirOffset);

      // 4 - Array Size
      int arraySize = fm.readInt();
      FieldValidator.checkNumFiles(arraySize);

      // 4 - Actual Number of Materials
      int numMaterials = fm.readInt();
      FieldValidator.checkNumFiles(numMaterials);

      // 4 - Maximum Index
      // 4 - Unknown
      fm.skip(8);

      // Loop through directory
      for (int i = 0; i < numMaterials; i++) {
        long offset = fm.getOffset();

        // 1 - Alpha?
        // 1 - Flags
        // 2 - RGB?
        // 4 - Red (Float)
        // 4 - Green (Float)
        // 4 - Blue (Float)
        // 4 - Texture ID
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Cycle Pointer
        // 4 - Unknown
        fm.skip(44);

        String filename = "Material " + (i + 1) + ".material";
        //System.out.println(filename);

        int length = 44;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      //
      // MESH DIRECTORY
      //
      fm.relativeSeek(meshDirOffset);

      // 4 - Array Size
      arraySize = fm.readInt();
      FieldValidator.checkNumFiles(arraySize);

      // 4 - Actual Number of Meshes
      int numMeshes = fm.readInt();
      FieldValidator.checkNumFiles(numMeshes);

      // 4 - Maximum Index
      fm.skip(4);

      // Loop through directory
      int[] offsets = new int[numMeshes];
      int startPointer = realNumFiles;

      int meshHeaderSize = 104;
      int[] meshHeaderOffsets = new int[numMeshes];
      long[][] blockLengthsToAdjust = new long[numMeshes][2];
      for (int i = 0; i < numMeshes; i++) {
        meshHeaderOffsets[i] = (int) fm.getOffset();

        // 4 - Unknown (0/1)
        // 4 - Unknown (0/2)
        // 4 - Unknown
        // 4 - Parent Count
        fm.skip(16);

        // 4 - Polygon Count
        int polygonCount = fm.readInt();

        // 4 - Vertex Count
        int vertexCount = fm.readInt();

        // 4 - Normal Count
        int normalCount = fm.readInt();

        // 4 - Morph Count
        int morphCount = fm.readInt();

        // 4 - Light Count
        int lightCount = fm.readInt();

        // 4 - null
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - null
        // 4 - Polygon Pointer
        // 4 - Vertex Pointer
        // 4 - Normals Pointer
        // 4 - Lights Pointer
        // 4 - Morphs Pointer
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - null
        // 4 - Unknown (0/1)
        // 4 - Unknown
        fm.skip(64);

        // 4 - Mesh Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        String filename = "Mesh " + (i + 1) + ".mesh";
        //System.out.println(filename + "\t" + offset);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("PolygonCount", polygonCount);
        resource.addProperty("VertexCount", vertexCount);
        resource.addProperty("NormalCount", normalCount);
        resource.addProperty("MorphCount", morphCount);
        resource.addProperty("LightCount", lightCount);

        // want to prepend the mesh header to the mesh data, so we can read it.
        long[] blockOffsets = new long[] { meshHeaderOffsets[i], offset };
        long[] blockLengths = new long[] { meshHeaderSize, 0 };
        blockLengthsToAdjust[i] = blockLengths;

        resource.setExporter(new BlockExporterWrapper(exporterDefault, blockOffsets, blockLengths, blockLengths));

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }
      // calculate the sizes
      for (int i = 0; i < numMeshes - 1; i++) {
        Resource resource = resources[startPointer + i];

        int length = offsets[i + 1] - offsets[i];
        resource.setLength(length);
        resource.setDecompressedLength(length);

        blockLengthsToAdjust[i][1] = length;
      }
      int lastLength = nodeDirOffset - offsets[numMeshes - 1];
      resources[realNumFiles - 1].setLength(lastLength);
      resources[realNumFiles - 1].setDecompressedLength(lastLength);

      blockLengthsToAdjust[numMeshes - 1][1] = lastLength;

      //
      // NODES DIRECTORY
      //
      fm.relativeSeek(nodeDirOffset);

      // Loop through directory
      offsets = new int[numNodes];
      startPointer = realNumFiles;

      for (int i = 0; i < numNodes; i++) {
        // 36 - Node Name (null terminated, filled with nulls)
        String filename = fm.readNullString(36);

        // 4 - Flags
        // 4 - null
        // 4 - Unknown
        // 4 - Zone ID?
        fm.skip(16);

        // 4 - Node Type (2=world, 4=display, ...)
        int nodeType = fm.readInt();
        filename += ".node" + nodeType;

        // 4 - Node Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        //System.out.println(filename + "\t" + offset);

        // 152 - Other Node Data
        fm.skip(152);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }
      // calculate the sizes
      for (int i = 0; i < numNodes - 1; i++) {
        Resource resource = resources[startPointer + i];

        int length = offsets[i + 1] - offsets[i];
        resource.setLength(length);
        resource.setDecompressedLength(length);
      }
      lastLength = (int) (arcSize - offsets[numNodes - 1]);
      resources[realNumFiles - 1].setLength(lastLength);
      resources[realNumFiles - 1].setDecompressedLength(lastLength);

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
