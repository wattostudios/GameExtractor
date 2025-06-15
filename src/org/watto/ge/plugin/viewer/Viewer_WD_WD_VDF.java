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

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_3DModel;
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_WD_WD;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WD_WD_VDF extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WD_WD_VDF() {
    super("WD_WD_VDF", "WD_WD_VDF Model");
    setExtensions("vdf");
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
      if (plugin instanceof Plugin_WD_WD) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 5 - Header (159,153,102,246,2)
      byte[] headerBytes = fm.readBytes(5);
      if (ByteConverter.unsign(headerBytes[0]) == 159 && ByteConverter.unsign(headerBytes[1]) == 153 && headerBytes[2] == 102 && ByteConverter.unsign(headerBytes[3]) == 246 && headerBytes[4] == 2) {
        rating += 5;
      }

      // 4 - File Length [+5]
      if (fm.readInt() + 5 == fm.getLength()) {
        rating += 5;
      }

      // 4 - Unknown (-1)
      if (fm.readInt() == -1) {
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
      // Set up the mesh
      //TriangleMesh triangleMesh = new TriangleMesh();
      MeshView[] meshView = new MeshView[0]; // we're using MeshView, as we're setting textures on the mesh
      int numMeshes = 0;

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

      // Read in the model

      // 5 - Header (159,153,102,246,2)
      // 4 - File Length [+5]
      // 4 - Unknown (-1)
      fm.skip(13);

      long arcSize = fm.getLength();

      while (fm.getOffset() < arcSize) {
        // 1 - Entry Type (1=Entry, 2=Group)
        int entryType = fm.readByte();
        //System.out.println("Entry " + entryType + " at " + (fm.getOffset() - 1));

        // plain entry, just skip over it
        if (entryType == 1) {
          // 4 - Entry Length (including this field)
          int entryLength = fm.readInt() - 4;
          FieldValidator.checkLength(entryLength, arcSize);

          // 1 - Unknown
          // 4 - Field Name Length
          // X - Field Name
          // X - Extra Field Data
          fm.skip(entryLength);
        }
        else if (entryType == 2) {
          // a group, which should contain a mesh

          // 4 - Group Length (including this field)
          int groupLength = fm.readInt() - 8;
          FieldValidator.checkLength(groupLength, arcSize);

          // 4 - Group ID? (0=fields, 1=meshes)
          int groupID = fm.readInt();

          /*
          if (groupID != 1) {
            // not the main mesh group, so skip it
            fm.skip(groupLength);
            continue;
          }
          */

          // If we're here, we're in a mesh group that we want to read

          byte[] vertexData = null;
          byte[] faceData = null;
          int numVertices = 0;
          int numFaces = 0;
          int typeID = -1;

          long endOffset = fm.getOffset() + groupLength;
          while (fm.getOffset() < endOffset) {
            // 1 - Entry Flag (1)
            int entryFlag = fm.readByte();
            //System.out.println("   Entry " + entryFlag + " at " + (fm.getOffset() - 1));
            if (entryFlag == 2) {
              // a sub-group = just skip it (contains texture data or something)

              // 4 - Group Length (including this field)
              int subgroupLength = fm.readInt() - 4;
              FieldValidator.checkLength(subgroupLength, arcSize);

              fm.skip(subgroupLength);
              continue;
            }
            else if (entryFlag != 1) {
              // unexpected
              ErrorLogger.log("[Viewer_WD_WD_VDF] Unexpected entry within a group: " + entryFlag);
            }

            // 4 - Entry Length (including this field)
            int entryLength = fm.readInt() - 9;
            FieldValidator.checkLength(entryLength, arcSize);

            // 1 - Unknown
            fm.skip(1);

            // 4 - Field Name Length
            int nameLength = fm.readInt();
            FieldValidator.checkFilenameLength(nameLength);

            // X - Field Name
            String name = fm.readString(nameLength);
            //System.out.println("   " + name);

            entryLength -= nameLength;

            if (name.equals("Faces")) {
              faceData = fm.readBytes(entryLength);
            }
            else if (name.equals("Vertexes")) {
              vertexData = fm.readBytes(entryLength);
            }
            else if (name.equals("NumVertexes")) {
              // 4 - Number of Vertices
              numVertices = fm.readInt();
              FieldValidator.checkNumVertices(numVertices);
            }
            else if (name.equals("NumFaces")) {
              // 4 - Number of Face Indices
              numFaces = fm.readInt();
              FieldValidator.checkNumFaces(numFaces);
            }
            else if (name.equals("Type")) {
              // 4 - Type ID
              typeID = fm.readInt();
              //System.out.println("Type = " + typeID);
            }
            else {
              // X - Extra Field Data
              fm.skip(entryLength);
            }

          }

          // if we're here, we hopefully have faces and vertices to read and process
          if (numVertices != 0 && numFaces != 0 && vertexData != null && faceData != null) {
            // process the data

            int numVertices3 = numVertices * 3;
            points = new float[numVertices3];

            int numPoints2 = numVertices * 2;
            texCoords = new float[numPoints2];

            FileManipulator dataFM = new FileManipulator(new ByteBuffer(vertexData));

            for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
              // 4 - Point X (Float)
              // 4 - Point Y (Float)
              // 4 - Point Z (Float)
              float xPoint = dataFM.readFloat();
              float yPoint = dataFM.readFloat();
              float zPoint = dataFM.readFloat();

              points[j] = xPoint;
              points[j + 1] = yPoint;
              points[j + 2] = zPoint;

              // 4 - Unknown
              // 4 - Unknown
              dataFM.skip(8);

              // 4 - TexCoord U
              // 4 - TexCoord T
              float xTexture = dataFM.readFloat();
              float yTexture = dataFM.readFloat();
              texCoords[k] = xTexture;
              texCoords[k + 1] = yTexture;

              // 4 - Unknown
              // 4 - Unknown
              dataFM.skip(8);

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

            dataFM.close();

            dataFM = new FileManipulator(new ByteBuffer(faceData));

            int numFaces3 = numFaces;
            FieldValidator.checkNumFaces(numFaces3);

            numFaces = numFaces3 / 3;
            int numFaces6 = numFaces3 * 2;

            faces = new int[numFaces6]; // need to store front and back faces

            for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
              // 2 - Point Index 1
              // 2 - Point Index 2
              // 2 - Point Index 3
              int facePoint1 = (ShortConverter.unsign(dataFM.readShort()));
              int facePoint2 = (ShortConverter.unsign(dataFM.readShort()));
              int facePoint3 = (ShortConverter.unsign(dataFM.readShort()));

              // reverse face first (so the light shines properly, for this model specifically)
              faces[j] = facePoint3;
              faces[j + 1] = facePoint2;
              faces[j + 2] = facePoint1;

              // forward face second
              faces[j + 3] = facePoint1;
              faces[j + 4] = facePoint2;
              faces[j + 5] = facePoint3;

            }

            dataFM.close();

            // render the mesh
            // add the part to the model
            if (faces != null && points != null && texCoords != null) {
              //System.out.println(typeID);
              // we have a full mesh for a single object - add it to the model
              TriangleMesh triangleMesh = new TriangleMesh();
              triangleMesh.getTexCoords().addAll(texCoords);

              triangleMesh.getPoints().addAll(points);
              triangleMesh.getFaces().addAll(faces);

              // Enlarge the meshView array
              MeshView[] oldArray = meshView;
              meshView = new MeshView[numMeshes + 1];
              System.arraycopy(oldArray, 0, meshView, 0, numMeshes);

              // Create and add the new MeshView
              MeshView view = new MeshView(triangleMesh);
              meshView[numMeshes] = view;
              numMeshes++;

              faces = null;
              points = null;
              texCoords = null;

              rendered = true;
            }
          }

        }
      }

      if (rendered) {
        // calculate the sizes and centers
        float diffX = (maxX - minX);
        float diffY = (maxY - minY);
        float diffZ = (maxZ - minZ);

        float centerX = minX + (diffX / 2);
        float centerY = minY + (diffY / 2);
        float centerZ = minZ + (diffZ / 2);

        Point3D sizes = new Point3D(diffX, diffY, diffZ);
        Point3D center = new Point3D(centerX, centerY, centerZ);

        //PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh, sizes, center);
        // return the preview based on a MeshView
        PreviewPanel_3DModel preview = new PreviewPanel_3DModel(meshView, sizes, center);

        return preview;
      }

      return null;
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

}