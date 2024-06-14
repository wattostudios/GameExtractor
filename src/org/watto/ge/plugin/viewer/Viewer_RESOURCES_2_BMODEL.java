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
import org.watto.ge.plugin.archive.Plugin_RESOURCES_2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.FloatConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_RESOURCES_2_BMODEL extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_RESOURCES_2_BMODEL() {
    super("RESOURCES_2_BMODEL", "RAGE BMODEL Mesh Viewer");
    setExtensions("bmodel");

    setGames("RAGE");
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
      if (plugin instanceof Plugin_RESOURCES_2) {
        rating += 50;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      String header = fm.readString(4);
      if (header.equals((char) 24 + "LMB")) {
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

      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
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

      // 4 - Header ((byte)24 + "LMB")
      // 4 - Unknown
      fm.skip(8);

      // 4 - Number of Meshes
      int numMeshes = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkRange(numMeshes, 1, 50); // guess

      //System.out.println(numMeshes);

      meshView = new MeshView[numMeshes];

      String[] meshNames = new String[numMeshes];

      for (int m = 0; m < numMeshes; m++) {
        // 4 - Mesh Name Length (LITTLE ENDIAN)
        int nameLength = fm.readInt();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Mesh Name
        meshNames[m] = fm.readString(nameLength);

        // 4 - Unknown (1/-1)
        fm.skip(4);

        // 4 - Number of Vertices
        int numVertices = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumVertices(numVertices);

        // 4 - Number of Face Indices
        int numFaces = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumVertices(numFaces);

        // 4 - Mesh Format (543=TriangleStrip, 31=NormalTriangles)
        int meshFormat = IntConverter.changeFormat(fm.readInt());

        boolean triangleStrip = true;
        if (meshFormat == 31) {
          triangleStrip = false;
        }
        //System.out.println(meshFormat);

        // 4 - Unknown Float (1.0)
        // 4 - Unknown Float (1.0)
        // 4 - Unknown Float (1.0)
        // 12 - null
        // 4 - Unknown Float (1.0)
        // 4 - Unknown Float (1.0)
        // 8 - null
        fm.skip(40);

        //
        // VERTICES
        //

        int numVertices3 = numVertices * 3;
        points = new float[numVertices3];

        int numPoints2 = numVertices * 2;
        texCoords = new float[numPoints2];

        int skipSize = 20;
        if (meshFormat == 631) {
          skipSize = 8;
          return null; // don't support this mesh, not sure what it is
        }

        for (int v = 0, j = 0, k = 0; v < numVertices; v++, j += 3, k += 2) {

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = FloatConverter.changeFormat(fm.readFloat());
          float yPoint = FloatConverter.changeFormat(fm.readFloat());
          float zPoint = FloatConverter.changeFormat(fm.readFloat());

          points[j] = xPoint;
          points[j + 1] = yPoint;
          points[j + 2] = zPoint;

          texCoords[k] = 0;
          texCoords[k + 1] = 0;

          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - Unknown Float
          fm.skip(skipSize);

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
        // FACES
        //

        int numFaces3 = numFaces;
        FieldValidator.checkNumFaces(numFaces3);

        numFaces = numFaces3 / 3;
        //int numFaces6 = numFaces3 * 2;

        int maxFaces = numFaces3 * 6;
        faces = new int[maxFaces]; // need to store front and back faces

        int numTriangles = 0;

        if (!triangleStrip) {
          // read as 3-index triangles
          for (int f = 0, j = 0; f < numFaces; f++, j += 6) {
            // 2 - Point Index 1
            // 2 - Point Index 2
            // 2 - Point Index 3
            int facePoint1 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));
            int facePoint2 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));
            int facePoint3 = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));

            // forward face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint1;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint3;

            // reverse face second
            faces[j + 3] = facePoint3;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint1;

            numTriangles++;
          }
        }
        else {
          // read as a triangle strip

          int[] faceIndexes = new int[numFaces3];
          for (int f = 0; f < numFaces3; f++) {
            faceIndexes[f] = (ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())));
          }

          int faceIndex1 = faceIndexes[0];
          int faceIndex2 = faceIndexes[1];

          int indexReadPos = 2;
          int faceWritePos = 0;

          boolean swapPoints = false;
          while (indexReadPos < numFaces3) {
            int nextIndex = faceIndexes[indexReadPos];
            indexReadPos++;

            if (nextIndex == 65535) {
              // end of the current block, start the next one
              faceIndex1 = faceIndexes[indexReadPos];
              indexReadPos++;
              faceIndex2 = faceIndexes[indexReadPos];
              indexReadPos++;
              nextIndex = faceIndexes[indexReadPos];
              indexReadPos++;

              swapPoints = false;
            }

            if (swapPoints) {
              // reverse face first (so the light shines properly, for this model specifically)
              faces[faceWritePos] = nextIndex;
              faces[faceWritePos + 1] = faceIndex2;
              faces[faceWritePos + 2] = faceIndex1;

              // forward face second
              faces[faceWritePos + 3] = faceIndex1;
              faces[faceWritePos + 4] = faceIndex2;
              faces[faceWritePos + 5] = nextIndex;

            }
            else {
              // forward face first (so the light shines properly, for this model specifically)
              faces[faceWritePos] = faceIndex1;
              faces[faceWritePos + 1] = faceIndex2;
              faces[faceWritePos + 2] = nextIndex;

              // reverse face second
              faces[faceWritePos + 3] = nextIndex;
              faces[faceWritePos + 4] = faceIndex2;
              faces[faceWritePos + 5] = faceIndex1;

            }

            faceWritePos += 6;

            faceIndex1 = faceIndex2;
            faceIndex2 = nextIndex;

            swapPoints = !swapPoints;

            numTriangles++;
          }
        }

        // resize the arrays
        numTriangles *= 6;

        if (numTriangles < maxFaces) {
          int[] oldFaces = faces;
          faces = new int[numTriangles];
          System.arraycopy(oldFaces, 0, faces, 0, numTriangles);
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
          meshView[m] = view;

          rendered = true;
        }

        // Skip the mesh Footer
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - null
        // 4 - Header ((byte)24 + "LMB")
        fm.skip(32);

      }

      if (!rendered) {
        // didn't find any meshes
        return null;
      }

      // don't want to render the Collision mesh - only the proper mesh
      int realNumMeshes = 0;
      MeshView[] realMeshes = new MeshView[numMeshes];
      for (int m = 0; m < numMeshes; m++) {
        if (meshNames[m].startsWith("textures")) {
          // ignore it
        }
        else {
          realMeshes[realNumMeshes] = meshView[m];
          realNumMeshes++;
        }
      }

      if (realNumMeshes < numMeshes) {
        meshView = new MeshView[realNumMeshes];
        System.arraycopy(realMeshes, 0, meshView, 0, realNumMeshes);
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