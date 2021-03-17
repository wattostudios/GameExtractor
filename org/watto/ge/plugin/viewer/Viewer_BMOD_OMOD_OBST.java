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
import org.watto.datatype.Archive;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_BMOD_OMOD;
import org.watto.io.FileManipulator;
import javafx.scene.shape.TriangleMesh;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BMOD_OMOD_OBST extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BMOD_OMOD_OBST() {
    super("BMOD_OMOD_OBST", "BMOD_OMOD_OBST 3D Model");
    setExtensions("obst", "dumy");

    setGames("Platoon");
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
      if (plugin instanceof Plugin_BMOD_OMOD) {
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

      // 4 - Header
      if (fm.readString(4).equals("OMOD")) {
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

      float[] points = null;
      float[] normals = null;
      float[] texCoords = null;
      int[] faces = null;

      float scale = 0.05f;//0000000;

      float xTranslate = 0;
      float yTranslate = 0;
      float zTranslate = 0;

      // for drawing multiple meshes into the same object, need to increment the number of points
      // each time so that the faces connect to the correct points
      int baseNumPoints = 0;

      while (fm.getOffset() < arcSize) {
        // 4 - Block Name
        String blockType = fm.readString(4);

        // 4 - Block Length (including these 2 header fields)
        int blockLength = fm.readInt() - 8; // -8 to skip the 2 header fields

        //System.out.println("Block " + blockType + " at " + (fm.getOffset() - 8));

        FieldValidator.checkLength(blockLength, arcSize);

        if (blockType.equals("DUMY")) {
          // Dummy, can contain objects in it

          // 4 - Object Number (incremental from 0)
          fm.skip(4);

          // 4 - Name Length
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          // 68 - Unknown
          fm.skip(nameLength + 68);
        }
        else if (blockType.equals("OBST")) {
          // Object

          // 4 - Object Number (incremental from 0)
          fm.skip(4);

          // 4 - Name Length
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          //String name = fm.readString(nameLength);
          //System.out.println(name);
          fm.skip(nameLength);

          // 68 - Unknown
          fm.skip(68);
        }
        else if (blockType.equals("MESH")) {
          // Mesh

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Number of Vertex Points
          fm.skip(12);
        }
        else if (blockType.equals("VERT")) {
          // Vertex block, contained within a MESH
          // 4 - Number of Vertex Points
          int numPoints = fm.readInt();
          FieldValidator.checkPositive(numPoints);

          // 4 - Unknown
          // 4 - null
          fm.skip(8);

          int vertexSize = (blockLength - 20) / numPoints;
          //System.out.println(vertexSize);
          if (vertexSize != 32 && vertexSize != 36) {
            // something else
            ErrorLogger.log("[Viewer_BMOD_OMOD] Unknown vertex size: " + vertexSize);
            fm.skip(blockLength - 12);
            continue; // read the next block
          }

          int numPoints3 = numPoints * 3;
          points = new float[numPoints3];
          normals = new float[numPoints3];

          int numPoints2 = numPoints * 2;
          texCoords = new float[numPoints2];

          for (int i = 0, j = 0, k = 0; i < numPoints; i++, j += 3, k += 2) {
            // 4 - Vertex X
            // 4 - Vertex Y
            // 4 - Vertex Z
            float xPoint = fm.readFloat() / scale;
            float yPoint = fm.readFloat() / scale;
            float zPoint = fm.readFloat() / scale;

            points[j] = xPoint;
            points[j + 1] = yPoint;
            points[j + 2] = zPoint;

            // 4 - Normal X
            // 4 - Normal Y
            // 4 - Normal Z
            float xNormal = fm.readFloat() / scale;
            float yNormal = fm.readFloat() / scale;
            float zNormal = fm.readFloat() / scale;

            normals[j] = xNormal;
            normals[j + 1] = yNormal;
            normals[j + 2] = zNormal;

            // 4 - Texture Coordinate X
            // 4 - Texture Coordinate Y
            float xTexture = fm.readFloat();
            float yTexture = fm.readFloat();

            // We need to blow away the tex coords, we aren't using textures yet
            xTexture = 0;
            yTexture = 0;

            texCoords[k] = xTexture;
            texCoords[k + 1] = yTexture;

            if (vertexSize == 36) {
              // 4 - Unknown
              fm.skip(4);
            }
          }

          // 4 - Unknown (1)
          // 4 - Unknown
          fm.skip(8);
        }
        else if (blockType.equals("ISTR")) {
          // Face Indexs block, contained within a MESH

          // 4 - Number of Face Indexes
          int numFaces3 = fm.readInt();
          FieldValidator.checkPositive(numFaces3);

          int numFaces = numFaces3 / 3;
          int numFaces6 = numFaces3 * 2;
          faces = new int[numFaces6]; // need to store front and back faces

          for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
            // 2 - Face Point 1
            // 2 - Face Point 2
            // 2 - Face Point 3
            short facePoint1 = (short) (fm.readShort() + baseNumPoints);
            short facePoint2 = (short) (fm.readShort() + baseNumPoints);
            short facePoint3 = (short) (fm.readShort() + baseNumPoints);

            // reverse face first (so the light shines properly, for this model specifically)
            faces[j] = facePoint3;
            faces[j + 1] = facePoint2;
            faces[j + 2] = facePoint1;

            // forward face second
            faces[j + 3] = facePoint1;
            faces[j + 4] = facePoint2;
            faces[j + 5] = facePoint3;

          }
        }
        else if (blockType.equals("FACE")) {
          // Faces block, contained within a MESH

          // 4 - Number of Faces
          fm.skip(4);

          // for each Face
          // LODP entry
        }
        else if (blockType.equals("LODP")) {
          // Unknown block, contained within a FACE

          // 4 - Unknown
          // 4 - Number of FSEC Entries? (1)
          fm.skip(8);

          // for each FSEC entry
          // FSEC entry
        }
        else if (blockType.equals("FSEC")) {
          // Face Sequence? block, contained within a LODP
          // 2 - null
          // 4 - Unknown (0)
          // 4 - Unknown
          // 4 - Number of Face Indexes
          // 4 - Unknown (101)
          // 4 - First Face Index Number
          // 4 - Last Face Index Number
          fm.skip(26);
        }
        else if (blockType.equals("OBOX")) {
          // Unknown block

          // the OBOX moves the object to the right place in the overall model?

          //

          // 4 - Vertex X
          // 4 - Vertex Y
          // 4 - Vertex Z
          float xPoint = fm.readFloat() / scale;
          float yPoint = fm.readFloat() / scale;
          float zPoint = fm.readFloat() / scale;

          // 4 - Normal X
          // 4 - Normal Y
          // 4 - Normal Z
          float xNormal = fm.readFloat() / scale;
          float yNormal = fm.readFloat() / scale;
          float zNormal = fm.readFloat() / scale;

          // adjust each of the vertex points to the offset
          if (points != null) {
            int numPoints3 = points.length;
            for (int j = 0; j < numPoints3; j += 3) {
              points[j] += xTranslate;
              points[j + 1] += yTranslate;
              points[j + 2] += zTranslate;
            }
          }

          // adjust each of the normal points to the offset
          if (normals != null) {
            int numNormals3 = normals.length;
            for (int j = 0; j < numNormals3; j += 3) {
              normals[j] += xNormal;
              normals[j + 1] += yNormal;
              normals[j + 2] += zNormal;
            }
          }

          xTranslate = (xPoint + xNormal);
          yTranslate = (yPoint + yNormal);
          zTranslate = (zPoint + zNormal);

          if (faces != null && points != null && normals != null && texCoords != null) {
            // we have a full mesh for a single object - add it to the model
            triangleMesh.getTexCoords().addAll(texCoords);

            triangleMesh.getPoints().addAll(points);
            triangleMesh.getFaces().addAll(faces);
            triangleMesh.getNormals().addAll(normals);

            baseNumPoints += points.length / 3; // ready for the next mesh to be added

            faces = null;
            points = null;
            normals = null;
            texCoords = null;

          }

        }
        else if (blockType.equals("MBOX")) {
          // Unknown block

          // 24 - Unknown
          fm.skip(24);
        }
        else if (blockType.equals("TEXT")) {
          // Unknown block

          // 4 - null
          fm.skip(4);

          // 4 - String Length
          int stringLength = fm.readInt();
          FieldValidator.checkFilenameLength(stringLength);

          // X - String
          fm.skip(stringLength);
        }
        else if (blockType.equals("MATE")) {
          // Material block

          // 4 - Material Number? (0/1)
          fm.skip(4);

          // 4 - Texture Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Texture Filename
          fm.skip(filenameLength);

          // 4 - Number of Entries
          int numEntries = fm.readInt();
          FieldValidator.checkPositive(numEntries);

          // 96 - Unknown
          fm.skip(96);

          // for each entry
          // 20 - Unknown
          fm.skip(numEntries * 20);
        }
        else if (blockType.equals("BLST")) {
          // Unknown block

          // 4 - null
          fm.skip(4);
        }
        else if (blockType.equals("ASEL")) {
          // Unknown block

          // 4 - Number of Strings
          int numStrings = fm.readInt();
          FieldValidator.checkPositive(numStrings);

          for (int i = 0; i < numStrings; i++) {
            // 4 - String ID
            // 4 - Unknown
            fm.skip(8);

            // 4 - String Length
            int stringLength = fm.readInt();
            FieldValidator.checkFilenameLength(stringLength);

            // X - String
            fm.skip(stringLength);
          }
        }
        else if (blockType.equals("ASEC")) {
          // Unknown block

          // 4 - null
          fm.skip(4);
        }
        else if (blockType.equals("TIME")) {
          // Timestamp Block

          // 24 - Timestamp String
          fm.skip(24);
        }
        else {
          ErrorLogger.log("[Viewer_BMOD_OMOD] Unknown block type: " + blockType);
          fm.skip(blockLength);
        }
      }

      if (faces != null && points != null && normals != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        triangleMesh.getNormals().addAll(normals);

        faces = null;
        points = null;
        normals = null;
        texCoords = null;
      }

      PreviewPanel_3DModel preview = new PreviewPanel_3DModel(triangleMesh);
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

}