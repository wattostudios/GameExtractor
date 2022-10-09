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

package org.watto.ge.plugin.viewer;

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARCH00_LTAR;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARCH00_LTAR_MODEL00P_MODL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARCH00_LTAR_MODEL00P_MODL() {
    super("ARCH00_LTAR_MODEL00P_MODL", "FEAR MODEL00P Model");
    setExtensions("model00p");

    setGames("FEAR");
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

      ArchivePlugin readPlugin = Archive.getReadPlugin();
      if (readPlugin instanceof Plugin_ARCH00_LTAR) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readString(5).equals("MODL!")) {
        rating += 50;
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 8 - Header ("MODL!" + 3x nulls)
      // 4 - Unknown (1/9)
      fm.skip(12);

      // 4 - Number of 8-Byte Groups
      int group8Count = fm.readInt();
      FieldValidator.checkPositive(group8Count);

      // 4 - Number of 39-Byte Blocks
      int block39Count = fm.readInt();
      FieldValidator.checkPositive(block39Count);

      // 4 - Unknown (1/7)
      // 4 - Unknown (1/4)
      // 4 - Unknown (1/7)
      fm.skip(12);

      // 4 - Number of 40-byte blocks? (0/5)
      int block40Count = fm.readInt();
      FieldValidator.checkPositive(block40Count);

      // 4 - Unknown (0/5/2)
      // 4 - Unknown (1)
      fm.skip(8);

      // 4 - Part Name Directory Length
      int partLength = fm.readInt();
      FieldValidator.checkLength(partLength, arcSize);

      // 4 - null
      fm.skip(4);

      // 4 - Number of 81-byte Blocks? (0/11)
      int block81Count = fm.readInt();
      FieldValidator.checkPositive(block81Count);

      // 4 - Unknown (1/7)
      // 12 - null
      // 4 - Unknown (0/4)
      // 4 - Unknown (0/6)
      // 8 - null
      fm.skip(32);

      // 4 - 20-byte Block Length
      int block20Length = fm.readInt();
      FieldValidator.checkLength(block20Length, arcSize);

      // PART NAMES
      fm.skip(partLength);

      // for each 39-byte block
      //    4 - Unknown
      //    2 - Unknown ID (incremental from 0)
      //    1 - Unknown (0/2)
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //   4 - Unknown
      System.out.println("Block39 at " + fm.getOffset() + " for " + block39Count);
      fm.skip(block39Count * 39);

      // 4 - null
      // 4 - Unknown
      fm.skip(8);

      // for each 39-byte block
      //   4 - Unknown
      fm.skip(block39Count * 4);

      // 20-BYTE BLOCK
      System.out.println("Block20 at " + fm.getOffset() + " for " + block20Length);
      fm.skip(block20Length);

      // Sometimes there's other data here, not sure how to read it, so try to skip over it by finding the next block

      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      // 4 - Unknown Float
      fm.skip(16);

      int currentInt = fm.readInt();
      for (int i = 0; i < 2000; i++) {
        // 4 - Unknown (4)

        if (currentInt == 4) {
          currentInt = fm.readInt();

          //4 - Unknown (200/0)
          if (currentInt == 4) {
            continue;
          }

          //12 - null
          currentInt = fm.readInt();
          if (currentInt == 0) {
            currentInt = fm.readInt();
            if (currentInt == 0) {
              currentInt = fm.readInt();
              if (currentInt == 0 || currentInt == 1) {
                // found the 3 nulls
                System.out.println("skipping to offset " + fm.getOffset());
                break;

              }
            }
          }

        }
        else {
          currentInt = fm.readInt();
        }

      }

      /*
      
      // 4 - Number of 8-byte Blocks
      int block8Count = fm.readInt();
      FieldValidator.checkPositive(block8Count);
      
      // for each 8-byte block
      //   4 - Unknown
      //   4 - null
      System.out.println("Block8 at " + fm.getOffset() + " for " + block8Count);
      fm.skip(block8Count * 8);
      
      // 4 - Number of 8Plus-byte Blocks
      int block8PlusCount = fm.readInt();
      
      while (fm.getOffset() < arcSize && (block8PlusCount > 1000 || block8PlusCount == 0)) {
        // might be another set of floats followed by 8-byte blocks (realistically, should be able to use the 8-byte groups value in the header for this, but it works, so leave it)
      
        // 4 - Unknown Float (ALREADY READ THIS)
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
      
        // 4 - Unknown (4)
        // 4 - Unknown (100/0)
        // 12 - null
        fm.skip(32);
      
        // 4 - Number of 8-byte Blocks
        block8Count = fm.readInt();
        FieldValidator.checkPositive(block8Count);
      
        // for each 8-byte block
        //   4 - Unknown
        //   4 - null
        System.out.println("Block8 (#2) at " + fm.getOffset() + " for " + block8Count);
        fm.skip(block8Count * 8);
      
        // 4 - Number of 8Plus-byte Blocks
        block8PlusCount = fm.readInt();
      
      }
      FieldValidator.checkPositive(block8PlusCount);
      */
      // 8-BYTE GROUPS
      for (int i = 0; i < group8Count; i++) {
        if (i == 0) {
          // don't read this - we already read it above as a way of finding the right place in the file
        }
        else {
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          fm.skip(16);

          // 4 - Unknown (4)
          // 4 - Unknown (100/0)
          // 12 - null
          fm.skip(20);
        }

        // 4 - Number of 8-byte Blocks
        int block8Count = fm.readInt();
        FieldValidator.checkPositive(block8Count);

        // for each 8-byte block
        //   4 - Unknown
        //   4 - null
        System.out.println("Block8 at " + fm.getOffset() + " for " + block8Count);
        fm.skip(block8Count * 8);
      }

      // 4 - Number of 8Plus-byte Blocks
      int block8PlusCount = fm.readInt();
      FieldValidator.checkPositive(block8PlusCount);

      // for each 8Plus-byte block
      System.out.println("Block8Plus at " + fm.getOffset() + " for " + block8PlusCount);
      for (int i = 0; i < block8PlusCount; i++) {
        //   4 - Unknown
        fm.skip(4);

        //   4 - Number of 4-byte entries in this block
        int numEntries = fm.readInt();
        FieldValidator.checkRange(numEntries, 0, 50000);

        //    for each 4-byte entry in this block
        //      4 - Unknown
        fm.skip(numEntries * 4);
      }

      // 4 - Number of 40-byte Blocks
      fm.skip(4); // already read this in the header

      // for each 40-byte block
      //   4 - Unknown
      //    4 - Unknown
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      //    4 - Unknown Float
      System.out.println("Block40 at " + fm.getOffset() + " for " + block40Count);
      fm.skip(block40Count * 40);

      // 4 - Number of External Filenames [-1]
      int numFilenames = fm.readInt() - 1;
      FieldValidator.checkPositive(numFilenames);

      // for each External Filename
      for (int i = 0; i < numFilenames; i++) {
        //   2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        //   X - Filename
        fm.skip(filenameLength);
      }

      // 1 - Unknown (1) (end of filename flag?)
      // 4 - Unknown (0/2)
      // 4 - Unknown
      fm.skip(9);

      // 4 - Number of 81-byte blocks
      fm.skip(4); // already read this in the header

      if (fm.getOffset() > arcSize) {
        return null; // early exit in case of invalid mesh
      }

      // for each 81-byte block
      System.out.println("Block81 at " + fm.getOffset() + " for " + block81Count);
      for (int i = 0; i < block81Count; i++) {
        //   41 - Unknown
        fm.skip(41);

        //    4 - Number of Entries in this block
        int numEntries = fm.readInt();
        FieldValidator.checkRange(numEntries, 0, 50000);

        if (numEntries == 2) {
          fm.skip(24);
        }
        else if (numEntries == 3) {
          fm.skip(12);
        }
        else if (numEntries == 6) {
          fm.skip(116);
        }
        else if (numEntries == 7) {
          fm.skip(36);
        }

      }

      // 4 - Number of 112-byte blocks
      int block112Count = fm.readInt();
      FieldValidator.checkRange(block112Count, 0, 50000);

      if (fm.getOffset() > arcSize) {
        return null; // early exit in case of invalid mesh
      }

      // for each 112-byte block
      System.out.println("Block112 at " + fm.getOffset() + " for " + block112Count);
      for (int i = 0; i < block112Count; i++) {

        //   4 - Number of Entries in this Block
        int numEntries = fm.readInt();
        FieldValidator.checkRange(numEntries, 0, 50000);

        //    4 - Unknown
        //    4 - Unknown
        //    36 - Unknown
        fm.skip(44);

        //   for each entry in this block
        //       16 - Unknown
        fm.skip(numEntries * 16);
      }

      if (fm.getOffset() > arcSize) {
        return null; // early exit in case of invalid mesh
      }

      // 4 - null
      int block13Count = fm.readInt();
      FieldValidator.checkRange(block13Count, 0, 50000);

      // Block13
      System.out.println("Block13 at " + fm.getOffset() + " for " + block13Count);
      //fm.skip(block13Count * 13);
      for (int i = 0; i < block13Count; i++) {
        // 1 - Unknown
        fm.skip(1);

        // 4 - Float Block Count
        int numEntries = fm.readShort();
        int numEntries2 = fm.readShort();
        FieldValidator.checkRange(numEntries, 0, 100);

        if (numEntries2 == 256) {
          fm.skip(98);
        }
        else if (numEntries == 0) {
          fm.skip(8);
        }
        else if (numEntries == 3) {
          fm.skip(188);
        }

      }

      // 4 - Number of 23-byte blocks
      int block23Count = fm.readInt();
      for (int i = 0; i < 100; i++) {
        if (block23Count < 0 || block23Count > 1000) {
          fm.skip(5);
          block23Count = fm.readInt();
        }
        else {
          break;
        }
      }
      FieldValidator.checkRange(block23Count, 0, 50000);

      // for each 23-byte block
      //    4 - Unknown
      //    4 - Unknown (1)
      //    4 - null
      //    2 - Unknown
      //    4 - Unknown (256/257)
      //    1 - null
      //    2 - Unknown ID (incremental from 0)
      //   2 - null
      System.out.println("Block23 at " + fm.getOffset() + " for " + block23Count);
      fm.skip(block23Count * 23);

      // 4 - Unknown (0/1/2)
      // 4 - Unknown (0/1/2)
      // 4 - Unknown (1/2/14)
      fm.skip(12);

      // 4 - Unknown (0/2)
      int block2Count = fm.readInt();
      if (block2Count < 0 || block2Count > 50000) {
        fm.skip(7);
        block2Count = fm.readInt();
      }
      FieldValidator.checkRange(block2Count, 0, 50000);
      System.out.println("Block2 at " + fm.getOffset() + " for " + block2Count);

      fm.skip(block2Count * 2);

      // 4 - Unknown (1/7)
      // 4 - Unknown (1/5)
      fm.skip(8);

      System.out.println("Vertex+Face at " + fm.getOffset());

      // 4 - Vertex Directory Length
      int vertexDirLength = fm.readInt();
      FieldValidator.checkLength(vertexDirLength, arcSize);

      // 4 - Face Index Directory Length
      int faceDirLength = fm.readInt();
      FieldValidator.checkLength(faceDirLength, arcSize);

      //
      //
      // VERTICES
      //
      //

      int numVertices = vertexDirLength / 64;
      FieldValidator.checkNumVertices(numVertices);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Point X (float)
        // 4 - Point Y (float)
        // 4 - Point Z (float)
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        // 4 - Normal X (float)
        // 4 - Normal Y (float)
        // 4 - Normal Z (float)

        // 4 - Tex Co-Ords U (float)
        // 4 - Tex Co-Ords V (float)

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float

        // 2 - null
        // 2 - Unknown (255)
        // 4 - null
        fm.skip(52);

        // skip tex-coords for now
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

      //
      //
      // FACES
      //
      //
      int numFaces = faceDirLength / 2;
      FieldValidator.checkNumFaces(numFaces);

      int numFaces3 = numFaces;
      FieldValidator.checkNumFaces(numFaces3);

      numFaces = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

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

      // add the part to the model
      if (faces != null && points != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);

        faces = null;
        points = null;
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
    catch (

    Throwable t) {
      ErrorLogger.log(t);
      return null;
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