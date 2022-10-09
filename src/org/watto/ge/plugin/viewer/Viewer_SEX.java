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
import java.util.Arrays;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_SEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_SEX() {
    super("SEX", "SEX Model");
    setExtensions("sex");

    setGames("Urban Chaos");
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      String header = fm.readString(1);
      if (header.equals("#") || header.equals("v")) {
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

      long arcSize = fm.getLength();

      // Read in the model

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = new MeshView[100]; // max 100 individual mesh parts (guess)
      int numMeshes = 0;

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

      String meshString = "Triangle mesh";
      String vertexString = "Vertex";
      String faceString = "Face";
      String comma = ",";
      String bracket = "[\\)]";

      while (fm.getOffset() < arcSize) {
        String line = fm.readLine();

        if (line == null || line.length() < 4) {
          continue; // minimum for storing anything useful is 4 chars
        }

        try {

          if (line.startsWith(meshString)) {
            // starting a new mesh

            // close the old mesh first

            if (realNumFaces > 0) {

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

              // we have a full mesh for a single object (including all parts adjusted) - add it to the model
              if (faces != null && vertices != null && normals != null && texCoords != null) {
                // Create the Mesh
                TriangleMesh triangleMesh = new TriangleMesh();
                triangleMesh.getTexCoords().addAll(texCoords);

                triangleMesh.getPoints().addAll(vertices);
                triangleMesh.getFaces().addAll(faces);
                //triangleMesh.getNormals().addAll(normals);

                // Create the MeshView
                MeshView view = new MeshView(triangleMesh);
                meshView[numMeshes] = view;
                numMeshes++;

                vertices = new float[maxVertices];
                normals = new float[maxVertices];
                texCoords = new float[maxVertices];
                faces = new int[maxFaces];

                realNumVertices = 0;
                realNumNormals = 0;
                realNumTexCoords = 0;
                realNumFaces = 0;
              }

            }

          }
          else if (line.startsWith(vertexString)) {
            // a vertex
            // Vertex: (  -30.7283,   -8.2309,   92.1789)

            line = line.substring(9, line.length() - 1);
            String[] split = line.split(comma);

            float xPoint = Float.parseFloat(split[0].trim());
            float yPoint = Float.parseFloat(split[1].trim());
            float zPoint = Float.parseFloat(split[2].trim());

            vertices[realNumVertices++] = xPoint;
            vertices[realNumVertices++] = yPoint;
            vertices[realNumVertices++] = zPoint;

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
          else if (line.startsWith(faceString)) {
            // a face
            // Face: Material  0 xyz (    9,   11,   10) uv (    9,   11,   10) edge (0, 1, 1) group 1

            line = line.substring(23);
            String[] mainSplit = line.split(bracket);

            String[] split = mainSplit[0].split(comma);
            int face1 = Integer.parseInt(split[0].trim());
            int face2 = Integer.parseInt(split[1].trim());
            int face3 = Integer.parseInt(split[2].trim());

            // front face
            faces[realNumFaces++] = face1;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face3;

            // reverse face
            faces[realNumFaces++] = face3;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face1;

          }
          else {
            // don't care - skip it
            continue;
          }

        }
        catch (Throwable t) {
          // something went wrong, hopefully just a float parsing or something
          //System.out.println("Broken: " + line);
          continue;
        }
      }

      // we have a full mesh for a single object (including all parts adjusted) - add it to the model
      if (faces != null && vertices != null && normals != null && texCoords != null) {

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

        // Create the Mesh
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(vertices);
        triangleMesh.getFaces().addAll(faces);
        //triangleMesh.getNormals().addAll(normals);

        // Create the MeshView
        MeshView view = new MeshView(triangleMesh);
        meshView[numMeshes] = view;
        numMeshes++;

        vertices = new float[maxVertices];
        normals = new float[maxVertices];
        texCoords = new float[maxVertices];
        faces = new int[maxFaces];

        realNumVertices = 0;
        realNumNormals = 0;
        realNumTexCoords = 0;
        realNumFaces = 0;
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

      // resize the MeshView[]
      MeshView[] oldView = meshView;
      meshView = new MeshView[numMeshes];
      System.arraycopy(oldView, 0, meshView, 0, numMeshes);

      // return the preview based on a MeshView
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
  public void write(PreviewPanel panel, FileManipulator fm) {

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
      int thumbnailSize = 150;  // bigger than ImageResource, so it is shrunk (and smoothed as a result)
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