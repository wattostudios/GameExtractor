/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

/*
 * Based on Code from Oracle/Sun...
 * 
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSSelectableInterface;
import org.watto.event.listener.WSSelectableListener;
import org.watto.ge.helper.Transform3D;
import org.watto.xml.XMLReader;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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
import javafx.scene.shape.Mesh;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class PreviewPanel_3DModel extends PreviewPanel implements WSSelectableInterface, WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

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

  /**
  **********************************************************************************************
  DO NOT USE - Only to generate a dummy panel for finding writable ViewerPlugins for this type
  **********************************************************************************************
  **/
  public PreviewPanel_3DModel() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_3DModel(TriangleMesh triangleMeshIn) {
    super();
    triangleMesh = triangleMeshIn;
    createInterface();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_3DModel(TriangleMesh triangleMeshIn, Point3D sizesIn, Point3D centerIn) {
    super();
    triangleMesh = triangleMeshIn;
    sizes = sizesIn;
    center = centerIn;
    createInterface();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_3DModel(MeshView meshViewIn, Point3D sizesIn, Point3D centerIn) {
    super();
    meshView = new MeshView[] { meshViewIn };
    sizes = sizesIn;
    center = centerIn;
    createInterface();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PreviewPanel_3DModel(MeshView[] meshViewIn, Point3D sizesIn, Point3D centerIn) {
    super();
    meshView = meshViewIn;
    sizes = sizesIn;
    center = centerIn;
    createInterface();
  }

  Point3D center = null;

  Point3D sizes = null;

  /**
   **********************************************************************************************
   
   **********************************************************************************************
   **/
  @Override
  public boolean onDeselect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_3DModel_Wireframe")) {
        setWireframe(checkbox.isSelected());
      }
      else if (code.equals("PreviewPanel_3DModel_Smoothing")) {
        setSmoothing(checkbox.isSelected());
      }
      else if (code.equals("PreviewPanel_3DModel_ShowTextures")) {
        setShowTextures(checkbox.isSelected());
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onSelect(JComponent c, Object e) {
    if (c instanceof WSCheckBox) { // WSCheckBox, not WSOptionCheckBox, because we've registered the listener on the checkbox
      WSCheckBox checkbox = (WSCheckBox) c;
      String code = checkbox.getCode();
      if (code.equals("PreviewPanel_3DModel_Wireframe")) {
        setWireframe(checkbox.isSelected());
      }
      else if (code.equals("PreviewPanel_3DModel_Smoothing")) {
        setSmoothing(checkbox.isSelected());
      }
      else if (code.equals("PreviewPanel_3DModel_ShowTextures")) {
        setShowTextures(checkbox.isSelected());
      }

      return true; // changing the Setting is handled by a separate listener on the WSObjectCheckbox class, so we can return true here OK
    }
    return false;
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setWireframe(boolean wireframe) {
    ObservableList<Node> nodes = world.getChildren();
    int numNodes = nodes.size();
    for (int i = 0; i < numNodes; i++) {
      Node node = nodes.get(i);
      if (node instanceof MeshView) {
        if (wireframe) {
          ((MeshView) node).setDrawMode(DrawMode.LINE);
        }
        else {
          ((MeshView) node).setDrawMode(DrawMode.FILL);
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setShowTextures(boolean showTextures) {
    if (meshView == null) {
      // we didn't have textures anyway
      return;
    }

    Platform.setImplicitExit(false); // stop the JavaFX from dying when the thread finishes
    Platform.runLater(() -> { // FX components need to be managed by JavaFX

      int numFound = 0;

      ObservableList<Node> nodes = world.getChildren();
      int numNodes = nodes.size();
      for (int i = 0; i < numNodes; i++) {
        Node node = nodes.get(i);
        if (node instanceof MeshView) {
          if (showTextures) {
            //((MeshView) node).setMaterial(meshView.getMaterial());
            nodes.set(i, meshView[numFound]);
            numFound++;
          }
          else {
            MeshView view = new MeshView(meshView[numFound].getMesh());
            view.setDrawMode(DrawMode.FILL);
            view.setCullFace(CullFace.NONE);

            nodes.set(i, view);
            numFound++;
          }
        }
      }

    });
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSmoothing(boolean smoothing) {
    ObservableList<Node> nodes = world.getChildren();
    int numNodes = nodes.size();
    for (int i = 0; i < numNodes; i++) {
      Node node = nodes.get(i);
      if (node instanceof MeshView) {
        MeshView meshView = (MeshView) node;
        Mesh mesh = meshView.getMesh();
        if (mesh instanceof TriangleMesh) {
          TriangleMesh triangleMesh = (TriangleMesh) mesh;

          int faceCount = triangleMesh.getFaces().size() / triangleMesh.getFaceElementSize();

          int[] smoothingGroups = new int[faceCount];
          if (smoothing) {
            Arrays.fill(smoothingGroups, 1); // means each item is smoothed to the same object
          }
          else {
            Arrays.fill(smoothingGroups, 0); // means no smoothing
          }

          triangleMesh.getFaceSmoothingGroups().setAll(smoothingGroups);
        }
      }
    }
  }

  Scene scene = null;

  JFXPanel jfxPanel = null;

  WSPanel mainPanel = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void createInterface() {

    WSOptionCheckBox wireframeCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_3DModel_Wireframe\" setting=\"PreviewPanel_3DModel_Wireframe\" />"));
    WSOptionCheckBox smoothingCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_3DModel_Smoothing\" setting=\"PreviewPanel_3DModel_Smoothing\" />"));
    WSOptionCheckBox texturesCheckbox = new WSOptionCheckBox(XMLReader.read("<WSOptionCheckBox opaque=\"false\" code=\"PreviewPanel_3DModel_ShowTextures\" setting=\"PreviewPanel_3DModel_ShowTextures\" />"));

    //add a listener to the checkbox, so we can capture and process select/deselect
    WSSelectableListener selectableListener = new WSSelectableListener(this);
    wireframeCheckbox.addItemListener(selectableListener);
    smoothingCheckbox.addItemListener(selectableListener);
    texturesCheckbox.addItemListener(selectableListener);

    WSPanel topPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" layout=\"GridLayout\" rows=\"1\" columns=\"3\" />"));
    topPanel.add(wireframeCheckbox);
    topPanel.add(smoothingCheckbox);
    topPanel.add(texturesCheckbox);

    add(topPanel, BorderLayout.NORTH);

    jfxPanel = new JFXPanel(); // Scrollable JCompenent

    mainPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" border-width=\"4\" code=\"PreviewPanel_3DModel_MainPanel\"></WSPanel>"));
    mainPanel.add(jfxPanel, BorderLayout.CENTER);

    add(mainPanel, BorderLayout.CENTER);

    //
    // Rotation Controls
    //

    WSPanel rotatePanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\" layout=\"GridLayout\" rows=\"3\" columns=\"3\" />"));
    rotatePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    rotatePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_RotateUp\" />")));
    rotatePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));

    rotatePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_RotateLeft\" />")));
    rotatePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    rotatePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_RotateRight\" />")));

    rotatePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    rotatePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_RotateDown\" />")));
    rotatePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));

    WSPanel rotateWithLabelPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" />"));
    rotateWithLabelPanel.add(new WSLabel(XMLReader.read("<WSLabel code=\"PreviewPanel_3DModel_RotateLabel\" />")), BorderLayout.NORTH);
    rotateWithLabelPanel.add(rotatePanel, BorderLayout.CENTER);

    //
    // Move Controls
    //

    WSPanel movePanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\" layout=\"GridLayout\" rows=\"3\" columns=\"3\" />"));
    movePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    movePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_MoveUp\" />")));
    movePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));

    movePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_MoveLeft\" />")));
    movePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    movePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_MoveRight\" />")));

    movePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));
    movePanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_MoveDown\" />")));
    movePanel.add(new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" />")));

    WSPanel moveWithLabelPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" />"));
    moveWithLabelPanel.add(new WSLabel(XMLReader.read("<WSLabel code=\"PreviewPanel_3DModel_MoveLabel\" />")), BorderLayout.NORTH);
    moveWithLabelPanel.add(movePanel, BorderLayout.CENTER);

    //
    // Zoom Controls
    //

    WSPanel zoomPanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\" layout=\"GridLayout\" rows=\"2\" columns=\"1\" />"));
    zoomPanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_ZoomIn\" />")));
    zoomPanel.add(new WSButton(XMLReader.read("<WSButton code=\"PreviewPanel_3DModel_ZoomOut\" />")));

    WSPanel zoomWithLabelPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" />"));
    zoomWithLabelPanel.add(new WSLabel(XMLReader.read("<WSLabel code=\"PreviewPanel_3DModel_ZoomLabel\" />")), BorderLayout.NORTH);
    zoomWithLabelPanel.add(zoomPanel, BorderLayout.CENTER);

    //
    // Add the controls to the interface
    //

    WSPanel controlPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"1\" columns=\"3\" />"));
    controlPanel.add(rotateWithLabelPanel);
    controlPanel.add(moveWithLabelPanel);
    controlPanel.add(zoomWithLabelPanel);

    add(controlPanel, BorderLayout.SOUTH);

    reloadMesh();

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reloadMesh() {

    Platform.setImplicitExit(false); // stop the JavaFX from dying when the thread finishes
    Platform.runLater(() -> { // FX components need to be managed by JavaFX
      root.getChildren().add(world);
      root.setDepthTest(DepthTest.ENABLE);

      //root.setCacheHint(CacheHint.QUALITY);

      // buildScene();
      buildCamera();

      boolean showTextures = Settings.getBoolean("PreviewPanel_3DModel_ShowTextures");

      if (triangleMesh != null) {
        // Build it from a TriangleMesh
        MeshView view = new MeshView(triangleMesh);
        view.setDrawMode(DrawMode.FILL);
        view.setCullFace(CullFace.NONE);

        world.getChildren().add(view);

        setWireframe(Settings.getBoolean("PreviewPanel_3DModel_Wireframe"));
        setSmoothing(Settings.getBoolean("PreviewPanel_3DModel_Smoothing"));
      }
      else if (meshView != null) {
        // Build it from a MeshView

        int numViews = meshView.length;
        for (int v = 0; v < numViews; v++) {
          if (!showTextures) {
            // if we're displaying a MeshView (which has textures on it), and the user doesn't want to see textures, just show the triangleMesh instead
            MeshView view = new MeshView(meshView[v].getMesh());
            view.setDrawMode(DrawMode.FILL);
            view.setCullFace(CullFace.NONE);

            world.getChildren().add(view);
          }
          else {
            // show textures
            meshView[v].setDrawMode(DrawMode.FILL);
            meshView[v].setCullFace(CullFace.NONE);

            world.getChildren().add(meshView[v]);
          }
        }

        setWireframe(Settings.getBoolean("PreviewPanel_3DModel_Wireframe"));
        setSmoothing(Settings.getBoolean("PreviewPanel_3DModel_Smoothing"));
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void onCloseRequest() {
    // Flush the variables clear for garbage collection
    triangleMesh = null;
    meshView = null;
    snapshotImage = null;
    scene = null;
    snapshotImage = null;
    snapshotWidth = 0;
    snapshotHeight = 0;

    jfxPanel = null;
    mainPanel = null;

  }

  final Group root = new Group();

  final Transform3D world = new Transform3D();

  final PerspectiveCamera camera = new PerspectiveCamera(true);

  final Transform3D cameraXform = new Transform3D();

  final Transform3D cameraXform2 = new Transform3D();

  final Transform3D cameraXform3 = new Transform3D();

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean onClick(JComponent source, java.awt.event.MouseEvent event) {
    if (source instanceof WSComponent) {
      String code = ((WSComponent) source).getCode();

      int rotateAmount = 10;
      double moveAmount = 1;
      double zoomAmount = 1;

      double multiplier = (0 - camera.getTranslateZ()) * 0.1;
      moveAmount *= (multiplier / 2);
      zoomAmount *= multiplier;

      if (code.equals("PreviewPanel_3DModel_RotateLeft")) {
        cameraXform.rz.setAngle(cameraXform.rz.getAngle() - rotateAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_RotateRight")) {
        cameraXform.rz.setAngle(cameraXform.rz.getAngle() + rotateAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_RotateUp")) {
        cameraXform.rx.setAngle(cameraXform.rx.getAngle() - rotateAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_RotateDown")) {
        cameraXform.rx.setAngle(cameraXform.rx.getAngle() + rotateAmount);
      }

      else if (code.equals("PreviewPanel_3DModel_MoveLeft")) {
        cameraXform2.t.setX(cameraXform2.t.getX() - moveAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_MoveRight")) {
        cameraXform2.t.setX(cameraXform2.t.getX() + moveAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_MoveUp")) {
        cameraXform2.t.setY(cameraXform2.t.getY() - moveAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_MoveDown")) {
        cameraXform2.t.setY(cameraXform2.t.getY() + moveAmount);
      }

      else if (code.equals("PreviewPanel_3DModel_ZoomIn")) {
        camera.setTranslateZ(camera.getTranslateZ() + zoomAmount);
      }
      else if (code.equals("PreviewPanel_3DModel_ZoomOut")) {
        camera.setTranslateZ(camera.getTranslateZ() - zoomAmount);
      }

      else {
        return false;
      }
      return true;

    }
    return false;
  }

}