/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.viewer;

import java.awt.Image;
import java.io.File;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ASSETS_14;
import org.watto.ge.plugin.archive.Plugin_ASSETS_15;
import org.watto.ge.plugin.archive.Plugin_ASSETS_17;
import org.watto.ge.plugin.archive.Plugin_ASSETS_20;
import org.watto.ge.plugin.archive.Plugin_ASSETS_21;
import org.watto.ge.plugin.archive.Plugin_ASSETS_22;
import org.watto.ge.plugin.archive.Plugin_ASSETS_5;
import org.watto.ge.plugin.archive.Plugin_ASSETS_6;
import org.watto.ge.plugin.archive.Plugin_ASSETS_8;
import org.watto.ge.plugin.archive.Plugin_ASSETS_9;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_Unity3D_MESH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_Unity3D_MESH() {
    super("Unity3D_MESH", "Unity3D 3D Mesh Viewer");
    setExtensions("mesh");

    setGames("Unity3D Engine");
    setPlatforms("PC");
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
      if (plugin instanceof Plugin_ASSETS_22 || plugin instanceof Plugin_ASSETS_21 || plugin instanceof Plugin_ASSETS_20 || plugin instanceof Plugin_ASSETS_17 || plugin instanceof Plugin_ASSETS_15 || plugin instanceof Plugin_ASSETS_14 || plugin instanceof Plugin_ASSETS_9 || plugin instanceof Plugin_ASSETS_8 || plugin instanceof Plugin_ASSETS_6 || plugin instanceof Plugin_ASSETS_5) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  float minX = 20000f;

  float maxX = -20000f;

  float minY = 20000f;

  float maxY = -20000f;

  float minZ = 20000f;

  float maxZ = -20000f;

  int tripleZeroCount = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      long arcSize = fm.getLength();

      int version = 0;
      ArchivePlugin plugin = Archive.getReadPlugin();

      if (plugin instanceof Plugin_ASSETS_5) {
        version = 5;
      }
      else if (plugin instanceof Plugin_ASSETS_6) {
        version = 6;
      }
      else if (plugin instanceof Plugin_ASSETS_8) {
        version = 8;
      }
      else if (plugin instanceof Plugin_ASSETS_9) {
        version = 9;
      }
      else if (plugin instanceof Plugin_ASSETS_14) {
        version = 14;
      }
      else if (plugin instanceof Plugin_ASSETS_15) {
        version = 15;
      }
      else if (plugin instanceof Plugin_ASSETS_17) {
        version = 17;
      }
      else if (plugin instanceof Plugin_ASSETS_20) {
        version = 20;
      }
      else if (plugin instanceof Plugin_ASSETS_21) {
        version = 21;
      }
      else if (plugin instanceof Plugin_ASSETS_22) {
        version = 22;
      }

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Number of Headers (1/2)
      int numHeaders = fm.readInt();
      FieldValidator.checkRange(numHeaders, 1, 50); // guess

      // 4 - null
      fm.skip(4);

      // 4 - Number of Faces
      int numFaces = fm.readInt();
      FieldValidator.checkNumFaces(numFaces);

      // 4 - Unknown (0 for the first entry)
      // 4 - null
      //fm.skip(8);
      int unknownCheck1 = fm.readInt();
      //System.out.println("Unknown Check 1: " + unknownCheck1);
      int unknownCheck2 = fm.readInt();
      //System.out.println("Unknown Check 2: " + unknownCheck1);

      // 4 - Number of Vertices (approx)
      int vertexCheck = fm.readInt(); // version 17 = null
      //System.out.println("Vertex Check: " + vertexCheck);

      int numVertexInHeader = vertexCheck;
      if (vertexCheck == 0) { // version 17
        // 4 - Number of Vertices (approx)
        //fm.skip(4);
        numVertexInHeader = fm.readInt();
      }

      // 4 - Center X (Float)
      float centerXHeader = fm.readFloat();
      // 4 - Center Y (Float)
      float centerYHeader = fm.readFloat();
      // 4 - Center Z (Float)
      float centerZHeader = fm.readFloat();
      // 4 - Radius X (Float)
      float radiusXHeader = fm.readFloat();
      // 4 - Radius Y (Float)
      float radiusYHeader = fm.readFloat();
      // 4 - Radius Z (Float)
      float radiusZHeader = fm.readFloat();

      // X - Additional Headers
      if (numHeaders > 1) {
        //System.out.println("Num Headers: " + numHeaders);

        if (vertexCheck == 0) { // version 17
          fm.skip((numHeaders - 1) * 48);
        }
        else { // version 9 or 15
          //fm.skip((numHeaders - 1) * 40);
          fm.skip((numHeaders - 1) * 44);
        }

        //fm.skip((numHeaders - 1) * 44);
      }

      // 4 - Number of 40-byte Blocks (usually null)
      int num40Blocks = fm.readInt();
      try {
        FieldValidator.checkNumFaces(num40Blocks + 1); // +1 to allow 0 blocks
      }
      catch (Throwable t) {
        num40Blocks = fm.readInt();
        FieldValidator.checkNumFaces(num40Blocks + 1); // +1 to allow 0 blocks
      }

      // X*40 - 40-byte Blocks
      fm.skip(num40Blocks * 40);

      // 4 - Number of 12-Byte Blocks (usually null)
      int num12Blocks = fm.readInt();
      FieldValidator.checkNumFaces(num12Blocks + 1); // +1 to allow 0 blocks

      // X*12 - 12-byte Blocks
      fm.skip(num12Blocks * 12);

      int previousField = -1;
      int unknownIntFlag = 0;

      boolean fieldsBeforeFaces = false;

      // 4 - Number of Name Blocks (usually null)
      int numNameBlocks = fm.readInt();
      if (numNameBlocks == 65536 || numNameBlocks == 16842752) { // some version 9 have a short header
        int flagInt = numNameBlocks;
        byte[] flagBytes = ByteArrayConverter.convertLittle(flagInt);

        // 4 - Unknown (0/1)
        fm.skip(4);
      }
      else {
        FieldValidator.checkNumFaces(numNameBlocks + 1); // +1 to allow 0 blocks

        long nameStart = fm.getOffset();
        try {

          for (int n = 0; n < numNameBlocks; n++) {
            // 4 - Name Length
            int nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Name
            fm.skip(nameLength);

            // 0-3 - null Padding to a multiple of 4 bytes
            fm.skip(ArchivePlugin.calculatePadding(nameLength, 4));

            // 4 - Unknown
            // 4 - null
            fm.skip(8);
          }

          // 4 - Number of 8-Byte Blocks (usually null)
          int num8Blocks = fm.readInt();
          FieldValidator.checkNumFaces(num8Blocks + 1); // +1 to allow 0 blocks

          // X*8 - 8-byte Blocks
          fm.skip(num8Blocks * 8);

          // 4 - Number of 64-byte blocks
          int num64Blocks = fm.readInt();
          FieldValidator.checkNumFaces(num64Blocks + 1); // +1 to allow 0 blocks

          // X*64 - 64-byte Blocks
          fm.skip(num64Blocks * 64);

        }
        catch (Throwable t) {
          // v22 seems to have longer name fields, sometimes, so go back and try again
          fm.relativeSeek(nameStart);

          for (int n = 0; n < numNameBlocks; n++) {
            // 4 - Name Length
            int nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Name
            fm.skip(nameLength);

            // 0-3 - null Padding to a multiple of 4 bytes
            fm.skip(ArchivePlugin.calculatePadding(nameLength, 4));

            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(12);
          }

          // v22 HAS 4-Byte BLOCKS HERE INSTEAD OF 8-byte BLOCKS 
          // 4 - Number of 4-Byte Blocks (usually null)
          int num8Blocks = fm.readInt();
          FieldValidator.checkNumFaces(num8Blocks + 1); // +1 to allow 0 blocks

          // X*4 - 4-byte Blocks
          fm.skip(num8Blocks * 4);

          // 4 - Number of 64-byte blocks
          int num64Blocks = fm.readInt();
          FieldValidator.checkNumFaces(num64Blocks + 1); // +1 to allow 0 blocks

          // X*64 - 64-byte Blocks
          fm.skip(num64Blocks * 64);

          fieldsBeforeFaces = true;
        }

        // 4 - Number of 4-Byte Blocks
        int num4Blocks = fm.readInt();
        FieldValidator.checkNumFaces(num4Blocks + 1); // +1 to allow 0 blocks

        // X*4 - 4-byte Blocks
        fm.skip(num4Blocks * 4);

        // 4 - Unknown (usually null)
        //fm.skip(4);
        //int unknownInt = fm.readInt();
        byte[] unknownIntFlagBytes = fm.readBytes(4);
        unknownIntFlag = IntConverter.convertLittle(unknownIntFlagBytes);

        //System.out.println("Unknown Int: " + unknownIntFlag);
        //System.out.println("Unknown Int Flags: " + unknownIntFlagBytes[0] + "\t" + unknownIntFlagBytes[1] + "\t" + unknownIntFlagBytes[2] + "\t" + unknownIntFlagBytes[3]);
        //System.out.println("Unknown Int: " + unknownInt);

        // 4 - Flags? (0,0,1,1 or 0,0,1,0)
        byte[] flagBytes = fm.readBytes(4);
        int flagInt = IntConverter.convertLittle(flagBytes);
        //System.out.println("Flags: " + flagBytes[0] + "\t" + flagBytes[1] + "\t" + flagBytes[2] + "\t" + flagBytes[3]);

        if (num4Blocks != 0 && flagInt == num4Blocks) {
          // Chasing Static has this field populated to the same as num4Blocks and num64Blocks

          // X*24 - 24-byte Blocks
          fm.skip(flagInt * 24);

          // Now read the flags

          // 4 - Flags? (0,0,1,1 or 0,0,1,0)
          flagBytes = fm.readBytes(4);
          flagInt = IntConverter.convertLittle(flagBytes);
        }

        // 4 - Unknown (0/1)
        if (vertexCheck != 0 && (flagBytes[1] == 0 && flagInt != 0)) {
          previousField = fm.readInt();
          //System.out.println("Previous Field: " + previousField);
        }
      }

      //
      // FACES
      //

      int faceDataSize = 2;
      if (numFaces > 65536) {
        faceDataSize = 4;
      }

      if (fieldsBeforeFaces) {
        // v22 has 2 fields here, before the faces start
        fm.skip(8);
      }

      // 4 - Length of Faces List (not including this field)
      int faceListLength = fm.readInt();
      if (faceListLength == 0 || faceListLength == 1) {
        //System.out.println("Face List Length: " + faceListLength);
        faceListLength = fm.readInt();
      }
      if (faceListLength == 65536 && previousField != -1) {
        //System.out.println("Face List Length: " + faceListLength);
        try {
          // we skipped an extra 4 bytes when we shouldn't have done so, so go back 4 bytes
          FieldValidator.checkLength(previousField, arcSize);
          faceListLength = previousField;
          fm.relativeSeek(fm.getOffset() - 4);
        }
        catch (Throwable t) {
        }
      }
      try {
        if (faceListLength == 256 && numFaces > 128) {
          //System.out.println("Face List Length: " + faceListLength);
          faceListLength = fm.readInt();
        }
        else if (faceListLength == 256) {
          // check the next field, if it's null, we need to move forward a few more
          long currentPos = fm.getOffset();
          if (fm.readInt() == 0) {
            faceListLength = fm.readInt();
          }
          else {
            fm.relativeSeek(currentPos);
          }
        }
        FieldValidator.checkLength(faceListLength, arcSize);

        // also check the remaining length, to better error check
        FieldValidator.checkOffset(fm.getOffset() + faceListLength, arcSize);
      }
      catch (Throwable t) {
        faceListLength = fm.readInt();
        FieldValidator.checkLength(faceListLength, arcSize);
      }

      if (faceListLength == 0) { // Chasing Static has another null here
        faceListLength = fm.readInt();
        FieldValidator.checkLength(faceListLength, arcSize);

        if (faceListLength == 0) { // v20 (eg Sunblaze) has another null here
          faceListLength = fm.readInt();
          FieldValidator.checkLength(faceListLength, arcSize);
        }
      }

      int numFaces3 = faceListLength / faceDataSize;
      FieldValidator.checkNumFaces(numFaces3);

      numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      if (faceDataSize == 2) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = (ShortConverter.unsign(fm.readShort()));
          int facePoint2 = (ShortConverter.unsign(fm.readShort()));
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;

        }
      }
      else if (faceDataSize == 4) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
          int facePoint1 = (int) (IntConverter.unsign(fm.readInt()));
          int facePoint2 = (int) (IntConverter.unsign(fm.readInt()));
          int facePoint3 = (int) (IntConverter.unsign(fm.readInt()));

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;

        }
      }

      // 0-2 - null Padding to a multiple of 4 bytes
      if (faceListLength % 4 != 0) {
        fm.skip(2);
      }

      long endOfFaces = fm.getOffset();

      // 4 - Number of 32-Byte Blocks (also equals the number of vertices)
      int num32Blocks = fm.readInt();
      FieldValidator.checkNumFaces(num32Blocks + 1); // +1 to allow 0 blocks

      // X*32 - 32-byte Blocks
      int length32 = num32Blocks * 32;
      if (fm.getOffset() + length32 >= arcSize) {
        // past the end of the archive, so this field is probably actually the numVertex.
        // this will be corrected down further
        num32Blocks = 0;
      }
      fm.skip(num32Blocks * 32);

      // 4 - Vertex Size Flags (11/15/43/47)
      int vertexSizeFlags = fm.readInt();
      if (vertexSizeFlags == 0) { // some v9 files (with a short header)
        vertexSizeFlags = fm.readInt();
      }
      //System.out.println("Vertex Size Flags: " + vertexSizeFlags);

      // 4 - Number of Vertices (approx)
      int numVertices = fm.readInt();
      if (numVertices == 1065353216) { // some v9 with small headers - this is 1.0f in 64-byte blocks
        // we've already read 4 bytes from the first 64-byte entry...
        fm.skip(((vertexSizeFlags - 1) * 64) + 60);

        // now read the vertex size flags and the numVertices
        vertexSizeFlags = fm.readInt();
        numVertices = fm.readInt();
      }
      try {
        FieldValidator.checkNumVertices(numVertices);
        FieldValidator.checkPositive(numVertices - 5); // want to catch some small ones here too, as small is probably wrong
      }
      catch (Throwable t) {
        // for some, it doesn't have the 32-block or size flags, so go back and try reading the numVertices
        fm.relativeSeek(endOfFaces);

        numVertices = fm.readInt();
        //System.out.println("Vertex Size Flags (take 2): " + vertexSizeFlags);
        FieldValidator.checkNumVertices(numVertices);
      }

      // 4 - Number of Detail Blocks (6)
      int numDetailBlocks = fm.readInt();
      //System.out.println("Num Detail Blocks: " + numDetailBlocks);
      try {
        FieldValidator.checkRange(numDetailBlocks, 0, 50); // guess
      }
      catch (Throwable t) {
        // for some, it doesn't have the size flags, so go back and try reading the numVertices and numDetailBlocks again
        fm.relativeSeek(endOfFaces + 4);

        numVertices = fm.readInt();
        //System.out.println("Vertex Size Flags (take 3): " + vertexSizeFlags);
        FieldValidator.checkNumVertices(numVertices);

        numDetailBlocks = fm.readInt();
        //System.out.println("Num Detail Blocks: " + numDetailBlocks);
        FieldValidator.checkRange(numDetailBlocks, 0, 50); // guess
      }

      // X*4 - Detail Blocks
      fm.skip(numDetailBlocks * 4);

      int vertexBlockSize = 0;

      boolean knownVertexBlockSize = false;

      // a check to see if the vertex block size calculation ends up being even, because if it is, it's probably right.
      boolean evenCheck = false;

      // 4 - Unknown (4)
      //System.out.println(fm.getOffset());
      int fieldTest4 = fm.readInt();
      if (fieldTest4 == 4) {

        // 4 - Vertex Size Flags (again, or smaller) (11/15/43/47) (35)
        // 4 - null
        fm.skip(8);

        // 4 - Vertex Block Size (24/32/36/40/48/...)
        vertexBlockSize = fm.readInt();
        FieldValidator.checkRange(vertexBlockSize, 0, 128); // guess

        knownVertexBlockSize = true;

        // 4 - Texture? Block Size (0/8/12)
        // 4 - Offset to Texture? Block (relative to the start of the Vertices List)
        // 4 - Texture? Block Size (0/8/12)
        fm.skip(12);

        // 20 - null
        fm.skip(20);

        // 4 - null
        fm.skip(4);

        // 4 - Length of Vertices List (not including this field or the null above)
        int verticesListLength = fm.readInt();
        FieldValidator.checkLength(verticesListLength, arcSize);
      }
      else {
        // the field we read was action the Length of Vertices List field
        int verticesListLength = fieldTest4;
        FieldValidator.checkLength(verticesListLength, arcSize);

        // need to work out the vertex block size from the vertex size flags 

        if (vertexSizeFlags == 131) {
          vertexBlockSize = 40;
        }
        else if (vertexSizeFlags == 139) {
          vertexBlockSize = 48;
        }
        else if (vertexSizeFlags == 143) {
          vertexBlockSize = 64;
        }
        else if (vertexSizeFlags == 155) {
          vertexBlockSize = 56;
        }
        else if (vertexSizeFlags == 159) {
          vertexBlockSize = 60;
        }
        else {
          //ErrorLogger.log("[Viewer_Unity3D_MESH] Unknown Vertex Size Flag: " + vertexSizeFlags);
        }

        int realVertexBlockSize = verticesListLength / numVertices;

        // for v22, at least in Space Tail game, it often doesn't have the 32-byte block, so we want to go back and try to read it properly
        if (version >= 22 && (realVertexBlockSize < 12 || realVertexBlockSize % 4 != 0)) {
          // not an even multiple of 4
          fm.relativeSeek(endOfFaces);

          // 4 - numVertices
          numVertices = fm.readInt();
          FieldValidator.checkNumVertices(numVertices);

          // 4 - numDetailsBlocks
          numDetailBlocks = fm.readInt();
          FieldValidator.checkRange(numDetailBlocks, 0, 50); // guess

          // X*4 - Detail Blocks
          fm.skip(numDetailBlocks * 4);

          // 4 - Vertex Field Length
          verticesListLength = fm.readInt();
          FieldValidator.checkLength(verticesListLength, arcSize);

          realVertexBlockSize = verticesListLength / numVertices;
        }

        // check to see if the vertex block size calculation ends up being even, because if it is, it's probably right.
        if (version >= 22) {

          long vertexStart = fm.getOffset();

          boolean vertexChanged = false;

          // first, check a few floats and see if we can find the appropriate size
          if (realVertexBlockSize > 40) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely, but need to triple check
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  // check #4
                  fm.skip(36);
                  checkFloat = fm.readFloat();
                  if (checkFloat == -1 || checkFloat == 1) {
                    realVertexBlockSize = 40;
                    //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                    vertexChanged = true;

                    knownVertexBlockSize = true;
                  }
                }
              }
            }

            fm.relativeSeek(vertexStart);
          }

          if (!vertexChanged) {
            evenCheck = ((verticesListLength - 16) % numVertices) == 0;
            if (evenCheck) {
              // seems to be split into 3 blocks... numVertices*12, 12 bytes nulls, numVertices*4, 4 bytes null, numVertices*X.
              // we only need the first 12-byte block
              realVertexBlockSize = 12;
            }
            else {
              evenCheck = ((verticesListLength - 12) % numVertices) == 0;
              if (evenCheck) {
                // seems to be split into 3 blocks... numVertices*12, 12 bytes nulls, numVertices*4, 4 bytes null, numVertices*X.
                // we only need the first 12-byte block
                realVertexBlockSize = 24;
              }
            }
          }
        }

        //System.out.println("Vertex Block Size (calculated): " + realVertexBlockSize);
        if (realVertexBlockSize != vertexBlockSize) {
          //ErrorLogger.log("[Viewer_Unity3D_MESH] Vertex Block Size: Flag says " + vertexBlockSize + " but calculation says " + realVertexBlockSize);
          vertexBlockSize = realVertexBlockSize;
        }

        if (vertexBlockSize == 0) {
          // v20 - might be stored in a separate resS file

          long vertexStart = fm.getOffset();
          try {
            fm.skip(208);

            // 4 - Offset to Vertex Data in resS file
            long vertOffset = fm.readInt();

            // 4 - Length of Vertex Data in resS file
            int vertLength = fm.readInt();
            if (vertLength == 0) {
              // vertOffset is probably a 8-byte field
              vertOffset = IntConverter.unsign((int) vertOffset);
              vertLength = fm.readInt();
            }

            // now calculate the vertexBlockSize (and check it looks OK)
            vertexBlockSize = vertLength / numVertices;
            FieldValidator.checkRange(vertexBlockSize, 12, 128);

            // 4 - Filename Length of resS file
            int ressFilenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(ressFilenameLength);

            // X - Filename of resS file
            String ressFilename = fm.readString(ressFilenameLength);

            if (ressFilename.startsWith("archive:/")) {
              int slashPos = ressFilename.indexOf('/', 9);
              if (slashPos > 0) {
                ressFilename = ressFilename.substring(slashPos + 1);
              }
              else {
                ressFilename = ressFilename.substring(9);
              }
            }

            // check that the resS file exists
            File ressFile = new File(FilenameSplitter.getDirectory(Archive.getBasePath()) + File.separator + ressFilename);
            if (ressFile.exists()) {
              long ressArcSize = ressFile.length();
              FieldValidator.checkOffset(vertOffset, ressArcSize);
              FieldValidator.checkLength(vertLength, ressArcSize);

              // if we're here, we found the data in the ress file, so close the FM and open the ress file instead
              fm.close();
              fm = new FileManipulator(ressFile, false);
              fm.seek(vertOffset);

            }

          }
          catch (Throwable t) {
            if (version >= 22) {

              // v22 Sons of the Forest has an extra 4 bytes here

              try {
                fm.relativeSeek(vertexStart);
                fm.skip(212); // extra 4 bytes compared to v20 earlier

                // 4 - Offset to Vertex Data in resS file
                long vertOffset = fm.readInt();

                // 4 - Length of Vertex Data in resS file
                int vertLength = fm.readInt();
                if (vertLength == 0) {
                  // vertOffset is probably a 8-byte field
                  vertOffset = IntConverter.unsign((int) vertOffset);
                  vertLength = fm.readInt();
                }

                // now calculate the vertexBlockSize (and check it looks OK)
                vertexBlockSize = vertLength / numVertices;
                FieldValidator.checkRange(vertexBlockSize, 12, 128);

                // 4 - Filename Length of resS file
                int ressFilenameLength = fm.readInt();
                FieldValidator.checkFilenameLength(ressFilenameLength);

                // X - Filename of resS file
                String ressFilename = fm.readString(ressFilenameLength);

                if (ressFilename.startsWith("archive:/")) {
                  int slashPos = ressFilename.indexOf('/', 9);
                  if (slashPos > 0) {
                    ressFilename = ressFilename.substring(slashPos + 1);
                  }
                  else {
                    ressFilename = ressFilename.substring(9);
                  }
                }

                // check that the resS file exists
                File ressFile = new File(FilenameSplitter.getDirectory(Archive.getBasePath()) + File.separator + ressFilename);
                if (ressFile.exists()) {
                  long ressArcSize = ressFile.length();
                  FieldValidator.checkOffset(vertOffset, ressArcSize);
                  FieldValidator.checkLength(vertLength, ressArcSize);

                  // if we're here, we found the data in the ress file, so close the FM and open the ress file instead
                  fm.close();
                  fm = new FileManipulator(ressFile, false);
                  fm.seek(vertOffset);

                }

              }
              catch (Throwable t2) {
                fm.relativeSeek(vertexStart);
                vertexBlockSize = 0;
              }
            }
            else {
              fm.relativeSeek(vertexStart);
              vertexBlockSize = 0;
            }
          }
        }

        //numVertices = verticesListLength / vertexBlockSize;
        //FieldValidator.checkNumVertices(numVertices);
      }

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      normals = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      // read the vertices
      boolean tryAgain = true;
      boolean repeated = false;

      long vertexStart = fm.getOffset();

      if (Settings.getBoolean("DebugMode")) {
        System.out.println("[Viewer_Unity3D_MESH]: Vertex Start = " + vertexStart);
      }

      while (tryAgain) {
        tryAgain = false;

        /*
        if (!knownVertexBlockSize) {
          // we just guessed the vertex block size, so we need to do a check, to see if the calculation is correct
          if (vertexBlockSize == 48) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely, but need to triple check
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  // check #4
                  fm.skip(36);
                  checkFloat = fm.readFloat();
                  if (checkFloat == -1 || checkFloat == 1) {
                    vertexBlockSize = 40;
                    //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                    knownVertexBlockSize = true;
                  }
                }
              }
            }
          }
          else if (vertexBlockSize == 52) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely, but need to triple check
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  // almost certain
                  vertexBlockSize = 40;
                  //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                  knownVertexBlockSize = true;
                }
              }
            }
          }
          else if (vertexBlockSize == 64) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  vertexBlockSize = 40;
                  //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                  knownVertexBlockSize = true;
                }
              }
            }
          }
          else if (vertexBlockSize == 68) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  vertexBlockSize = 40;
                  //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                  knownVertexBlockSize = true;
                }
              }
            }
          }
          else if (vertexBlockSize == 76) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  vertexBlockSize = 40;
                  //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                  knownVertexBlockSize = true;
                }
              }
            }
          }
        
          fm.relativeSeek(vertexStart);
        }
        */
        if (!knownVertexBlockSize) {
          // we just guessed the vertex block size, so we need to do a check, to see if the calculation is correct
          if (vertexBlockSize > 40) {
            fm.skip(36);
            float checkFloat = fm.readFloat();
            if (checkFloat == -1 || checkFloat == 1) {
              // might be a 40-byte block
              fm.skip(36);
              checkFloat = fm.readFloat();
              if (checkFloat == -1 || checkFloat == 1) {
                // highly likely, but need to triple check
                fm.skip(36);
                checkFloat = fm.readFloat();
                if (checkFloat == -1 || checkFloat == 1) {
                  // check #4
                  fm.skip(36);
                  checkFloat = fm.readFloat();
                  if (checkFloat == -1 || checkFloat == 1) {
                    vertexBlockSize = 40;
                    //System.out.println("Vertex Block Size (changed): " + vertexBlockSize);
                    knownVertexBlockSize = true;
                  }
                }
              }
            }
          }

          fm.relativeSeek(vertexStart);
        }

        // reset the error detector
        tripleZeroCount = 0;

        if (vertexBlockSize == 12) {
          readVertices12(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 16) {
          readVertices16(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 20) {
          readVertices20(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 24) {
          readVertices24(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 28) {
          readVertices28(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 32) {
          readVertices32(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 36) {
          readVertices36(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 40) {
          readVertices40(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 44) {
          readVertices44(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 48) {
          readVertices48(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 52) {
          readVertices52(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 56) {
          readVertices56(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 60) {
          readVertices60(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 64) {
          readVertices64(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 68) {
          readVertices68(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 72) {
          readVertices72(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 76) {
          readVertices76(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 80) {
          readVertices80(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 84) {
          readVertices84(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 88) {
          readVertices88(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 92) {
          readVertices92(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 96) {
          readVertices96(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 100) {
          readVertices100(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 104) {
          readVertices104(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 108) {
          readVertices108(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 112) {
          readVertices112(fm, numVertices, points, normals, texCoords);
        }
        else {
          ErrorLogger.log("[Viewer_Unity3D_MESH] Can't handle vertex block size: " + vertexBlockSize);
        }

        /*
        int boundsCheck = 0;
        
        int minXCheck = (int) (minX * 100);
        int minYCheck = (int) (minY * 100);
        int minZCheck = (int) (minZ * 100);
        
        if (minXCheck == -99 || minXCheck == -100) {
          boundsCheck++;
        }
        if (minYCheck == -99 || minYCheck == -100) {
          boundsCheck++;
        }
        if (minZCheck == -99 || minZCheck == -100) {
          boundsCheck++;
        }
        
        int maxXCheck = (int) (maxX * 100);
        int maxYCheck = (int) (maxY * 100);
        int maxZCheck = (int) (maxZ * 100);
        
        if (maxXCheck == 99 || maxXCheck == 100) {
          boundsCheck++;
        }
        if (maxYCheck == 99 || maxYCheck == 100) {
          boundsCheck++;
        }
        if (maxZCheck == 99 || maxZCheck == 100) {
          boundsCheck++;
        }
        */

        /*
        //if (!knownVertexBlockSize && !repeated && (unknownIntFlag != 0 || boundsCheck > 2)) {
        if (evenCheck && version >= 22) {
          // probably OK, don't repeat. Only for version 22 and above, for now, based on testing
        }
        else if (!knownVertexBlockSize && !repeated && (unknownIntFlag != 0)) {
          //System.out.println("Bounds Check Failed: " + boundsCheck);
          // some v15/17 meshes seem to have the vertexes before the texcoords, but don't know how to detect it, so try to pick it up here.
        
          // reset the min/max so the zoom is correct
          minX = 20000f;
          maxX = -20000f;
          minY = 20000f;
          maxY = -20000f;
          minZ = 20000f;
          maxZ = -20000f;
        
          fm.relativeSeek(vertexStart);
          tryAgain = true;
        
          if (version >= 22) {
            // if v22 doesn't work, try vertex size 24, and then try vertex size 12
            if (vertexBlockSize == 24) {
              vertexBlockSize = 12;
              repeated = true;
            }
            else {
              vertexBlockSize = 24;
              repeated = false;
            }
          }
          else {
            if (vertexBlockSize > 40) {
              vertexBlockSize = 40;
            }
            else if (vertexBlockSize > 24) {
              vertexBlockSize = 24;
            }
            else {
              vertexBlockSize -= 8;
            }
        
            repeated = true;
          }
        
        }
        */

      }

      int originalVertexBlockSize = vertexBlockSize;

      if (knownVertexBlockSize) {
        tripleZeroCount = 0; // if we KNOW the vertex block size, we don't want to do error-correction on it
      }
      else if (version <= 17) {
        tripleZeroCount = 0; // the problem doesn't seem to occur with earlier meshes
      }
      else {
        // not known block size, so guessing - do a check

        if (Settings.getBoolean("UnityMeshExtendedErrorChecking")) {
          //
          //
          // 3.16 We're actually going to try and check the calculated Center and Diff here, and if it's really close to the values in the header, it's probably OK
          //
          //
          float checkRadiusX = (maxX - minX) / 2;
          float checkRadiusY = (maxY - minY) / 2;
          float checkRadiusZ = (maxZ - minZ) / 2;

          float checkCenterX = minX + checkRadiusX;
          float checkCenterY = minY + checkRadiusY;
          float checkCenterZ = minZ + checkRadiusZ;

          checkRadiusX -= radiusXHeader;
          checkRadiusY -= radiusYHeader;
          checkRadiusZ -= radiusZHeader;

          checkCenterX -= centerXHeader;
          checkCenterY -= centerYHeader;
          checkCenterZ -= centerZHeader;

          if (Settings.getBoolean("DebugMode")) {
            System.out.println("[Viewer_Unity3D_MESH]: Bounding Box Check Values: " + checkRadiusX + "\t" + checkRadiusY + "\t" + checkRadiusZ + "\t" + checkCenterX + "\t" + checkCenterY + "\t" + checkCenterZ + "\t");
          }

          //checkRadiusX *= 1000;
          //checkRadiusY *= 1000;
          //checkRadiusZ *= 1000;
          // x100 is close enough, x1000 was being too strict and was falsely erroring out on some meshes
          checkRadiusX *= 100;
          checkRadiusY *= 100;
          checkRadiusZ *= 100;

          if (checkRadiusX < 0) {
            checkRadiusX = 0 - checkRadiusX;
          }
          if (checkRadiusY < 0) {
            checkRadiusY = 0 - checkRadiusY;
          }
          if (checkRadiusZ < 0) {
            checkRadiusZ = 0 - checkRadiusZ;
          }

          if (((int) checkRadiusX) >= 0 && ((int) checkRadiusX) <= 1 && ((int) checkRadiusY) >= 0 && ((int) checkRadiusY) <= 1 && ((int) checkRadiusZ) >= 0 && ((int) checkRadiusZ) <= 1) {
            // the radius checks out, so this was probably the right vertex block size
            tripleZeroCount = 0;
          }
          else {
            tripleZeroCount = 999;
          }
        }

      }

      // 3.16 Added lots of error checking here, to better detect bad vertex size calculations
      while (tripleZeroCount > 4) {
        // if we have a high "triple zero", then we probably read the mesh wrong, so try a smaller size
        // FIRST, TRY SOME COMMON SIZES

        if (Settings.getBoolean("DebugMode")) {
          System.out.println("[Viewer_Unity3D_MESH]: High triple zero count (" + tripleZeroCount + ") for vertex size " + vertexBlockSize);
        }

        tripleZeroCount = 0;

        minX = 20000f;
        maxX = -20000f;
        minY = 20000f;
        maxY = -20000f;
        minZ = 20000f;
        maxZ = -20000f;

        fm.relativeSeek(vertexStart);

        if (vertexBlockSize > 40) {
          vertexBlockSize = 40;
        }
        else if (vertexBlockSize > 24) {
          vertexBlockSize = 24;
        }
        else if (vertexBlockSize > 12) {
          vertexBlockSize = 12;
        }
        else {
          tripleZeroCount = 999;
          break;
        }

        if (vertexBlockSize == 12) {
          readVertices12(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 24) {
          readVertices24(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 40) {
          readVertices40(fm, numVertices, points, normals, texCoords);
        }
        else {
          break;
        }

        //
        //
        // 3.16 We're actually going to try and check the calculated Center and Diff here, and if it's really close to the values in the header, it's probably OK
        //
        //
        float checkRadiusX = (maxX - minX) / 2;
        float checkRadiusY = (maxY - minY) / 2;
        float checkRadiusZ = (maxZ - minZ) / 2;

        float checkCenterX = minX + checkRadiusX;
        float checkCenterY = minY + checkRadiusY;
        float checkCenterZ = minZ + checkRadiusZ;

        checkRadiusX -= radiusXHeader;
        checkRadiusY -= radiusYHeader;
        checkRadiusZ -= radiusZHeader;

        checkCenterX -= centerXHeader;
        checkCenterY -= centerYHeader;
        checkCenterZ -= centerZHeader;

        if (Settings.getBoolean("DebugMode")) {
          System.out.println("[Viewer_Unity3D_MESH]: Bounding Box Check Values: Radius:\t" + checkRadiusX + "\t" + checkRadiusY + "\t" + checkRadiusZ + "\tCenter:\t" + checkCenterX + "\t" + checkCenterY + "\t" + checkCenterZ + "\t");
        }

        checkRadiusX *= 1000;
        checkRadiusY *= 1000;
        checkRadiusZ *= 1000;

        if (checkRadiusX < 0) {
          checkRadiusX = 0 - checkRadiusX;
        }
        if (checkRadiusY < 0) {
          checkRadiusY = 0 - checkRadiusY;
        }
        if (checkRadiusZ < 0) {
          checkRadiusZ = 0 - checkRadiusZ;
        }

        if (((int) checkRadiusX) >= 0 && ((int) checkRadiusX) <= 1 && ((int) checkRadiusY) >= 0 && ((int) checkRadiusY) <= 1 && ((int) checkRadiusZ) >= 0 && ((int) checkRadiusZ) <= 1) {
          // the radius checks out, so this was probably the right vertex block size
          tripleZeroCount = 0;
        }
        else {
          tripleZeroCount = 999;
        }

      }

      if (tripleZeroCount > 4) {
        // reset back to the original size, then go down slowly to find the right one
        vertexBlockSize = originalVertexBlockSize;
      }

      // 3.16 Added lots of error checking here, to better detect bad vertex size calculations
      while (tripleZeroCount > 4) {
        // if we have a high "triple zero", then we probably read the mesh wrong, so try a smaller size

        if (Settings.getBoolean("DebugMode")) {
          System.out.println("[Viewer_Unity3D_MESH]: High triple zero count (" + tripleZeroCount + ") for vertex size " + vertexBlockSize);
        }

        tripleZeroCount = 0;

        minX = 20000f;
        maxX = -20000f;
        minY = 20000f;
        maxY = -20000f;
        minZ = 20000f;
        maxZ = -20000f;

        fm.relativeSeek(vertexStart);

        vertexBlockSize -= 4;

        if (vertexBlockSize == 12) {
          readVertices12(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 16) {
          readVertices16(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 20) {
          readVertices20(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 24) {
          readVertices24(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 28) {
          readVertices28(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 32) {
          readVertices32(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 36) {
          readVertices36(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 40) {
          readVertices40(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 44) {
          readVertices44(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 48) {
          readVertices48(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 52) {
          readVertices52(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 56) {
          readVertices56(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 60) {
          readVertices60(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 64) {
          readVertices64(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 68) {
          readVertices68(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 72) {
          readVertices72(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 76) {
          readVertices76(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 80) {
          readVertices80(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 84) {
          readVertices84(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 88) {
          readVertices88(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 92) {
          readVertices92(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 96) {
          readVertices96(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 100) {
          readVertices100(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 104) {
          readVertices104(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 108) {
          readVertices108(fm, numVertices, points, normals, texCoords);
        }
        else if (vertexBlockSize == 112) {
          readVertices112(fm, numVertices, points, normals, texCoords);
        }

      }

      // 164 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown Float?
      // 4 - Unknown Float?
      // 4 - Unknown Float?
      // 4 - Unknown Float?
      // 4 - Unknown (6/0)
      // 4 - null
      // 4 - null

      if (faces != null && points != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        triangleMesh.getNormals().addAll(normals);

        faces = null;
        points = null;
        normals = null;
        texCoords = null;
      }

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      Point3D sizes = new Point3D(diffX, diffY, diffZ);
      Point3D center = new Point3D(centerX, centerY, centerZ);

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);

      return preview;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices12(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices16(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Float
      fm.skip(4);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices20(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Float
      // 4 - Float
      fm.skip(8);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices24(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices28(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      //System.out.println(xPoint + "\t" + yPoint + "\t" + zPoint);

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      fm.skip(4);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices32(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      //System.out.println(xPoint + "\t" + yPoint + "\t" + zPoint);

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices36(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(12);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices40(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    int singleZeroCount = 0;

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      //System.out.println(xPoint + "\t" + yPoint + "\t" + zPoint);

      // 3.16 Added lots of error checking here, to better detect bad vertex size calculations
      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }
      // Special "extra cases" here because "40" is one of the common ones. This checks for double-zeros and single zeros
      else if ((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1 || zPoint == 0 || zPoint == 1)) {
        tripleZeroCount++;
      }
      else if ((yPoint == 0 || yPoint == 1) && (xPoint == 0 || xPoint == 1 || zPoint == 0 || zPoint == 1)) {
        tripleZeroCount++;
      }
      else if ((zPoint == 0 || zPoint == 1) && (yPoint == 0 || yPoint == 1 || xPoint == 0 || xPoint == 1)) {
        tripleZeroCount++;
      }
      else if (xPoint == 0 || xPoint == 1 || yPoint == 0 || yPoint == 1 || zPoint == 0 || zPoint == 1) {
        singleZeroCount++; // single zero count
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(16);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

    if (singleZeroCount > 10) {
      tripleZeroCount += singleZeroCount;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices44(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown
      fm.skip(20);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices48(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(24);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices52(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(28);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices56(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(32);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices60(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      //System.out.println(xPoint + "\t" + yPoint + "\t" + zPoint);

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 28 - Unknown
      fm.skip(28);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices64(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 32 - Unknown
      fm.skip(32);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices68(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 36 - Unknown
      fm.skip(36);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices72(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 40 - Unknown
      fm.skip(40);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices76(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 44 - Unknown
      fm.skip(44);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices80(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 48 - Unknown
      fm.skip(48);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices84(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 52 - Unknown
      fm.skip(52);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices88(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 56 - Unknown
      fm.skip(56);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices92(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 60 - Unknown
      fm.skip(60);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices96(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 64 - Unknown
      fm.skip(64);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices100(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 68 - Unknown
      fm.skip(68);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices104(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 72 - Unknown
      fm.skip(72);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices108(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 76 - Unknown
      fm.skip(76);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readVertices112(FileManipulator fm, int numVertices, float[] points, float[] normals, float[] texCoords) {

    for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
      // 4 - Vertex X
      // 4 - Vertex Y
      // 4 - Vertex Z
      float xPoint = fm.readFloat();
      float yPoint = fm.readFloat();
      float zPoint = fm.readFloat();

      if (((xPoint == 0 || xPoint == 1) && (yPoint == 0 || yPoint == 1) && (zPoint == 0 || zPoint == 1)) || (xPoint == yPoint && yPoint == zPoint)) {
        tripleZeroCount++;
      }

      points[j] = xPoint;
      points[j + 1] = yPoint;
      points[j + 2] = zPoint;

      // 4 - Normal X
      // 4 - Normal Y
      // 4 - Normal Z
      float xNormal = fm.readFloat();
      float yNormal = fm.readFloat();
      float zNormal = fm.readFloat();

      normals[j] = xNormal;
      normals[j + 1] = yNormal;
      normals[j + 2] = zNormal;

      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(8);

      // 80 - Unknown
      fm.skip(80);

      // Don't know where the texture co-ords are yet
      float xTexture = 0;
      float yTexture = 0;

      texCoords[k] = xTexture;
      texCoords[k + 1] = yTexture;

      // Calculate the size of the object
      if (xPoint < minX) {
        minX = xPoint;
      }
      if (xPoint > maxX) {
        maxX = xPoint;
      }

      if (yPoint < minY) {
        minY = yPoint;
      }
      if (yPoint > maxY) {
        maxY = yPoint;
      }

      if (zPoint < minZ) {
        minZ = zPoint;
      }
      if (zPoint > maxZ) {
        maxZ = zPoint;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel panel, FileManipulator destination) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource readThumbnail(FileManipulator source) {
    try {
      PreviewPanel preview = read(source);
      if (preview == null || !(preview instanceof PreviewPanel_3DModel)) {
        return null;
      }

      PreviewPanel_3DModel preview3D = (PreviewPanel_3DModel) preview;

      // generate a thumbnail-sized snapshot
      int thumbnailSize = 150; // bigger than ImageResource, so it is shrunk (and smoothed as a result)
      preview3D.generateSnapshot(thumbnailSize, thumbnailSize);

      Image image = preview3D.getImage();
      if (image != null) {
        ImageResource resource = new ImageResource(image, preview3D.getImageWidth(), preview3D.getImageHeight());
        preview3D.onCloseRequest(); // cleanup memory
        return resource;
      }

      preview3D.onCloseRequest(); // cleanup memory

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}