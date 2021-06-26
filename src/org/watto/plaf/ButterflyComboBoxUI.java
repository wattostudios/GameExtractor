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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.plaf.metal.MetalComboBoxUI;
import org.watto.component.WSComboBox;

/***********************************************************************************************
 * Used to paint the GUI for <code>WSComboBox</code>es
 ***********************************************************************************************/
public class ButterflyComboBoxUI extends MetalComboBoxUI {

  /***********************************************************************************************
   * Creates a <code>ButterflyButtonUI</code> instance for rendering the <code>component</code>
   * @param component the <code>Component</code> to get the painter for
   * @return a new <code>ButterflyButtonUI</code> instance
   ***********************************************************************************************/
  public static ComponentUI createUI(JComponent component) {
    return new ButterflyComboBoxUI();
  }

  /***********************************************************************************************
   * Creates the arrow <code>JButton</code> used to show or hide the popup
   * @return the arrow <code>JButton</code>
   ***********************************************************************************************/
  @Override
  protected JButton createArrowButton() {
    JButton button = super.createArrowButton();
    button.setOpaque(false);
    return button;
  }

  /***********************************************************************************************
   * Creates the editor field
   * @return the editor
   ***********************************************************************************************/
  @Override
  protected ComboBoxEditor createEditor() {
    MetalComboBoxEditor editor = new MetalComboBoxEditor();
    JTextField edit = (JTextField) editor.getEditorComponent();
    edit.setBackground(LookAndFeelManager.getMidColor());
    edit.setOpaque(false);
    //edit.setBorder(new EmptyBorder(0,0,0,0));

    // THIS IS PROBABLY WHERE WE SET THE COLOR OF EDITABLE FILECHOOSER FIELDS (JTEXTFIELD)
    // MAYBE CALL setUI() AND PASS IN A SPECIAL UI FOR THE PAINTING

    return editor;
  }

  /***********************************************************************************************
   * Creates the popup of values
   * @return the popup
   ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  @Override
  protected ComboPopup createPopup() {
    ButterflyComboPopup comboBoxPopup = new ButterflyComboPopup(comboBox);

    comboBoxPopup.setBorder(new EmptyBorder(0, 0, 0, 0));

    JList list = comboBoxPopup.getList();
    list.setOpaque(false);
    //list.setBackground(AquanauticTheme.COLOR_DARK);
    list.setSelectionBackground(LookAndFeelManager.getLightColor());
    list.setSelectionForeground(LookAndFeelManager.getTextColor());

    //comboBoxPopup.scroller.setOpaque(false);

    return comboBoxPopup;

  }

  /***********************************************************************************************
   * Sets up the painting properties for painting on the <code>Component</code>
   * @param component the <code>Component</code> that will be painted
   ***********************************************************************************************/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void installUI(JComponent component) {
    super.installUI(component);

    JComboBox comboBox = (JComboBox) component;
    comboBox.setRenderer(new ButterflyComboBoxCurrentItemRenderer());
    //comboBox.setRequestFocusEnabled(false);
    comboBox.setRequestFocusEnabled(true);
    comboBox.setOpaque(false);
  }

  /***********************************************************************************************
   * Paints the current value
   * @param graphics the <code>Graphics</code> to paint on
   * @param bounds the bounds of the painting <code>Rectangle</code>
   * @param hasFocus whether the current value has focus or not
   ***********************************************************************************************/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void paintCurrentValue(Graphics graphics, Rectangle bounds, boolean hasFocus) {
    ListCellRenderer renderer;
    if (comboBox instanceof WSComboBox) {
      renderer = ((WSComboBox) comboBox).getCurrentValueRenderer();
    }
    else {
      renderer = comboBox.getRenderer();
    }

    Component component;

    int padIn = LookAndFeelManager.getPropertyInt("BORDER_WIDTH");

    if (hasFocus && !isPopupVisible(comboBox)) {
      component = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, true, false);
    }
    else {
      component = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
      component.setBackground(new Color(0, 0, 0, 0));
    }

    component.setFont(comboBox.getFont());

    if (hasFocus && !isPopupVisible(comboBox)) {
      component.setForeground(Color.BLACK);
      component.setBackground(new Color(0, 0, 0, 0));
      //c.setBackground(Color.RED);
    }
    else {
      if (comboBox.isEnabled()) {
        component.setForeground(Color.BLACK);
        component.setBackground(new Color(0, 0, 0, 0));
        //c.setBackground(Color.RED);
      }
      else {
        component.setForeground(LookAndFeelManager.getMidColor());
        component.setBackground(new Color(0, 0, 0, 0));
        //c.setBackground(Color.RED);
      }
    }

    boolean shouldValidate = false;
    if (component instanceof JPanel) {
      shouldValidate = true;
    }

    //((JComponent)c).setOpaque(false);
    currentValuePane.paintComponent(graphics, component, comboBox, bounds.x + padIn, bounds.y, bounds.width - padIn - padIn, bounds.height, shouldValidate);
  }

  /***********************************************************************************************
   * Paints the background of the current value
   * @param graphics the <code>Graphics</code> to paint on
   * @param bounds the bounds of the painting <code>Rectangle</code>
   * @param hasFocus whether the current value has focus or not
   ***********************************************************************************************/
  @Override
  public void paintCurrentValueBackground(Graphics graphics, Rectangle bounds, boolean hasFocus) {

    int w = (int) bounds.getWidth();
    int h = (int) bounds.getHeight();

    //AquanauticPainter.paintGradient(g,0,0,w,h);
    //AquanauticPainter.paintLines(g,0,0,w,h);
    ButterflyPainter.paintCurvedGradient((Graphics2D) graphics, 0, 0, w, h);

  }

  /***********************************************************************************************
   * Removes the painting properties from the <code>Component</code>
   * @param component the <code>Component</code> to remove the properties from
   ***********************************************************************************************/
  @SuppressWarnings("rawtypes")
  @Override
  public void uninstallUI(JComponent component) {
    super.uninstallUI(component);

    JComboBox comboBox = (JComboBox) component;
    comboBox.setRequestFocusEnabled(true);
    comboBox.setOpaque(true);
  }
}