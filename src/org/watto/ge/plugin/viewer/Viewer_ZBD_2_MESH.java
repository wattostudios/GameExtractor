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
import org.watto.SingletonManager;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ZBD_2;
import org.watto.io.FileManipulator;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ZBD_2_MESH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ZBD_2_MESH() {
    super("ZBD_2_MESH", "Crimson Skies MESH Model");
    setExtensions("mesh");

    setGames("Crimson Skies");
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
      if (readPlugin instanceof Plugin_ZBD_2) {
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {

    try {

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

      int polygonCount = 0;
      int vertexCount = 0;
      int normalCount = 0;
      int morphCount = 0;
      int lightCount = 0;

      // get the width/height from the properties of the image resource, which were read by the ArchivePlugin
      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject == null || !(resourceObject instanceof Resource)) {
        return null;
      }
      Resource resource = (Resource) resourceObject;

      try {
        polygonCount = Integer.parseInt(resource.getProperty("PolygonCount"));
        vertexCount = Integer.parseInt(resource.getProperty("VertexCount"));
        normalCount = Integer.parseInt(resource.getProperty("NormalCount"));
        morphCount = Integer.parseInt(resource.getProperty("MorphCount"));
        lightCount = Integer.parseInt(resource.getProperty("LightCount"));
      }
      catch (Throwable t) {
        //
      }

      if (polygonCount <= 0) {
        return null; // can only read meshes with polygons in it
      }

      // Skip the 104-byte header
      fm.skip(104);

      //
      //
      // VERTICES
      //
      //
      int numVertices3 = vertexCount * 3;
      points = new float[numVertices3];

      int numPoints2 = vertexCount * 2;
      texCoords = new float[numPoints2];

      for (int i = 0, j = 0, k = 0; i < vertexCount; i++, j += 3, k += 2) {
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
      // NORMALS
      //
      //
      fm.skip(normalCount * 12);

      //
      //
      // MORPHS
      //
      //
      fm.skip(morphCount * 12);

      //
      //
      // LIGHTS
      //
      //
      int totalExtraCount = 0;
      for (int i = 0; i < lightCount; i++) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);

        // 4 - Extra Count
        int extraCount = fm.readInt();
        totalExtraCount += extraCount;

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Pointer
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(60);
      }

      fm.skip(totalExtraCount * 12);

      //
      //
      // POLYGONS
      //
      //
      int triangleCount = 0;
      int[] pointCounts = new int[polygonCount];
      boolean[] hasNormals = new boolean[polygonCount];
      for (int i = 0; i < polygonCount; i++) {
        // 1 - Number of Vertices in this Polygon
        int numPoints = fm.readByte();
        FieldValidator.checkRange(numPoints, 3, 127);
        pointCounts[i] += numPoints;

        triangleCount += numPoints - 2;

        // 3 - Unknown
        // 4 - Unknown
        // 4 - Vertex Pointer
        fm.skip(11);

        // 4 - Normal Pointer
        hasNormals[i] = (fm.readInt() != 0);

        // 4 - Unknown (1)
        // 4 - Tex Co-Ord Pointer
        // 4 - Colors Pointer
        // 4 - Unknown Pointer
        // 4 - Material Index
        // 4 - Material Info
        fm.skip(24);

      }

      int numFaces3 = triangleCount * 3;
      FieldValidator.checkNumFaces(numFaces3);

      //triangleCount = numFaces3 / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      int j = 0;
      for (int i = 0; i < polygonCount; i++) {
        int numPoints = pointCounts[i];
        int numTriangles = numPoints - 2;

        //System.out.println(numTriangles);

        // 4 - Vertex Index 1
        // 4 - Vertex Index 2
        // 4 - Vertex Index 3
        int facePoint1 = fm.readInt();
        int facePoint2 = fm.readInt();
        int facePoint3 = fm.readInt();

        // reverse face first (so the light shines properly, for this model specifically)
        faces[j] = facePoint3;
        faces[j + 1] = facePoint2;
        faces[j + 2] = facePoint1;

        // forward face second
        faces[j + 3] = facePoint1;
        faces[j + 4] = facePoint2;
        faces[j + 5] = facePoint3;

        j += 6;

        // read the other triangles in this polygon

        for (int t = 1; t < numTriangles; t++) {
          // keep point 1
          // point 2 is the last point read
          // point 3 is the next point we read
          facePoint2 = facePoint3;
          facePoint3 = fm.readInt();

          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;

          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;

          j += 6;
        }

        /*
        // As a triangle strip
        boolean swap = true;
        for (int t = 1; t < numTriangles; t++) {
          // point 1 = point 2
          // point 2 = point 3
          // point 3 is the next point we read
          facePoint1 = facePoint2;
          facePoint2 = facePoint3;
          facePoint3 = fm.readInt();
        
          if (swap) {
            int facePointTemp = facePoint1;
            facePoint1 = facePoint2;
            facePoint2 = facePointTemp;
          }
        
          swap = !swap;
        
          // reverse face first (so the light shines properly, for this model specifically)
          faces[j] = facePoint3;
          faces[j + 1] = facePoint2;
          faces[j + 2] = facePoint1;
        
          // forward face second
          faces[j + 3] = facePoint1;
          faces[j + 4] = facePoint2;
          faces[j + 5] = facePoint3;
        
          j += 6;
        }
        */

        // 4 - Unknown (0/6/8)
        fm.skip(4);

        // Skip over the other stuff in this polygon so we can get to the next one
        // for each vertex in this Polygon
        //   4 - Normal Index
        if (hasNormals[i]) {
          fm.skip(numPoints * 4);
        }

        // for each vertex in this Polygon
        //  4 - Tex Co-Ord U (Float)
        //  4 - Tex Co-Ord V (Float)
        fm.skip(numPoints * 8);

        // for each vertex in this Polygon
        //  4 - Red (Float)
        //  4 - Green (Float)
        //  4 - Blue (Float)
        fm.skip(numPoints * 12);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        //fm.skip(12);

      }

      // build the model
      if (faces != null && points != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        //triangleMesh.getNormals().addAll(normals);

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