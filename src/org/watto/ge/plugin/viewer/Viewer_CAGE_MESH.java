/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_CAGE;
import org.watto.io.FileManipulator;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_CAGE_MESH extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_CAGE_MESH() {
    super("CAGE_MESH", "Darkest of Days MESH Viewer");
    setExtensions("mesh");

    setGames("Darkest of Days");
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
      if (plugin instanceof Plugin_CAGE) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() < 100) {
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
      MeshView[] meshView = null; // we're using MeshView, as we're setting textures on the mesh

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
      String texture = null;

      // 4 - null
      fm.skip(4);

      // 4 - Number of Parts
      int numParts = fm.readInt();
      FieldValidator.checkNumFiles(numParts);

      meshView = new MeshView[numParts];

      for (int p = 0; p < numParts; p++) {
        // X - Part Name
        // 1 - null Part Name Terminator
        fm.readNullString();

        //
        // VERTICES
        //
        System.out.println("Vertices at " + fm.getOffset());

        // 4 - Number of Vertices
        int numVertices = fm.readInt();
        FieldValidator.checkNumVertices(numVertices);

        // 2 - Unknown (1)
        fm.skip(2);

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat();
          float yPoint = fm.readFloat();
          float zPoint = fm.readFloat();

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          texCoords[k] = 0;
          texCoords[k + 1] = 0;

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
        // skip all the other vertex stuff
        //

        // for each vertex
        //   12 - Unknown
        fm.skip(numVertices * 12);

        System.out.println("Blocks at " + fm.getOffset());

        // 4 - Unknown (or null)
        //fm.skip(4);
        fm.skip(2);

        // 2 - Number of Blocks (1/2)
        int numBlocks = fm.readShort();
        if (numBlocks == 0) {
          numBlocks = fm.readShort();
        }
        FieldValidator.checkPositive(numBlocks);

        // for each block
        //   2 - Unknown (2)
        //   for each vertex
        //      8 - Unknown
        fm.skip(numBlocks * ((numVertices * 8) + 2));

        System.out.println("3-Count at " + fm.getOffset());

        // 2 - Unknown (3)
        int count = fm.readShort();

        // for each vertex
        //   12 - Unknown
        if (count != 0) {
          fm.skip(numVertices * 12);
        }

        System.out.println("3-Count at " + fm.getOffset());

        // 2 - Unknown (3)
        count = fm.readShort();

        // for each vertex
        //   12 - Unknown
        if (count != 0) {
          fm.skip(numVertices * 12);
        }

        System.out.println("4-Count at " + fm.getOffset());

        // 2 - Unknown (0/4)
        count = fm.readShort();

        if (count != 0) {
          // for each vertex
          //   32 - Unknown
          fm.skip(numVertices * 32);
        }

        // 2 - null
        fm.skip(2);

        //
        // FACES
        //
        System.out.println("Faces at " + fm.getOffset());

        // 4 - Number of Face Indices
        int numFaces = fm.readInt();
        FieldValidator.checkNumFaces(numFaces);

        int faceDataSize = 4;

        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        int numFaces6 = numFaces3 * 2;

        faces = new int[numFaces6]; // need to store front and back faces

        for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
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
          meshView[p] = view;

          rendered = true;
        }

        // Prepare for the next Group, so that the subsequent groups point to the right vertices
        // NO LONGER NEEDED, AS WE'RE USING SEPARATE MeshView FOR EACH GROUP
        //vertexStartIndex += numVertices;

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