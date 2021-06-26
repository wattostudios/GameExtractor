////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.plaf;

import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalFileChooserUI;

/***********************************************************************************************
 * Used to paint the GUI for <code>JFileChooser</code>s
 ***********************************************************************************************/
public class ButterflyFileChooserUI extends MetalFileChooserUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyFileChooserUI</code> instance for rendering the
   * <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyFileChooserUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyFileChooserUI((JFileChooser) component);
  }

  /**
   **********************************************************************************************
   * Constructs the GUI for the given <code>JFileChooser</code>
   * @param fileChooser the <code>JFileChooser</code> to work on
   **********************************************************************************************
   **/
  public ButterflyFileChooserUI(JFileChooser fileChooser) {
    super(fileChooser);
  }

  /***********************************************************************************************
   * Creates the <code>JList</code> for showing the list of <code>File</code>s
   * @param fileChooser the <code>JFileChooser</code> to work on
   * @return a <code>JList</code> in a <code>JPanel</code>
   ***********************************************************************************************/
  @Override
  protected JPanel createList(JFileChooser fileChooser) {
    JPanel listPanel = super.createList(fileChooser);

    Dimension oldDimension = listPanel.getPreferredSize();
    Dimension newDimension = new Dimension((int) oldDimension.getWidth(), (int) oldDimension.getHeight() + 240);
    listPanel.setMaximumSize(newDimension);
    listPanel.setMinimumSize(newDimension);
    listPanel.setPreferredSize(newDimension);

    return listPanel;
  }

  /***********************************************************************************************
   * Installs the <code>JButton</code> <code>Icon</code>s for the <code>JFileChooser</code>
   * @param fileChooser the <code>JFileChooser</code> to work on
   ***********************************************************************************************/
  @Override
  protected void installIcons(JFileChooser fileChooser) {

    //fileIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyFileIcon.png"));
    //directoryIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyDirectoryIcon.png"));
    //floppyDriveIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyFloppyDriveIcon.png"));
    //upFolderIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyUpFolderIcon.png"));
    //newFolderIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyNewFolderIcon.png"));
    //computerIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyComputerIcon.png"));
    //hardDriveIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyHardDriveIcon.png"));
    //homeFolderIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyHomeFolderIcon.png"));
    //listViewIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyListViewIcon.png"));
    //detailsViewIcon = new ImageIcon(WSHelper.getResource("images/WSFileChooser/ButterflyDetailsViewIcon.png"));

    fileIcon = new ImageIcon("images/WSFileChooser/FileIcon.png");
    directoryIcon = new ImageIcon("images/WSFileChooser/DirectoryIcon.png");
    floppyDriveIcon = new ImageIcon("images/WSFileChooser/FloppyDriveIcon.png");
    upFolderIcon = new ImageIcon("images/WSFileChooser/UpFolderIcon.png");
    newFolderIcon = new ImageIcon("images/WSFileChooser/NewFolderIcon.png");
    computerIcon = new ImageIcon("images/WSFileChooser/ComputerIcon.png");
    hardDriveIcon = new ImageIcon("images/WSFileChooser/HardDriveIcon.png");
    homeFolderIcon = new ImageIcon("images/WSFileChooser/HomeFolderIcon.png");
    listViewIcon = new ImageIcon("images/WSFileChooser/ListViewIcon.png");
    detailsViewIcon = new ImageIcon("images/WSFileChooser/DetailsViewIcon.png");

  }

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);

    Dimension oldDimension = component.getPreferredSize();
    Dimension newDimension = new Dimension((int) oldDimension.getWidth(), (int) oldDimension.getHeight() + 44);
    component.setPreferredSize(newDimension);
  }
}