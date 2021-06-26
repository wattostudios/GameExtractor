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
import java.text.DecimalFormat;
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.component.PreviewPanel_MeshInvestigator;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_STL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_STL() {
    super("STL", "STL (Binary) Model");
    setExtensions("stl");

    setGames("STL (Binary) Model");
    setPlatforms("PC");

    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_3DModel) {
      return true;
    }
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      fm.skip(80);

      // 4 - Number of Faces
      if (FieldValidator.checkNumFaces(fm.readInt())) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

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

      // 80 - Header
      fm.skip(80);

      // 4 - Number of Faces
      int numFaces = fm.readInt();
      FieldValidator.checkNumFaces(numFaces);

      int numVertices = numFaces * 3;

      int numFaces6 = numFaces * 6;
      int numVertices2 = numVertices * 2;
      int numVertices3 = numVertices * 3;

      float[] vertices = new float[numVertices3];
      float[] normals = new float[numVertices3];
      float[] texCoords = new float[numVertices2];
      int[] faces = new int[numFaces6];

      float minX = 0f;
      float maxX = 0f;
      float minY = 0f;
      float maxY = 0f;
      float minZ = 0f;
      float maxZ = 0f;

      int nextVertex = 0;
      for (int f = 0, v = 0; f < numFaces6; f += 6, v += 9) {
        // 4 - Face Normal X
        // 4 - Face Normal Y
        // 4 - Face Normal Z
        fm.skip(12);

        // 4 - Vertex 1 X
        // 4 - Vertex 1 Y
        // 4 - Vertex 1 Z
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        vertices[v] = xPoint;
        vertices[v + 1] = yPoint;
        vertices[v + 2] = zPoint;
        nextVertex++;

        // 4 - Vertex 2 X
        // 4 - Vertex 2 Y
        // 4 - Vertex 2 Z
        xPoint = fm.readFloat();
        yPoint = fm.readFloat();
        zPoint = fm.readFloat();

        vertices[v + 3] = xPoint;
        vertices[v + 4] = yPoint;
        vertices[v + 5] = zPoint;
        nextVertex++;

        // 4 - Vertex 3 X
        // 4 - Vertex 3 Y
        // 4 - Vertex 3 Z
        xPoint = fm.readFloat();
        yPoint = fm.readFloat();
        zPoint = fm.readFloat();

        vertices[v + 6] = xPoint;
        vertices[v + 7] = yPoint;
        vertices[v + 8] = zPoint;
        nextVertex++;

        // 2 - Attributes Byte Count
        short attributeLength = fm.readShort();
        FieldValidator.checkRange(attributeLength, 0, 255);
        fm.skip(attributeLength);

        // need to store the face, which is built form the previous 3 vertices
        int face1 = nextVertex - 3;
        int face2 = nextVertex - 2;
        int face3 = nextVertex - 1;

        // reverse face
        faces[f] = face3;
        faces[f + 1] = face2;
        faces[f + 2] = face1;

        // front face
        faces[f + 3] = face1;
        faces[f + 4] = face2;
        faces[f + 5] = face3;
      }

      // Calculate the size of the object
      for (int i = 0; i < numVertices3; i += 3) {
        float xPoint = vertices[i];
        float yPoint = vertices[i + 1];
        float zPoint = vertices[i + 2];

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

      // no texcoords - fill with empty
      Arrays.fill(texCoords, 0);

      if (faces != null && vertices != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(vertices);
        triangleMesh.getFaces().addAll(faces);

        // no normals
        //triangleMesh.getNormals().addAll(normals);

        faces = null;
        vertices = null;
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
  public void write(PreviewPanel panel, FileManipulator fm) {
    TriangleMesh mesh = null;

    if (panel instanceof PreviewPanel_3DModel) {
      PreviewPanel_3DModel panel3D = (PreviewPanel_3DModel) panel;
      mesh = panel3D.getModel();
    }
    else if (panel instanceof PreviewPanel_MeshInvestigator) {
      PreviewPanel_MeshInvestigator panel3D = (PreviewPanel_MeshInvestigator) panel;
      mesh = panel3D.getModel();
    }
    else {
      return;
    }

    if (mesh == null) {
      return;
    }

    // Get the Vertices and Faces
    ObservableFloatArray verticesObservable = mesh.getPoints();
    int numVertices = verticesObservable.size();
    float[] vertices = new float[numVertices];
    verticesObservable.toArray(vertices);

    ObservableFaceArray facesObservable = mesh.getFaces();
    int numFaces = facesObservable.size();
    int[] faces = new int[numFaces];
    facesObservable.toArray(faces);

    // 80 - Header
    fm.writeString("Exported by Game Extractor http://www.watto.org/extract                         ");

    // 4 - Number of Faces
    fm.writeInt(numFaces);

    DecimalFormat df = new DecimalFormat("#");
    df.setMaximumFractionDigits(8);

    for (int i = 0; i < numFaces; i += 3) {
      int face1 = faces[i] * 3; // *3 to convert it into a position in the vertices[]
      int face2 = faces[i + 1] * 3; // *3 to convert it into a position in the vertices[]
      int face3 = faces[i + 2] * 3; // *3 to convert it into a position in the vertices[]

      float vertex1x = vertices[face1];
      float vertex1y = vertices[face1 + 1];
      float vertex1z = vertices[face1 + 2];

      float vertex2x = vertices[face2];
      float vertex2y = vertices[face2 + 1];
      float vertex2z = vertices[face2 + 2];

      float vertex3x = vertices[face3];
      float vertex3y = vertices[face3 + 1];
      float vertex3z = vertices[face3 + 2];

      // 4 - Face Normal X
      // 4 - Face Normal Y
      // 4 - Face Normal Z
      fm.writeFloat(0);
      fm.writeFloat(0);
      fm.writeFloat(0);

      // 4 - Vertex 1 X
      // 4 - Vertex 1 Y
      // 4 - Vertex 1 Z
      fm.writeFloat(vertex1x);
      fm.writeFloat(vertex1y);
      fm.writeFloat(vertex1z);

      // 4 - Vertex 2 X
      // 4 - Vertex 2 Y
      // 4 - Vertex 2 Z
      fm.writeFloat(vertex2x);
      fm.writeFloat(vertex2y);
      fm.writeFloat(vertex2z);

      // 4 - Vertex 3 X
      // 4 - Vertex 3 Y
      // 4 - Vertex 3 Z
      fm.writeFloat(vertex3x);
      fm.writeFloat(vertex3y);
      fm.writeFloat(vertex3z);

      // 2 - Attributes Byte Count
      fm.writeShort(0);

    }

    faces = null; // free memory
    vertices = null; // free memory

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

      return null;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

}