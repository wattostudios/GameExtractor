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
import org.watto.Settings;
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
public class Viewer_STL_SOLID extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_STL_SOLID() {
    super("STL_SOLID", "STL (ASCII) Model");
    setExtensions("stl", "stla");

    setGames("STL (ASCII) Model");
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

      if (fm.readString(5).equals("solid")) {
        rating += 50;
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      TriangleMesh triangleMesh = new TriangleMesh();

      int maxFaces = Settings.getInt("MaxNumberOfFaces4");
      int maxVertices = Settings.getInt("MaxNumberOfVertices4");

      float[] vertices = new float[maxVertices];
      float[] normals = new float[maxVertices];
      float[] texCoords = new float[maxVertices];
      int[] faces = new int[maxFaces];

      int realNumVertices = 0;
      int realNumNormals = 0;
      int realNumTexCoords = 0;
      int realNumFaces = 0;

      float minX = 0f;
      float maxX = 0f;
      float minY = 0f;
      float maxY = 0f;
      float minZ = 0f;
      float maxZ = 0f;

      String spaceString = "\\s+";

      int nextVertex = 0;

      while (fm.getOffset() < arcSize) {
        String line = fm.readLine().trim(); // trip off the spaces either side, if any

        if (line == null || line.length() <= 6) {
          continue; // minimum for storing anything useful is 6 chars ("solid " or "facet ")
        }

        String[] split = line.split(spaceString);
        int splitLength = split.length;

        try {

          String firstWord = split[0];

          if (firstWord.equals("vertex")) {
            // a vertex

            // store the 3 points (x,y,z)
            if (splitLength < 4) {
              continue; // invalid number of points
            }

            float xPoint = Float.parseFloat(split[1]);
            float yPoint = Float.parseFloat(split[2]);
            float zPoint = Float.parseFloat(split[3]);

            vertices[realNumVertices++] = xPoint;
            vertices[realNumVertices++] = yPoint;
            vertices[realNumVertices++] = zPoint;

            // so we know what vertex we're up to, when building the faces
            nextVertex++;

            continue;
          }
          else if (firstWord.equals("facet")) {
            // face, with normal attached

            // we don't store face normals, so skip
            continue;
          }
          else if (firstWord.equals("outer")) {
            // start of a triangle of vertexes

            // nothing to do here
            continue;
          }
          else if (firstWord.equals("endloop")) {
            // end of a triangle of vertexes

            // nothing to do here
            continue;
          }
          else if (firstWord.equals("endfacet")) {
            // end of a face

            // need to store the face, which is built form the previous 3 vertices
            int face1 = nextVertex - 3;
            int face2 = nextVertex - 2;
            int face3 = nextVertex - 1;

            // reverse face
            faces[realNumFaces++] = face3;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face1;

            // front face
            faces[realNumFaces++] = face1;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face3;

            //System.out.println("Adding face " + face1 + "," + face2 + "," + face3);

            continue;
          }
          else if (firstWord.equals("solid")) {
            // start of a shape

            // nothing to do here
            continue;
          }
          else if (firstWord.equals("endsolid")) {
            // end of a shape

            // nothing to do here
            continue;
          }
          else {
            // unknown
            return null;
          }

        }
        catch (Throwable t) {
          // something went wrong, hopefully just a float parsing or something
          //System.out.println("Broken: " + line);
          continue;
        }
      }

      // shrink the arrays.
      // if we haven't found any normals or texCoords, create them now
      if (realNumVertices < maxVertices) {
        // shrink
        float[] oldVertices = vertices;
        vertices = new float[realNumVertices];
        System.arraycopy(oldVertices, 0, vertices, 0, realNumVertices);
      }
      if (realNumNormals < maxVertices) {
        if (realNumNormals == 0) {
          /*
          // create empty ones
          realNumNormals = realNumVertices; // must equal the number of vertex points
          normals = new float[realNumNormals];
          Arrays.fill(normals, 0);
          */
          // don't create them, just don't add anything to the mesh, let it work it out
        }
        else {
          // shrink
          float[] oldNormals = normals;
          normals = new float[realNumNormals];
          System.arraycopy(oldNormals, 0, normals, 0, realNumNormals);
        }
      }
      if (realNumTexCoords < maxVertices) {
        if (realNumTexCoords == 0) {
          // create empty ones
          realNumTexCoords = realNumVertices / 3 * 2; // must be 2 for each *vertex* (not each vertex point)
          texCoords = new float[realNumTexCoords];
          Arrays.fill(texCoords, 0);
        }
        else {
          // shrink
          float[] oldTexCoords = texCoords;
          texCoords = new float[realNumTexCoords];
          System.arraycopy(oldTexCoords, 0, texCoords, 0, realNumTexCoords);
        }
      }
      if (realNumFaces < maxFaces) {
        // shrink
        int[] oldFaces = faces;
        faces = new int[realNumFaces];
        System.arraycopy(oldFaces, 0, faces, 0, realNumFaces);
      }

      // Calculate the size of the object
      for (int i = 0; i < realNumVertices; i += 3) {
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

      if (faces != null && vertices != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(vertices);
        triangleMesh.getFaces().addAll(faces);

        if (realNumNormals > 0) {
          triangleMesh.getNormals().addAll(normals);
        }

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

    // header
    fm.writeLine("solid model");

    DecimalFormat df = new DecimalFormat("#");
    df.setMaximumFractionDigits(8);

    // Get the Vertices and Faces
    ObservableFloatArray verticesObservable = mesh.getPoints();
    int numVertices = verticesObservable.size();
    float[] vertices = new float[numVertices];
    verticesObservable.toArray(vertices);

    ObservableFaceArray facesObservable = mesh.getFaces();
    int numFaces = facesObservable.size();
    int[] faces = new int[numFaces];
    facesObservable.toArray(faces);

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

      fm.writeLine("facet normal 0.0 0.0 0.0");
      fm.writeLine("  outer loop");

      fm.writeLine("    vertex " + df.format(vertex1x) + ' ' + df.format(vertex1y) + ' ' + df.format(vertex1z));
      fm.writeLine("    vertex " + df.format(vertex2x) + ' ' + df.format(vertex2y) + ' ' + df.format(vertex2z));
      fm.writeLine("    vertex " + df.format(vertex3x) + ' ' + df.format(vertex3y) + ' ' + df.format(vertex3z));

      fm.writeLine("  endloop");
      fm.writeLine("endfacet");
    }

    faces = null; // free memory
    vertices = null; // free memory

    // footer
    fm.writeLine("endsolid model");

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