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

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_3DS_MM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_3DS_MM() {
    super("3DS_MM", "Autodesk 3DS Mesh Viewer");
    setExtensions("3ds");

    setGames("HROT");
    setPlatforms("PC");
    setStandardFileFormat(true);
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
      else {
        return 0;
      }

      String header = fm.readString(2);
      if (header.equals("MM")) {
        rating += 50;
      }

      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
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
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = new MeshView[1]; // we're using MeshView, as we're setting textures on the mesh

      float[] points = null;
      float[] texCoords = null;
      int[] faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      boolean rendered = false;

      String[] textureFiles = new String[0];

      while (fm.getOffset() < arcSize) {
        // 2 - Chunk Type
        short chunkType = fm.readShort();

        // 4 - Chunk Length (including these header fields, and all sub-chunks)
        int chunkLength = fm.readInt() - 6;
        FieldValidator.checkLength(chunkLength, arcSize);

        // X - Chunk Data
        if (chunkType == 19789) {
          // 4D4D - Main Chunk
        }
        else if (chunkType == 2) {
          // 0200 - Version

          // X - Data
          fm.skip(chunkLength);
        }
        else if (chunkType == 15677) {
          // 3D3D - Mesh Holder (contains a Material and an Object)
        }
        else if (chunkType == -20481) {
          // FFAF - Material

          // X - Data
          fm.skip(chunkLength);
        }
        else if (chunkType == 16384) {
          // 0040 - Object

          // X - Object Name
          // 1 - null Object Name Terminator
          fm.readNullString();
        }
        else if (chunkType == 16640) {
          // 0041 - Triangle Mesh
        }
        else if (chunkType == 16656) {
          // 1041 - Vertex List

          // 2 - Number of Vertices
          int numVertices = ShortConverter.unsign(fm.readShort());
          FieldValidator.checkNumVertices(numVertices);

          int numVertices3 = numVertices * 3;
          points = new float[numVertices3];

          int numPoints2 = numVertices * 2;
          texCoords = new float[numPoints2];

          for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

            //   4 - X Vertex (float)
            //   4 - Y Vertex (float)
            //   4 - Z Vertex (float)
            float xPoint = fm.readFloat();
            float yPoint = fm.readFloat();
            float zPoint = fm.readFloat();

            points[j] = xPoint;
            points[j + 1] = yPoint;
            points[j + 2] = zPoint;

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

        }
        else if (chunkType == 16672) {
          // 2041 - Faces List

          // 2 - Number of Faces
          int numFaces = ShortConverter.unsign(fm.readShort());
          FieldValidator.checkNumFaces(numFaces);

          int numFaces3 = numFaces * 3;
          int numFaces6 = numFaces3 * 2;

          faces = new int[numFaces6]; // need to store front and back faces

          for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
            //   2 - Point Index 1
            //   2 - Point Index 2
            //   2 - Point Index 3
            int facePoint1 = (ShortConverter.unsign(fm.readShort()));
            int facePoint2 = (ShortConverter.unsign(fm.readShort()));
            int facePoint3 = (ShortConverter.unsign(fm.readShort()));

            //          2 - Unknown
            fm.skip(2);

            // forward face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint3;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint1;

            // reverse face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint3;

          }

        }
        else {
          // X - Data
          fm.skip(chunkLength);
        }
      }

      //
      // RENDER THE MESH
      //

      // we have a full mesh for a single object (including all parts adjusted) - add it to the model
      if (faces != null && points != null && texCoords != null) {
        // Create the Mesh
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);

        faces = null;
        points = null;
        texCoords = null;

        // Create the MeshView
        MeshView view = new MeshView(triangleMesh);
        meshView[0] = view;

        /*
        // set the texture
        if (image != null) {
          Material material = new PhongMaterial(Color.WHITE, image, (Image) null, (Image) null, (Image) null);
          view.setMaterial(material);
        }
        */

        rendered = true;
      }

      if (!rendered) {
        // didn't find any meshes
        return null;
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

      java.awt.Image image = preview3D.getImage();
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