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
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_OBJ extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_OBJ() {
    super("OBJ", "OBJ Model");
    setExtensions("obj");

    setGames("OBJ Model");
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

      char spaceChar = ' ';

      char vertChar = 'v';
      char faceChar = 'f';
      char commentChar = '#';

      char normalChar2 = 'n';
      char textureChar2 = 't';
      char parameterSpaceChar2 = 'p';

      char slashChar = '/';

      String spaceString = " ";

      while (fm.getOffset() < arcSize) {
        String line = fm.readLine();

        if (line == null || line.length() < 4) {
          continue; // minimum for storing anything useful is 4 chars (eg "vt 0")
        }

        try {

          char checkChar = line.charAt(0);
          if (checkChar == commentChar) {
            continue;
          }
          else if (checkChar == vertChar) {
            // check if it's v, vp, vt, vn
            checkChar = line.charAt(1);
            if (checkChar == spaceChar) {
              // vertex (point)
              String[] split = line.split(spaceString);
              if (split.length < 4) {
                continue; // invalid
              }

              float xPoint = Float.parseFloat(split[1]);
              float yPoint = Float.parseFloat(split[2]);
              float zPoint = Float.parseFloat(split[3]);

              vertices[realNumVertices++] = xPoint;
              vertices[realNumVertices++] = yPoint;
              vertices[realNumVertices++] = zPoint;

              // ignore any other details on this line
              continue;
            }
            else if (checkChar == normalChar2) {
              // normal
              checkChar = line.charAt(2);

              if (checkChar != spaceChar) {
                continue; // invalid
              }

              String[] split = line.split(spaceString);
              if (split.length < 4) {
                continue; // invalid
              }

              float xPoint = Float.parseFloat(split[1]);
              float yPoint = Float.parseFloat(split[2]);
              float zPoint = Float.parseFloat(split[3]);

              normals[realNumNormals++] = xPoint;
              normals[realNumNormals++] = yPoint;
              normals[realNumNormals++] = zPoint;

              // ignore any other details on this line
              continue;
            }
            else if (checkChar == textureChar2) {
              // texture

              checkChar = line.charAt(2);

              if (checkChar != spaceChar) {
                continue; // invalid
              }

              String[] split = line.split(spaceString);
              int splitLength = split.length;
              if (splitLength < 2) {
                continue; // invalid
              }

              float uPoint = Float.parseFloat(split[1]);

              float vPoint = 0;
              if (splitLength >= 3) {
                vPoint = Float.parseFloat(split[2]);
              }

              /* // w is not implemented
              float wPoint = 0;
              if (splitLength >= 4) {
                wPoint = Float.parseFloat(split[2]);
              }
              */

              texCoords[realNumTexCoords++] = uPoint;
              texCoords[realNumTexCoords++] = vPoint;

              // ignore any other details on this line
              continue;
            }
            else if (checkChar == parameterSpaceChar2) {
              // parameter space
              continue; // NOT IMPLEMENTED
            }
            else {
              continue; // invalid
            }
          }
          else if (checkChar == faceChar) {
            // face
            checkChar = line.charAt(1);

            if (checkChar != spaceChar) {
              continue; // invalid
            }

            String[] split = line.split(spaceString);
            if (split.length < 4) {
              continue; // invalid
            }

            int face1 = 0;
            String currentSplit = split[1];
            int slashPos = currentSplit.indexOf(slashChar);
            if (slashPos > 0) {
              currentSplit = currentSplit.substring(0, slashPos);
            }
            face1 = Integer.parseInt(currentSplit);

            int face2 = 0;
            currentSplit = split[2];
            slashPos = currentSplit.indexOf(slashChar);
            if (slashPos > 0) {
              currentSplit = currentSplit.substring(0, slashPos);
            }
            face2 = Integer.parseInt(currentSplit);

            int face3 = 0;
            currentSplit = split[3];
            slashPos = currentSplit.indexOf(slashChar);
            if (slashPos > 0) {
              currentSplit = currentSplit.substring(0, slashPos);
            }
            face3 = Integer.parseInt(currentSplit);

            // Face indexes that we read in, start at 1. Need to convert to Java with 0 as the starting index
            face1--;
            face2--;
            face3--;

            // reverse face
            faces[realNumFaces++] = face3;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face1;

            // front face
            faces[realNumFaces++] = face1;
            faces[realNumFaces++] = face2;
            faces[realNumFaces++] = face3;

          }
          else {
            // something we don't support
            //System.out.println("Unsupported: " + checkChar);
            continue;
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
    MeshView[] meshView = null;

    if (panel instanceof PreviewPanel_3DModel) {
      PreviewPanel_3DModel panel3D = (PreviewPanel_3DModel) panel;
      mesh = panel3D.getModel();
      meshView = panel3D.getMeshView();
    }
    else if (panel instanceof PreviewPanel_MeshInvestigator) {
      PreviewPanel_MeshInvestigator panel3D = (PreviewPanel_MeshInvestigator) panel;
      mesh = panel3D.getModel();
    }
    else {
      return;
    }

    if (mesh == null && meshView == null) {
      return;
    }

    int numMeshes = 1;
    if (meshView != null) {
      numMeshes = meshView.length;
    }

    fm.writeLine("# Exported by Game Extractor http://www.watto.org/extract");

    DecimalFormat df = new DecimalFormat("#");
    df.setMaximumFractionDigits(8);

    int verticesProcessed = 0;
    for (int m = 0; m < numMeshes; m++) {
      if (meshView != null) {
        mesh = (TriangleMesh) meshView[m].getMesh();
      }

      fm.writeLine("o Object" + m); // allow multiple meshes in the same object

      // Vertices
      ObservableFloatArray verticesObservable = mesh.getPoints();
      int numVertices = verticesObservable.size();
      float[] vertices = new float[numVertices];
      verticesObservable.toArray(vertices);

      for (int i = 0; i < numVertices; i += 3) {
        fm.writeLine("v " + df.format(vertices[i]) + ' ' + df.format(vertices[i + 1]) + ' ' + df.format(vertices[i + 2]));
      }
      vertices = null; // free memory

      // Normals
      ObservableFloatArray normalsObservable = mesh.getNormals();
      int numNormals = normalsObservable.size();
      float[] normals = new float[numNormals];
      normalsObservable.toArray(normals);

      for (int i = 0; i < numNormals; i += 3) {
        float normal1 = normals[i];
        float normal2 = normals[i + 1];
        float normal3 = normals[i + 2];
        if (normal1 != 0 && normal2 != 0 && normal3 != 0) {
          fm.writeLine("vn " + df.format(normal1) + ' ' + df.format(normal2) + ' ' + df.format(normal3));
        }
      }
      normals = null; // free memory

      // Texture Co-ordinates
      ObservableFloatArray texCoordsObservable = mesh.getTexCoords();
      int numTexCoords = texCoordsObservable.size();
      float[] texCoords = new float[numTexCoords];
      texCoordsObservable.toArray(texCoords);

      for (int i = 0; i < numTexCoords; i += 2) {
        float texCoord1 = texCoords[i];
        float texCoord2 = texCoords[i + 1];
        if (texCoord1 != 0 && texCoord2 != 0) {
          fm.writeLine("vt " + df.format(texCoord1) + ' ' + df.format(texCoord2));
        }
      }
      texCoords = null; // free memory

      // Faces
      ObservableFaceArray facesObservable = mesh.getFaces();
      int numFaces = facesObservable.size();
      int[] faces = new int[numFaces];
      facesObservable.toArray(faces);

      for (int i = 0; i < numFaces; i += 3) {
        // Faces in Java start at 0. Need to convert to OBJ faces which start at 1
        int face1 = faces[i] + 1 + verticesProcessed;
        int face2 = faces[i + 1] + 1 + verticesProcessed;
        int face3 = faces[i + 2] + 1 + verticesProcessed;
        fm.writeLine("f " + face1 + ' ' + face2 + ' ' + face3);
      }
      faces = null; // free memory

      verticesProcessed += (numVertices / 3);
    }

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