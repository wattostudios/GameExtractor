/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.plugin.archive.Plugin_BGD;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BGD_GEO_MGGF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BGD_GEO_MGGF() {
    super("BGD_GEO_MGGF", "BGD_GEO_MGGF Model");
    setExtensions("geo");

    setGames("Redline");
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
      if (readPlugin instanceof Plugin_BGD) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readString(4).equals("MGGF")) {
        rating += 50;
      }

      if (fm.readInt() == 3) {
        rating += 5;
      }

      if (FieldValidator.checkNumFaces(fm.readInt())) {
        rating += 5;
      }

      if (FieldValidator.checkNumVertices(fm.readInt())) {
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

      // Read in the model

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      // 4 - Header (MGGF)
      // 4 - Unknown (3)
      fm.skip(8);

      // 4 - Number of Face Triplets
      int numFaces = fm.readInt();
      FieldValidator.checkNumFaces(numFaces);

      // 4 - Number of Vertices
      int numVertices = fm.readInt();
      FieldValidator.checkNumFaces(numVertices);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts + 1); // allow 0 materials

      // 4 - null
      // for (12)
      //   4 - Unknown Float
      fm.skip(52);

      // Find the Part Index Numbers
      int[] partVertexStarts = new int[numParts];
      int[] partVertexCounts = new int[numParts];
      int[] partFaceStarts = new int[numParts];
      int[] partFaceCounts = new int[numParts];
      for (int p = 0; p < numParts; p++) {
        // 50 - Texture Filename (null terminated, filled with nulls)
        // 40 - Part Name (null terminated, filled with nulls)
        // 4 - Unknown
        // 2 - Unknown
        fm.skip(96);

        // 2 - First Vertex Index for this Part
        int partVertexStart = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkRange(partVertexStart, 0, numVertices);
        partVertexStarts[p] = partVertexStart;

        // 2 - Number of Vertices in this Part
        int partVertexCount = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkRange(partVertexStart + partVertexCount, 0, numVertices);
        partVertexCounts[p] = partVertexCount;

        // 2 - Offset to the Start of the Face Triplets for this Part [*2] (relative to the start of the Faces Directory)
        fm.skip(2);

        // 2 - Number of Face Triplets for this Part
        int partFaceCount = ShortConverter.unsign(fm.readShort());
        FieldValidator.checkRange(partFaceCount, 0, numFaces);
        partFaceCounts[p] = partFaceCount;

        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(8);
      }
      int facePos = 0;
      for (int p = 0; p < numParts; p++) {
        partFaceStarts[p] = facePos;
        facePos += partFaceCounts[p];
      }

      //
      //
      // FACES
      //
      //
      int numFaces3 = numFaces * 3;
      FieldValidator.checkNumFaces(numFaces3);

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

      //
      //
      // VERTICES
      //
      //

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];

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

        // Don't know where the texture co-ords are yet
        float xTexture = 0;
        float yTexture = 0;

        texCoords[k] = xTexture;
        texCoords[k + 1] = yTexture;

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(24);

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
      // RENDER THE MESH
      //

      if (faces != null && points != null && texCoords != null) {
      }
      else {
        return null;
      }

      // Now build each part
      MeshView[] meshView = new MeshView[numParts];

      for (int p = 0; p < numParts; p++) {

        // Create the Mesh for this Part
        TriangleMesh triangleMesh = new TriangleMesh();

        // create small arrays for the faces and points for this Part only
        int partVertexStart = partVertexStarts[p] * 3; // blocks of 3
        int partVertexCount = partVertexCounts[p] * 3;
        int partFaceStart = partFaceStarts[p] * 6; // blocks of 6
        int partFaceCount = partFaceCounts[p] * 6;

        int partTexCount = partVertexCount / 3 * 2;

        float[] partPoints = new float[partVertexCount];
        int[] partFaces = new int[partFaceCount];
        float[] partTex = new float[partTexCount];

        System.arraycopy(points, partVertexStart, partPoints, 0, partVertexCount);
        System.arraycopy(faces, partFaceStart, partFaces, 0, partFaceCount);
        System.arraycopy(texCoords, 0, partTex, 0, partTexCount); // we don't have tex coords, so just grab from the start, they're all blank anyway

        triangleMesh.getTexCoords().addAll(partTex);

        triangleMesh.getPoints().addAll(partPoints);
        triangleMesh.getFaces().addAll(partFaces);

        // Create the MeshView
        MeshView view = new MeshView(triangleMesh);
        meshView[p] = view;

      }

      // cleanup
      if (faces != null && points != null && texCoords != null) {

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

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

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