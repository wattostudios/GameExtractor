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

import java.awt.Image;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_VPK;
import org.watto.ge.plugin.archive.Plugin_VPK_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VPK_VPK_VMESHC extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VPK_VPK_VMESHC() {
    super("VPK_VPK_VMESHC", "Valve VMESH_C Model");
    setExtensions("vmesh_c");

    setGames("Valve Engine");
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
      if (readPlugin instanceof Plugin_VPK || readPlugin instanceof Plugin_VPK_2) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      fm.skip(4);

      // 2 - Version Major (12)
      if (fm.readShort() == 12) {
        rating += 5;
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
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Unknown
      // 2 - Version Major? (12)
      // 2 - Version Minor? (1)
      // 4 - Unknown (8)
      // 4 - Unknown (4)
      fm.skip(16);

      // 4 - Header (RERL)
      // 4 - Unknown (44)
      // 4 - Unknown (53)
      // 4 - Header (REDI)
      // 4 - Unknown (88)
      // 4 - REDI Block Length
      fm.skip(24);

      // 4 - Header (DATA)
      fm.skip(4);

      // 4 - DATA Block 1 Length (not including these 3 header fields)
      int data1Length = fm.readInt();
      FieldValidator.checkLength(data1Length, arcSize);

      // 4 - DATA Block 2 Length (not including these 3 header fields)
      int data2Length = fm.readInt();
      FieldValidator.checkLength(data2Length, arcSize);

      // X - DATA Block 1
      // X - DATA Block 2
      fm.skip(data1Length + data2Length);

      // 4 - Unknown (32)
      fm.skip(4);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts);

      int[] vertexCounts = new int[numParts];
      int[] faceCounts = new int[numParts];

      long[] vertexOffsets = new long[numParts];
      long[] faceOffsets = new long[numParts];

      for (int i = 0; i < numParts; i++) {
        // 4 - Number of Vertices
        int numVertices = fm.readInt();
        FieldValidator.checkNumFaces(numVertices);

        vertexCounts[i] = numVertices;

        // 4 - Vertex Block Size (20)
        // 4 - Unknown (40)
        // 4 - Unknown (3)
        fm.skip(12);

        // 4 - Vertex Data Offset (relative to the start of this field)
        long vertexOffset = fm.getOffset() + fm.readInt();
        FieldValidator.checkOffset(vertexOffset, arcSize);

        vertexOffsets[i] = vertexOffset;

        // 4 - Vertex Data Length
        fm.skip(4);
      }

      for (int i = 0; i < numParts; i++) {
        // 4 - Number of Face Indexes
        int numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        faceCounts[i] = numFaces;

        // 4 - Face Index Block Size (2)
        // 4 - null
        // 4 - null
        fm.skip(12);

        // 4 - Face Index Data Offset (relative to the start of this field)
        long faceOffset = fm.getOffset() + fm.readInt();
        FieldValidator.checkOffset(faceOffset, arcSize);

        faceOffsets[i] = faceOffset;

        // 4 - Face Index Data Length
        fm.skip(4);
      }

      int partOffset = 0; // so each part references the right points
      for (int p = 0; p < numParts; p++) {

        long vertexOffset = vertexOffsets[p];
        int numVertices = vertexCounts[p];

        long faceOffset = faceOffsets[p];
        int numFaces = faceCounts[p];
        //
        //
        // VERTICES
        //
        //
        fm.seek(vertexOffset);

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];
        normals = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

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

        //
        //
        // FACES
        //
        //
        fm.seek(faceOffset);

        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = (ShortConverter.unsign(fm.readShort())) + partOffset;
          int facePoint2 = (ShortConverter.unsign(fm.readShort())) + partOffset;
          int facePoint3 = (ShortConverter.unsign(fm.readShort())) + partOffset;

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

        // get ready for the next part
        partOffset += numVertices;

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