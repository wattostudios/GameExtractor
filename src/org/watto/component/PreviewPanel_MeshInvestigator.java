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

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.datatype.ImageResource;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSEnterableInterface;
import org.watto.event.listener.WSClickableListener;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.Transform3D;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.xml.XMLReader;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class PreviewPanel_MeshInvestigator extends PreviewPanel_Image implements WSClickableInterface, WSEnterableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  File file = null;

  WSTextField vertexOffsetField;

  WSTextField vertexCountField;

  WSTextField vertexBlockSizeField;

  WSTextField faceOffsetField;

  WSTextField faceCountField;

  WSTextField faceBlockSizeField;

  WSButton regenerateMeshButton;

  WSButton showStatsLogButton;

  WSTextField uvOffsetField;

  WSButton calculateUVButton;

  /**
  **********************************************************************************************
  DO NOT USE - Only to generate a dummy panel for finding writable ViewerPlugins for this type
  **********************************************************************************************
  **/
  public PreviewPanel_MeshInvestigator() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_MeshInvestigator(File file) {
    super();

    createInterface();

    loadFile(file);

    /*
    ImageIcon icon = new ImageIcon(image);
    
    WSScrollPane scrollPane = new WSScrollPane(XMLReader.read("<WSScrollPane showBorder=\"true\" showInnerBorder=\"true\" opaque=\"false\"><WSPanel><WSLabel code=\"PreviewPanel_Image_ImageLabel\" opaque=\"true\" /></WSPanel></WSScrollPane>"));
    WSLabel imageLabel = (WSLabel) ComponentRepository.get("PreviewPanel_Image_ImageLabel");
    imageLabel.setIcon(icon);
    
    add(scrollPane, BorderLayout.CENTER);
    */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void loadFile(File file) {
    this.file = file;
    regenerateMesh();
  }

  Scene scene = null;

  JFXPanel jfxPanel = null;

  WSPanel mainPanel = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void createInterface() {

    // 3.16 Added "codes" to every XML-built object, so that they're cleaned up when the object is destroyed (otherwise it was being retained in the ComponentRepository)

    jfxPanel = new JFXPanel(); // Scrollable JCompenent

    mainPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" border-width=\"4\" vertical-gap=\"4\" code=\"PreviewPanel_3DModel_MainPanel\"></WSPanel>"));
    mainPanel.add(jfxPanel, BorderLayout.CENTER);

    //WSScrollPane scrollPane = new WSScrollPane(XMLReader.read("<WSScrollPane showBorder=\"true\" showInnerBorder=\"true\" opaque=\"false\"><WSPanel obeyBackgroundColor=\"true\" code=\"PreviewPanel_MeshInvestigator_Background\"><WSLabel code=\"PreviewPanel_MeshInvestigator_ImageLabel\" opaque=\"true\" /></WSPanel></WSScrollPane>"));
    add(mainPanel, BorderLayout.CENTER);

    // Text Fields
    vertexOffsetField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_VertexOffsetField\" showLabel=\"true\" opaque=\"false\" />"));
    vertexCountField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_VertexCountField\" showLabel=\"true\" opaque=\"false\" />"));
    vertexBlockSizeField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_VertexBlockSizeField\" showLabel=\"true\" opaque=\"false\" />"));

    faceOffsetField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_FaceOffsetField\" showLabel=\"true\" opaque=\"false\" />"));
    faceCountField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_FaceCountField\" showLabel=\"true\" opaque=\"false\" />"));
    faceBlockSizeField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_FaceBlockSizeField\" showLabel=\"true\" opaque=\"false\" />"));

    uvOffsetField = new WSTextField(XMLReader.read("<WSTextField code=\"PreviewPanel_MeshInvestigator_UVOffsetField\" opaque=\"false\" />"));

    // Buttons
    calculateUVButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_MeshInvestigator_CalculateUVButton\" />"));

    regenerateMeshButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_MeshInvestigator_RegenerateMeshButton\" />"));
    showStatsLogButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_MeshInvestigator_ShowStatsLogButton\" />"));

    // Set the initial values of the fields
    String offsetValue = Settings.getString("PreviewPanel_MeshInvestigator_VertexOffsetField");
    if (offsetValue == null || offsetValue.length() <= 0) {
      offsetValue = "0";
    }
    vertexOffsetField.setText(offsetValue);

    offsetValue = Settings.getString("PreviewPanel_MeshInvestigator_FaceOffsetField");
    if (offsetValue == null || offsetValue.length() <= 0) {
      offsetValue = "0";
    }
    faceOffsetField.setText(offsetValue);

    String countValue = Settings.getString("PreviewPanel_MeshInvestigator_VertexCountField");
    if (countValue == null || countValue.length() <= 0) {
      countValue = "0";
    }
    vertexCountField.setText(countValue);

    countValue = Settings.getString("PreviewPanel_MeshInvestigator_FaceCountField");
    if (countValue == null || countValue.length() <= 0) {
      countValue = "0";
    }
    faceCountField.setText(countValue);

    String blockValue = Settings.getString("PreviewPanel_MeshInvestigator_VertexBlockSizeField");
    if (blockValue == null || blockValue.length() <= 0) {
      blockValue = "24";
    }
    vertexBlockSizeField.setText(blockValue);

    blockValue = Settings.getString("PreviewPanel_MeshInvestigator_FaceBlockSizeField");
    if (blockValue == null || blockValue.length() <= 0) {
      blockValue = "2";
    }
    faceBlockSizeField.setText(blockValue);

    String uvValue = Settings.getString("PreviewPanel_MeshInvestigator_UVOffsetField");
    if (uvValue == null || uvValue.length() <= 0) {
      uvValue = "12";
    }
    uvOffsetField.setText(uvValue);

    // add the fields to the groups
    WSPanel vertexPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"1\" columns=\"3\" showBorder=\"true\" showLabel=\"true\" code=\"PreviewPanel_MeshInvestigator_VertexPanelLabel\" />"));
    vertexPanel.add(vertexOffsetField);
    vertexPanel.add(vertexCountField);
    vertexPanel.add(vertexBlockSizeField);

    WSPanel facePanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"1\" columns=\"3\" showBorder=\"true\" showLabel=\"true\" code=\"PreviewPanel_MeshInvestigator_FacePanelLabel\" />"));
    facePanel.add(faceOffsetField);
    facePanel.add(faceCountField);
    facePanel.add(faceBlockSizeField);

    WSPanel vertexFacePanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_3DModel_VertexFacePanelWrapper\" layout=\"GridLayout\" rows=\"2\" columns=\"1\" vertical-gap=\"4\" />"));
    vertexFacePanel.add(vertexPanel);
    vertexFacePanel.add(facePanel);

    WSPanel uvPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" showLabel=\"true\" code=\"PreviewPanel_MeshInvestigator_UVPanelLabel\" />"));
    uvPanel.add(uvOffsetField);
    uvPanel.add(calculateUVButton, BorderLayout.EAST);

    WSPanel vertexFaceUVPanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_3DModel_FaceUVPanelWrapper\" vertical-gap=\"4\" />"));
    vertexFaceUVPanel.add(vertexFacePanel);
    vertexFaceUVPanel.add(uvPanel, BorderLayout.SOUTH);

    WSPanel buttonPanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_3DModel_ButtonPanelWrapper\" vertical-gap=\"4\" />"));
    buttonPanel.add(regenerateMeshButton, BorderLayout.CENTER);
    buttonPanel.add(showStatsLogButton, BorderLayout.EAST);

    WSPanel bottomPanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_3DModel_BottomPanelWrapper\" vertical-gap=\"4\" />"));
    bottomPanel.add(vertexFaceUVPanel, BorderLayout.CENTER);
    bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(bottomPanel, BorderLayout.SOUTH);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String generateStatsLog() {
    try {
      String log = "";

      // Faces
      int numFaces = Integer.parseInt(faceCountField.getText());
      int faceOffset = Integer.parseInt(faceOffsetField.getText());
      int faceBlockSize = Integer.parseInt(faceBlockSizeField.getText());

      int faceEndOffset = faceOffset + (numFaces * faceBlockSize);

      log += "---------------------------------------------\n";
      log += "                    FACES\n";
      log += "---------------------------------------------\n";
      log += "\n";

      int minFace = 999999999;
      int maxFace = 0;
      int numFaces6 = numFaces / 3 * 6;
      for (int i = 0, j = 0; i < numFaces6; i += 6, j++) { // +6 to skip the reverse face
        int index1 = faces[i];
        int index2 = faces[i + 1];
        int index3 = faces[i + 2];

        if (index1 < minFace) {
          minFace = index1;
        }
        if (index2 < minFace) {
          minFace = index2;
        }
        if (index3 < minFace) {
          minFace = index3;
        }

        if (index1 > maxFace) {
          maxFace = index1;
        }
        if (index2 > maxFace) {
          maxFace = index2;
        }
        if (index3 > maxFace) {
          maxFace = index3;
        }

        log += j + "\t( " + index1 + ",\t" + index2 + ",\t" + index3 + " )\n";
      }

      int numVerticesCalculated = (maxFace - minFace);
      if (minFace == 0) {
        numVerticesCalculated++;
      }

      int numTriangles = numFaces / 3;

      log += "\n";
      log += "Number of Face Indices:\t" + numFaces + "\n";
      log += "Number of Triangles:\t" + numTriangles + "\n";
      log += "\n";
      log += "Faces Start Offset:\t" + faceOffset + "\n";
      log += "Faces End Offset:\t" + faceEndOffset + "\n";
      log += "Face Block Size:\t" + faceBlockSize + "\n";
      log += "\n";

      // Vertices
      int numVertices = Integer.parseInt(vertexCountField.getText());
      int vertexOffset = Integer.parseInt(vertexOffsetField.getText());
      int vertexBlockSize = Integer.parseInt(vertexBlockSizeField.getText());

      int vertexEndOffset = vertexOffset + (numVertices * vertexBlockSize);

      log += "------------------------------------------------\n";
      log += "                    VERTICES\n";
      log += "------------------------------------------------\n";
      log += "\n";

      int numVertices3 = numVertices * 3;
      for (int i = 0, j = 0; i < numVertices3; i += 3, j++) {
        float point1 = points[i];
        float point2 = points[i + 1];
        float point3 = points[i + 2];

        log += j + "\t( " + point1 + ",\t" + point2 + ",\t" + point3 + " )\n";
      }

      log += "\n";
      log += "Smallest Vertex Index:\t" + minFace + "\n";
      log += "Largest Vertex Index:\t" + maxFace + "\n";
      log += "Number of Vertices (entered):\t" + numVertices + "\n";
      log += "Number of Vertices (calculated):\t" + numVerticesCalculated + "\n";
      log += "\n";
      log += "Vertex Start Offset:\t" + vertexOffset + "\n";
      log += "Vertex End Offset:\t" + vertexEndOffset + "\n";
      log += "Vertex Block Size:\t" + vertexBlockSize + "\n";
      log += "\n";
      log += "Mesh Size:\t( " + sizes.getX() + ",\t" + sizes.getY() + ",\t" + sizes.getZ() + " )\n";
      log += "Center Point:\t( " + center.getX() + ",\t" + center.getY() + ",\t" + center.getZ() + " )\n";
      log += "\n";

      log += "------------------------------------------------------------\n";
      log += "                    TEXTURE CO-ORDINATES\n";
      log += "------------------------------------------------------------\n";
      log += "\n";

      int numUV = texCoords.length;
      for (int i = 0, j = 0; i < numUV; i += 2, j++) {
        float point1 = texCoords[i];
        float point2 = texCoords[i + 1];

        log += j + "\t( " + point1 + ",\t" + point2 + " )\n";
      }

      return log;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return "";
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image generateUVImage() {
    try {

      /*
      float minWidth = 9999999f;
      float maxWidth = 0f;
      
      float minHeight = 9999999f;
      float maxHeight = 0f;
      
      // calculate the size of the image
      int numCoords = texCoords.length;
      for (int i = 0; i < numCoords; i += 2) {
        float uPoint = texCoords[i];
        float vPoint = texCoords[i + 1];
      
        if (uPoint < minWidth) {
          minWidth = uPoint;
        }
        if (uPoint > maxWidth) {
          maxWidth = uPoint;
        }
      
        if (vPoint < minHeight) {
          minHeight = vPoint;
        }
        if (vPoint > maxHeight) {
          maxHeight = vPoint;
        }
      }
      
      int width = (int) (maxWidth - minWidth);
      int height = (int) (maxHeight - minHeight);
      */

      int width = 400;
      int height = 400;

      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      int black = 255 << 24;

      Arrays.fill(pixels, black);

      int white = Integer.MAX_VALUE;

      int numCoords = texCoords.length;
      for (int i = 0; i < numCoords; i += 2) {
        float uPoint = texCoords[i];
        float vPoint = texCoords[i + 1];

        uPoint *= width;
        vPoint *= height;

        int uPointInt = (int) uPoint;
        int vPointInt = (int) vPoint;

        for (int v = vPointInt; v < vPointInt + 4; v++) {
          for (int u = uPointInt; u < uPointInt + 4; u++) {
            int position = v * width + u;
            pixels[position] = white;
          }
        }

      }

      return new ImageResource(pixels, width, height).getImage();

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
  public void showMeshStatsPopup() {

    WSPanel mainPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" border-width=\"4\" minimum-width=\"775\" maximum-width=\"775\" minimum-height=\"600\" maximum-height=\"600\" height=\"600\"></WSPanel>"));

    WSScrollPane scrollPane = new WSScrollPane(XMLReader.read("<WSScrollPane showBorder=\"true\" showInnerBorder=\"true\" opaque=\"false\"><WSTextArea code=\"PreviewPanel_MeshInvestigator_StatsLog\" /></WSScrollPane>"));
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    WSButton closeButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_MeshInvestigator_CloseStatsLogButton\" />"));
    closeButton.addMouseListener(new WSClickableListener(this));

    mainPanel.add(closeButton, BorderLayout.SOUTH);

    WSTextArea statsLogArea = (WSTextArea) ComponentRepository.get("PreviewPanel_MeshInvestigator_StatsLog");
    String statsLog = generateStatsLog();
    statsLogArea.setText(statsLog);

    Font monoFont = new Font("monospaced", Font.PLAIN, 12);
    statsLogArea.setFont(monoFont);

    statsLogArea.setCaretPosition(0);

    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      // Remove the existing panel on the overlay  
      overlayPanel.removeAll();

      // Add the Wizard Panel
      overlayPanel.add(mainPanel);

      // set the background color around the panel
      overlayPanel.setObeyBackgroundColor(true);
      overlayPanel.setBackground(new java.awt.Color(255, 255, 255, 160));

      // Validate and show the display
      overlayPanel.validate();
      overlayPanel.setVisible(true);
      overlayPanel.repaint();
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void showUVTexturePopup() {

    WSPanel mainPanel = new WSPanel(XMLReader.read("<WSPanel code=\"PreviewPanel_3DModel_PopupMainPanelWrapper\" showBorder=\"true\" border-width=\"4\" minimum-width=\"775\" maximum-width=\"775\" minimum-height=\"600\" maximum-height=\"600\" height=\"600\"></WSPanel>"));

    WSScrollPane scrollPane = new WSScrollPane(XMLReader.read("<WSScrollPane code=\"PreviewPanel_3DModel_PopupScrollPaneWrapper\" showBorder=\"true\" showInnerBorder=\"true\" opaque=\"false\"><WSLabel code=\"PreviewPanel_MeshInvestigator_UVTextureImage\" /></WSScrollPane>"));
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    WSButton closeButton = new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_MeshInvestigator_CloseUVButton\" />"));
    closeButton.addMouseListener(new WSClickableListener(this));

    mainPanel.add(closeButton, BorderLayout.SOUTH);

    WSLabel uvLabel = (WSLabel) ComponentRepository.get("PreviewPanel_MeshInvestigator_UVTextureImage");
    Image uvImage = generateUVImage();
    uvLabel.setIcon(new ImageIcon(uvImage));

    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      // Remove the existing panel on the overlay  
      overlayPanel.removeAll();

      // Add the Wizard Panel
      overlayPanel.add(mainPanel);

      // set the background color around the panel
      overlayPanel.setObeyBackgroundColor(true);
      overlayPanel.setBackground(new java.awt.Color(255, 255, 255, 160));

      // Validate and show the display
      overlayPanel.validate();
      overlayPanel.setVisible(true);
      overlayPanel.repaint();
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent source, java.awt.event.MouseEvent event) {
    if (source instanceof WSComponent) {
      String code = ((WSComponent) source).getCode();
      if (code.equals("PreviewPanel_MeshInvestigator_RegenerateMeshButton")) {
        regenerateMesh();
        return true;
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_ShowStatsLogButton")) {
        regenerateMesh();
        showMeshStatsPopup();
        return true;
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_CalculateUVButton")) {
        regenerateUVMesh();
        //showUVTexturePopup();
        return true;
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_CloseStatsLogButton") || code.equals("PreviewPanel_MeshInvestigator_CloseUVButton")) {
        WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
        if (overlayPanel != null) {
          overlayPanel.removeAll();
          overlayPanel.setVisible(false);

          overlayPanel.validate();
          overlayPanel.repaint();
        }
      }
    }

    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection
    faces = null;
    points = null;
    //normals = null;
    texCoords = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onEnter(JComponent source, KeyEvent event) {

    if (source instanceof WSComponent) {
      String code = ((WSComponent) source).getCode();
      if (code.equals("PreviewPanel_MeshInvestigator_VertexOffsetField")) {
        regenerateMesh();
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_FaceOffsetField")) {
        regenerateMesh();
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_VertexCountField")) {
        regenerateMesh();
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_FaceCountField")) {
        regenerateMesh();
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_VertexBlockSizeField")) {
        regenerateMesh();
      }
      else if (code.equals("PreviewPanel_MeshInvestigator_FaceBlockSizeField")) {
        regenerateMesh();
      }
      else {
        return false;
      }
      return true;
    }

    return false;
  }

  float[] points = null;

  //float[] normals = null;
  float[] texCoords = null;

  int[] faces = null;

  float minX = 20000f;

  float maxX = -20000f;

  float minY = 20000f;

  float maxY = -20000f;

  float minZ = 20000f;

  float maxZ = -20000f;

  public static int MESH_VERTEX_ONLY = 0;

  public static int MESH_UV_ONLY = 1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildMeshFromFile() {
    buildMeshFromFile(MESH_VERTEX_ONLY);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void buildMeshFromFile(int meshFormat) {
    try {

      if (file == null || !file.exists()) {
        return;
      }

      long arcSize = file.length();

      // Set up the mesh
      triangleMesh = new TriangleMesh();

      points = null;
      //normals = null;
      texCoords = null;
      faces = null;

      minX = 20000f;
      maxX = -20000f;
      minY = 20000f;
      maxY = -20000f;
      minZ = 20000f;
      maxZ = -20000f;

      FileManipulator fm = new FileManipulator(file, false);

      //
      // read faces
      //

      int numFaces = Integer.parseInt(faceCountField.getText());
      FieldValidator.checkNumFaces(numFaces);

      int faceOffset = Integer.parseInt(faceOffsetField.getText());
      FieldValidator.checkOffset(faceOffset, arcSize);

      int faceBlockSize = Integer.parseInt(faceBlockSizeField.getText());
      if (faceBlockSize != 2 && faceBlockSize != 4) {
        return;
      }

      fm.seek(faceOffset);

      int numFaces3 = numFaces;
      numFaces = numFaces / 3;
      int numFaces6 = numFaces3 * 2;

      faces = new int[numFaces6]; // need to store front and back faces

      if (faceBlockSize == 2) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 2 - Point Index 1
          // 2 - Point Index 2
          // 2 - Point Index 3
          int facePoint1 = (ShortConverter.unsign(fm.readShort()));
          int facePoint2 = (ShortConverter.unsign(fm.readShort()));
          int facePoint3 = (ShortConverter.unsign(fm.readShort()));

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
      else if (faceBlockSize == 4) {
        for (int i = 0, j = 0; i < numFaces; i++, j += 6) {
          // 4 - Point Index 1
          // 4 - Point Index 2
          // 4 - Point Index 3
          int facePoint1 = (int) (IntConverter.unsign(fm.readInt()));
          int facePoint2 = (int) (IntConverter.unsign(fm.readInt()));
          int facePoint3 = (int) (IntConverter.unsign(fm.readInt()));

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

      //
      // read vertices
      //

      int numVertices = Integer.parseInt(vertexCountField.getText());
      FieldValidator.checkNumVertices(numVertices);

      int vertexOffset = Integer.parseInt(vertexOffsetField.getText());
      FieldValidator.checkOffset(vertexOffset, arcSize);

      int vertexBlockSize = Integer.parseInt(vertexBlockSizeField.getText());
      FieldValidator.checkRange(vertexBlockSize, 12, 256);

      int uvOffset = 0;
      try {
        uvOffset = Integer.parseInt(uvOffsetField.getText());
        if (uvOffset < 12 || uvOffset + 8 > vertexBlockSize) {
          uvOffset = 0; // out of bounds
        }
        //uvOffset -= 12;
      }
      catch (Throwable t) {
      }

      fm.seek(vertexOffset);

      int numVertices3 = numVertices * 3;
      points = new float[numVertices3];
      //normals = new float[numVertices3];

      int numPoints2 = numVertices * 2;
      texCoords = new float[numPoints2];

      int additionalSizeBeforeUV = uvOffset;
      if (uvOffset != 0) {
        additionalSizeBeforeUV -= 12;
      }

      int additionalSizeAfterUV = vertexBlockSize - additionalSizeBeforeUV - 12;
      if (uvOffset != 0) {
        additionalSizeAfterUV -= 8;
      }

      for (int i = 0, j = 0, k = 0; i < numVertices; i++, j += 3, k += 2) {
        // 4 - Vertex X
        // 4 - Vertex Y
        // 4 - Vertex Z
        float xPoint = fm.readFloat();
        float yPoint = fm.readFloat();
        float zPoint = fm.readFloat();

        points[j] = xPoint;
        points[j + 1] = yPoint;
        points[j + 2] = zPoint;

        fm.skip(additionalSizeBeforeUV);

        // Dummy
        float xTexture = 0;
        float yTexture = 0;

        if (uvOffset != 0) {
          // get actual UV values
          xTexture = fm.readFloat();
          yTexture = fm.readFloat();
        }

        texCoords[k] = xTexture;
        texCoords[k + 1] = yTexture;

        fm.skip(additionalSizeAfterUV);

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

      fm.close();

      if (meshFormat == MESH_UV_ONLY) {
        // Render the UV image, not the actual mesh

        minX = 20000f;
        maxX = -20000f;
        minY = 20000f;
        maxY = -20000f;

        // move the UV co-ords into the points
        int numPoints = points.length;
        for (int p = 0, u = 0; p < numPoints; p += 3, u += 2) {
          float point1 = texCoords[u];
          float point2 = texCoords[u + 1];

          points[p] = point1;
          points[p + 1] = point2;
          points[p + 2] = 0;

          // Calculate the size of the object
          if (point1 < minX) {
            minX = point1;
          }
          if (point1 > maxX) {
            maxX = point1;
          }

          if (point2 < minY) {
            minY = point2;
          }
          if (point2 > maxY) {
            maxY = point2;
          }

        }

        // set an appropriate size for the centering
        minZ = 0;
        maxZ = 0;

      }

      if (faces != null && points != null && texCoords != null) {
        // we have a full mesh for a single object - add it to the model
        triangleMesh.getTexCoords().addAll(texCoords);

        triangleMesh.getPoints().addAll(points);
        triangleMesh.getFaces().addAll(faces);
        //triangleMesh.getNormals().addAll(normals);

      }

      // calculate the sizes and centers
      float diffX = (maxX - minX);
      float diffY = (maxY - minY);
      float diffZ = (maxZ - minZ);

      float centerX = minX + (diffX / 2);
      float centerY = minY + (diffY / 2);
      float centerZ = minZ + (diffZ / 2);

      sizes = new Point3D(diffX, diffY, diffZ);
      center = new Point3D(centerX, centerY, centerZ);

      int[] smoothingGroups = new int[numFaces];
      Arrays.fill(smoothingGroups, 0); // means no smoothing
      triangleMesh.getFaceSmoothingGroups().setAll(smoothingGroups);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void regenerateUVMesh() {

    Settings.set("PreviewPanel_MeshInvestigator_VertexOffsetField", vertexOffsetField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_VertexCountField", vertexCountField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_VertexBlockSizeField", vertexBlockSizeField.getText());

    Settings.set("PreviewPanel_MeshInvestigator_FaceOffsetField", faceOffsetField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_FaceCountField", faceCountField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_FaceBlockSizeField", faceBlockSizeField.getText());

    Settings.set("PreviewPanel_MeshInvestigator_UVOffsetField", uvOffsetField.getText());

    buildMeshFromFile(MESH_UV_ONLY);

    if (file == null || !file.exists()) {
      return;
    }

    Platform.setImplicitExit(false); // stop the JavaFX from dying when the thread finishes
    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      // RESET THINGS
      root = new Group();
      world = new Transform3D();

      camera = new PerspectiveCamera(true);
      cameraXform = new Transform3D();
      cameraXform2 = new Transform3D();
      cameraXform3 = new Transform3D();

      // START BUILDING
      root.getChildren().add(world);
      root.setDepthTest(DepthTest.ENABLE);

      //root.setCacheHint(CacheHint.QUALITY);

      // buildScene();
      buildUVCamera();

      if (triangleMesh != null) {
        // Build it from a TriangleMesh
        MeshView view = new MeshView(triangleMesh);
        view.setDrawMode(DrawMode.FILL);
        view.setCullFace(CullFace.NONE);

        world.getChildren().add(view);
      }
      else if (meshView != null) {
        // Build it from a MeshView

        int numViews = meshView.length;
        for (int v = 0; v < numViews; v++) {
          // show textures
          meshView[v].setDrawMode(DrawMode.FILL);
          meshView[v].setCullFace(CullFace.NONE);

          world.getChildren().add(meshView[v]);
        }

      }

      int width = 0;//jfxPanel.getWidth(); // 1024
      int height = 0;//jfxPanel.getHeight(); // 768

      try {
        WSSidePanelHolder sidePanel = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");

        //if (width == 0) {
        width = sidePanel.getWidth() - 50;
        //}
        //if (height == 0) {
        height = sidePanel.getHeight() - 150;
        //}
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

      if (width == 0) {
        width = previousWidth;
      }
      if (height == 0) {
        height = previousHeight;
      }

      previousWidth = width;
      previousHeight = height;

      if (width == 0) {
        width = 300;
      }

      double scale = 1d;
      if (sizes != null) {
        if (center != null && height != 0) {
          // center vertically

          // Because we've rotated the object by 90 degrees, we need to translate "Y" based on the center "Z"
          //double moveZ = 0 - center.getZ();
          //camera.setTranslateY(moveZ);

          //double moveX = center.getX() - 0.5;
          //camera.setTranslateX(moveX);

          // TEST START
          double moveY = 0 - center.getY();
          camera.setTranslateY(moveY);

          double moveZ = 0 - center.getZ();
          camera.setTranslateZ(moveZ);
          // TEST END

          cameraXform.rx.setPivotX(center.getX());
          cameraXform.rx.setPivotY(center.getY());
          cameraXform.rx.setPivotZ(center.getZ());

          cameraXform.ry.setPivotX(center.getX());
          cameraXform.ry.setPivotY(center.getY());
          cameraXform.ry.setPivotZ(center.getZ());

          cameraXform.rz.setPivotX(center.getX());
          cameraXform.rz.setPivotY(center.getY());
          cameraXform.rz.setPivotZ(center.getZ());

        }

        double modelSize = sizes.getX();

        double diffY = sizes.getY();
        if (diffY > modelSize) {
          modelSize = diffY;
        }

        double diffZ = sizes.getZ();
        if (diffZ > modelSize) {
          modelSize = diffZ;
        }

        //if (width != 0) {

        //scale = ((modelSize / (Math.tan(width)))) * 30;// * 1.5;// * 10;
        //scale = ((modelSize / (Math.tan(90)))) * 5;
        scale = ((modelSize / (Math.tan(180)))) * (1 / ((float) width) * 2000); // slightly less than 2500 for normal

        //System.out.println("NORMAL: \t" + width + "\t" + height + "\t" + modelSize + "\t" + scale);

        if (scale > 0) {
          scale = 0 - scale;
        }
        camera.setTranslateZ(scale);
        //}

      }

      scene = new Scene(root, width, height, true, SceneAntialiasing.BALANCED);

      // Set the background color to whatever was last chosen
      String backgroundColor = Settings.getString("PreviewPanel_3DModel_BackgroundColor");
      if (backgroundColor.equals("BLACK")) {
        scene.setFill(Color.BLACK);
      }
      else if (backgroundColor.equals("WHITE")) {
        scene.setFill(Color.WHITE);
      }
      else {
        scene.setFill(Color.GREY);
      }

      handleMouse(scene, world);

      scene.setCamera(camera);
      jfxPanel.setScene(scene);

    });

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void regenerateMesh() {

    Settings.set("PreviewPanel_MeshInvestigator_VertexOffsetField", vertexOffsetField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_VertexCountField", vertexCountField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_VertexBlockSizeField", vertexBlockSizeField.getText());

    Settings.set("PreviewPanel_MeshInvestigator_FaceOffsetField", faceOffsetField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_FaceCountField", faceCountField.getText());
    Settings.set("PreviewPanel_MeshInvestigator_FaceBlockSizeField", faceBlockSizeField.getText());

    Settings.set("PreviewPanel_MeshInvestigator_UVOffsetField", uvOffsetField.getText());

    buildMeshFromFile();

    if (file == null || !file.exists()) {
      return;
    }

    Platform.setImplicitExit(false); // stop the JavaFX from dying when the thread finishes
    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      // RESET THINGS
      root = new Group();
      world = new Transform3D();

      camera = new PerspectiveCamera(true);
      cameraXform = new Transform3D();
      cameraXform2 = new Transform3D();
      cameraXform3 = new Transform3D();

      // START BUILDING
      root.getChildren().add(world);
      root.setDepthTest(DepthTest.ENABLE);

      //root.setCacheHint(CacheHint.QUALITY);

      // buildScene();
      buildCamera();

      if (triangleMesh != null) {
        // Build it from a TriangleMesh
        MeshView view = new MeshView(triangleMesh);
        view.setDrawMode(DrawMode.FILL);
        view.setCullFace(CullFace.NONE);

        world.getChildren().add(view);
      }
      else if (meshView != null) {
        // Build it from a MeshView

        int numViews = meshView.length;
        for (int v = 0; v < numViews; v++) {
          // show textures
          meshView[v].setDrawMode(DrawMode.FILL);
          meshView[v].setCullFace(CullFace.NONE);

          world.getChildren().add(meshView[v]);
        }

      }

      int width = 0;//jfxPanel.getWidth(); // 1024
      int height = 0;//jfxPanel.getHeight(); // 768

      try {
        WSSidePanelHolder sidePanel = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");

        //if (width == 0) {
        width = sidePanel.getWidth() - 50;
        //}
        //if (height == 0) {
        height = sidePanel.getHeight() - 150;
        //}
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

      if (width == 0) {
        width = previousWidth;
      }
      if (height == 0) {
        height = previousHeight;
      }

      previousWidth = width;
      previousHeight = height;

      if (width == 0) {
        width = 300;
      }

      double scale = 1d;
      if (sizes != null) {
        if (center != null && height != 0) {
          // center vertically

          // Because we've rotated the object by 90 degrees, we need to translate "Y" based on the center "Z"
          //double moveZ = 0 - center.getZ();
          //camera.setTranslateY(moveZ);

          double moveX = 0 - center.getX();
          camera.setTranslateX(moveX);

          // TEST START
          double moveY = 0 - center.getY();
          camera.setTranslateY(moveY);

          double moveZ = 0 - center.getZ();
          camera.setTranslateZ(moveZ);
          // TEST END

          cameraXform.rx.setPivotX(center.getX());
          cameraXform.rx.setPivotY(center.getY());
          cameraXform.rx.setPivotZ(center.getZ());

          cameraXform.ry.setPivotX(center.getX());
          cameraXform.ry.setPivotY(center.getY());
          cameraXform.ry.setPivotZ(center.getZ());

          cameraXform.rz.setPivotX(center.getX());
          cameraXform.rz.setPivotY(center.getY());
          cameraXform.rz.setPivotZ(center.getZ());

        }

        double modelSize = sizes.getX();

        double diffY = sizes.getY();
        if (diffY > modelSize) {
          modelSize = diffY;
        }

        double diffZ = sizes.getZ();
        if (diffZ > modelSize) {
          modelSize = diffZ;
        }

        //if (width != 0) {

        //scale = ((modelSize / (Math.tan(width)))) * 30;// * 1.5;// * 10;
        //scale = ((modelSize / (Math.tan(90)))) * 5;
        scale = ((modelSize / (Math.tan(180)))) * (1 / ((float) width) * 2500);

        //System.out.println("NORMAL: \t" + width + "\t" + height + "\t" + modelSize + "\t" + scale);

        if (scale > 0) {
          scale = 0 - scale;
        }
        camera.setTranslateZ(scale);
        //}

      }

      scene = new Scene(root, width, height, true, SceneAntialiasing.BALANCED);

      // Set the background color to whatever was last chosen
      String backgroundColor = Settings.getString("PreviewPanel_3DModel_BackgroundColor");
      if (backgroundColor.equals("BLACK")) {
        scene.setFill(Color.BLACK);
      }
      else if (backgroundColor.equals("WHITE")) {
        scene.setFill(Color.WHITE);
      }
      else {
        scene.setFill(Color.GREY);
      }

      handleMouse(scene, world);

      scene.setCamera(camera);
      jfxPanel.setScene(scene);

    });

  }

  /** for remembering the dimension of the jfxPanel **/
  static int previousWidth = 0;

  static int previousHeight = 0;

  Group root = new Group();

  Transform3D world = new Transform3D();

  PerspectiveCamera camera = new PerspectiveCamera(true);

  Transform3D cameraXform = new Transform3D();

  Transform3D cameraXform2 = new Transform3D();

  Transform3D cameraXform3 = new Transform3D();

  private static final double CAMERA_INITIAL_DISTANCE = -1000;

  private static final double CAMERA_INITIAL_X_ANGLE = 90;//70.0;

  private static final double CAMERA_INITIAL_Y_ANGLE = 0;//320.0;

  private static final double CAMERA_INITIAL_Z_ANGLE = 45;//320.0;

  private static final double CAMERA_NEAR_CLIP = 0.01;

  private static final double CAMERA_FAR_CLIP = 50000.0;

  /*
  private static final double AXIS_LENGTH = 250.0;
  
  private static final double HYDROGEN_ANGLE = 104.5;
  */

  private static final double CONTROL_MULTIPLIER = 0.1;

  private static final double SHIFT_MULTIPLIER = 10.0;

  private static final double MOUSE_SPEED = 1;

  private static final double ROTATION_SPEED = 1.0;

  double mousePosX;

  double mousePosY;

  double mouseOldX;

  double mouseOldY;

  double mouseDeltaX;

  double mouseDeltaY;

  private void buildCamera() {
    root.getChildren().add(cameraXform);
    cameraXform.getChildren().add(cameraXform2);
    cameraXform2.getChildren().add(cameraXform3);
    cameraXform3.getChildren().add(camera);
    cameraXform3.setRotateZ(180.0);
    camera.setNearClip(CAMERA_NEAR_CLIP);
    camera.setFarClip(CAMERA_FAR_CLIP);
    camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
    cameraXform.rz.setAngle(CAMERA_INITIAL_Z_ANGLE);
    cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
    cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
  }

  private void buildUVCamera() {
    root.getChildren().add(cameraXform);
    cameraXform.getChildren().add(cameraXform2);
    cameraXform2.getChildren().add(cameraXform3);
    cameraXform3.getChildren().add(camera);
    cameraXform3.setRotateY(90.0);
    cameraXform3.setRotateZ(90.0);
    camera.setNearClip(CAMERA_NEAR_CLIP);
    camera.setFarClip(CAMERA_FAR_CLIP);
    camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);

    cameraXform.rz.setAngle(0);
    cameraXform.ry.setAngle(0);
    cameraXform.rx.setAngle(90);
  }

  private void handleMouse(Scene scene, final Node root) {
    scene.setOnMouseClicked(new EventHandler<MouseEvent>() {

      @Override
      public void handle(MouseEvent me) {
        snapshotImage = null; // reset the snapshot image

        toggleBackgroundColor();
      }
    });
    scene.setOnMousePressed(new EventHandler<MouseEvent>() {

      @Override
      public void handle(MouseEvent me) {
        //dragging = true;
        snapshotImage = null; // reset the snapshot image

        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseOldX = me.getSceneX();
        mouseOldY = me.getSceneY();
      }
    });
    scene.setOnMouseDragged(new EventHandler<MouseEvent>() {

      @Override
      public void handle(MouseEvent me) {
        dragging = true; // so we don't toggle the background color change

        snapshotImage = null; // reset the snapshot image

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);

        double modifier = 1.0;

        if (me.isControlDown()) {
          modifier = CONTROL_MULTIPLIER;
        }
        if (me.isShiftDown()) {
          modifier = SHIFT_MULTIPLIER;
        }
        if (me.isPrimaryButtonDown()) {
          cameraXform.ry.setAngle(cameraXform.ry.getAngle() - mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED);
          cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED);
        }
        else if (me.isSecondaryButtonDown()) {
          double z = camera.getTranslateZ();
          double multiplier = (0 - z) * 0.1;
          //double newZ = z + mouseDeltaY * (MOUSE_SPEED /* * 2*/) * modifier;
          double newZ = z + mouseDeltaY * multiplier;
          camera.setTranslateZ(newZ);
        }
        else if (me.isMiddleButtonDown()) {
          //cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED);
          //cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED);
          cameraXform.rz.setAngle(cameraXform.rz.getAngle() - mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED);
        }
      }
    });
  }

  Image snapshotImage = null;

  int snapshotHeight = 0;

  int snapshotWidth = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void generateSnapshot(int width, int height) {
    snapshotWidth = width;
    snapshotHeight = height;
    generateSnapshot();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void generateSnapshot() {

    // queue on JavaFX thread and wait for completion
    final CountDownLatch doneLatch = new CountDownLatch(1);

    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      try {

        if (snapshotWidth == 0) {
          snapshotWidth = 2000;
        }
        if (snapshotHeight == 0) {
          snapshotHeight = 2000;
        }

        WritableImage image = new WritableImage(snapshotHeight, snapshotWidth);

        // CENTER THE MODEL IN THE SNAPSHOT SIZE
        if (center != null) {
          // center vertically

          // Because we've rotated the object by 90 degrees, we need to translate "Y" based on the center "Z"
          //double moveZ = 0 - center.getZ();
          //camera.setTranslateY(moveZ);

          double moveX = 0 - center.getX();
          camera.setTranslateX(moveX);

          /*
          // TEST START
          double moveY = 0 - center.getY();
          camera.setTranslateY(moveY);
          
          double moveZ = 0 - center.getZ();
          camera.setTranslateZ(moveZ);
          // TEST END
          */

          cameraXform.rx.setPivotX(center.getX());
          cameraXform.rx.setPivotY(center.getY());
          cameraXform.rx.setPivotZ(center.getZ());

          cameraXform.ry.setPivotX(center.getX());
          cameraXform.ry.setPivotY(center.getY());
          cameraXform.ry.setPivotZ(center.getZ());

          cameraXform.rz.setPivotX(center.getX());
          cameraXform.rz.setPivotY(center.getY());
          cameraXform.rz.setPivotZ(center.getZ());
        }

        // ZOOM TO FIX THE MODEL IN THE SNAPSHOT SIZE
        if (snapshotWidth != 2000 && snapshotHeight != 2000 && sizes != null) {
          // If we've already set the appropriate zoom for generating the preview, don't need to change the zoom here, as it's implicit in scene.snapshot()
          double modelSize = sizes.getX();

          double diffY = sizes.getY();
          if (diffY > modelSize) {
            modelSize = diffY;
          }

          double diffZ = sizes.getZ();
          if (diffZ > modelSize) {
            modelSize = diffZ;
          }

          if (snapshotWidth != 0) {
            //double scale = ((modelSize / (Math.tan(snapshotWidth)))) * 1.5;
            double scale = ((modelSize / (Math.tan(180)))) * (1 / ((float) snapshotWidth) * 600);
            //System.out.println("THUMBNAIL: \t" + snapshotWidth + "\t" + snapshotHeight + "\t" + modelSize + "\t" + scale);
            if (scale > 0) {
              scale = 0 - scale;
            }
            camera.setTranslateZ(scale);
          }
        }
        // END OF ZOOM
        //previousWidth = 0;
        //previousHeight = 0;

        scene.snapshot(image);

        BufferedImage bufImage = SwingFXUtils.fromFXImage(image, null);

        snapshotImage = bufImage;

      }
      finally {
        doneLatch.countDown();
      }
    });

    try {
      doneLatch.await();
    }
    catch (InterruptedException e) {
      ErrorLogger.log(e);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image getImage() {
    if (snapshotImage == null) {
      generateSnapshot();
    }
    return snapshotImage;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getImageHeight() {
    if (snapshotImage == null) {
      generateSnapshot();
    }
    return snapshotHeight;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getImageWidth() {
    if (snapshotImage == null) {
      generateSnapshot();
    }
    return snapshotWidth;
  }

  Point3D center = null;

  Point3D sizes = null;

  TriangleMesh triangleMesh = null;

  MeshView[] meshView = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public TriangleMesh getModel() {
    return triangleMesh;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setModel(TriangleMesh triangleMesh) {
    this.triangleMesh = triangleMesh;
  }

  boolean dragging = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void toggleBackgroundColor() {
    if (scene == null) {
      return;
    }
    if (dragging) { // if we were dragging, don't change the background, that's only on click
      dragging = false;
      return;
    }

    String backgroundColor = Settings.getString("PreviewPanel_3DModel_BackgroundColor");

    if (backgroundColor.equals("BLACK")) {
      // Change to White
      Settings.set("PreviewPanel_3DModel_BackgroundColor", "WHITE");
      scene.setFill(Color.WHITE);
    }
    else if (backgroundColor.equals("WHITE")) {
      // Change to Grey
      Settings.set("PreviewPanel_3DModel_BackgroundColor", "GREY");
      scene.setFill(Color.GREY);
    }
    else {
      // Change to Black
      Settings.set("PreviewPanel_3DModel_BackgroundColor", "BLACK");
      scene.setFill(Color.BLACK);
    }
  }

}