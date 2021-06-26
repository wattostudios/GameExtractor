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
import org.watto.ge.plugin.archive.Plugin_TOD_TOD2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TOD_TOD2_MESH_MESH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TOD_TOD2_MESH_MESH() {
    super("TOD_TOD2_MESH_MESH", "Pacman TOD2 MESH Viewer");
    setExtensions("mesh");

    setGames("Pacman: Adventures In Time");
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
      if (plugin instanceof Plugin_TOD_TOD2) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals("MESH") || header.equals("MATL")) {
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
  @SuppressWarnings("unused")
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

      // 4 - Header (MESH)
      String header = fm.readString(4);

      boolean isMATL = false;

      if (header.equals("MATL")) {
        // Skip the MATL and BBOX to get to the MESH
        isMATL = true;

        // 4 - Block Length (not including these 2 fields)
        int matlLength = fm.readInt();
        FieldValidator.checkLength(matlLength, arcSize);

        fm.skip(matlLength);

        header = fm.readString(4);
      }
      if (header.equals("BBOX")) {
        // 4 - Block Length (not including these 2 fields)
        int bboxLength = fm.readInt();
        FieldValidator.checkLength(bboxLength, arcSize);

        fm.skip(bboxLength);

        header = fm.readString(4);
      }
      if (!header.equals("MESH")) {
        ErrorLogger.log("[Viewer_TOD_TOD2_MESH_MESH] Unknown mesh block: " + header);
      }

      int numParts = 1;
      int numFaces = 0;
      int numVertices = 0;

      if (isMATL) {
        // 4 - Block Length (not including these 2 fields)
        fm.skip(4);

        // 4 - Number of Face Indexes
        numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        // 4 - Number of Vertices
        numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Number of Parts (same as Number of Images?)
        numParts = fm.readInt();
        FieldValidator.checkNumFiles(numParts);

        // 4 - Number of Vertices
        fm.skip(4);
      }
      else {
        // 4 - Block Length (not including these 2 fields)
        // 16 - Mesh Name/Description (null terminated, filled with nulls)
        fm.skip(20);

        // 4 - Number of Images
        int numImages = fm.readInt();
        FieldValidator.checkNumFiles(numImages + 1); // +1 to allow for 0 images

        // for each image
        //   64 - Image Filename (null terminated, filled with nulls)
        fm.skip(numImages * 64);

        // 4 - Number of Parts (same as Number of Images?)
        numParts = fm.readInt();
        FieldValidator.checkNumFiles(numParts);

        // 4 - Number of Face Indexes
        numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        // 4 - Number of Vertices
        numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);
      }

      int[] faceStarts = new int[numParts];
      int[] faceCounts = new int[numParts];
      int[] vertexStarts = new int[numParts];
      int[] vertexCounts = new int[numParts];

      if (!isMATL) {
        for (int i = 0; i < numParts; i++) {
          // 4 - Part ID (incremental from 0)
          fm.skip(4);

          // 4 - ID of the first Face Index in this Part
          int faceStart = fm.readInt();
          FieldValidator.checkRange(faceStart, 0, numFaces);
          faceStarts[i] = faceStart;

          // 4 - Number of Face Indexes in this Part
          int faceCount = fm.readInt();
          FieldValidator.checkRange(faceCount, 0, numFaces);
          faceCounts[i] = faceCount;

          // 4 - ID of the first Vertex in this Part
          int vertexStart = fm.readInt();
          FieldValidator.checkRange(vertexStart, 0, numFaces);
          vertexStarts[i] = vertexStart;

          // 4 - Number of Vertices in this Part
          int vertexCount = fm.readInt();
          FieldValidator.checkRange(vertexCount, 0, numFaces);
          vertexCounts[i] = vertexCount;

          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          fm.skip(24);
        }
      }

      //
      // FACES
      //

      int faceDataSize = 2;

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

      //
      // VERTICES
      //

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

        // 4 - Normal X
        // 4 - Normal Y
        // 4 - Normal Z
        float xNormal = fm.readFloat();
        float yNormal = fm.readFloat();
        float zNormal = fm.readFloat();

        normals[j] = xNormal;
        normals[j + 1] = yNormal;
        normals[j + 2] = zNormal;

        // 4 - U Float
        // 4 - V Float
        fm.skip(8);

        // Skip the texture mapping for now
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
      // IGNORE THE ANIM AND KMSH SECTIONS
      //

      // need to adjust the subsequent parts (the first part is OK)...
      // need to change the face indexes so that each subsequent part has the right vertex numbers
      for (int p = 1; p < numParts; p++) {
        int faceStart = faceStarts[p] / 3; // as this value is a face *index* number, not a face number
        int faceCount = faceCounts[p];

        int vertexStart = vertexStarts[p];
        int vertexCount = vertexCounts[p];

        // change the starts/ends to suit the arrays (which have sizes of 3 or 6)
        faceStart *= 6;
        int faceEnd = faceStart + (faceCount * 2);

        for (int f = faceStart; f < faceEnd; f++) {
          faces[f] += vertexStart;
        }
      }

      // we have a full mesh for a single object (including all parts adjusted) - add it to the model
      if (faces != null && points != null && normals != null && texCoords != null) {
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