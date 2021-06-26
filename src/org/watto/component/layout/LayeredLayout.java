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

package org.watto.component.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;

/***********************************************************************************************
A layout where a number of <code>Component</code>s can be layered on top of each other
***********************************************************************************************/

public class LayeredLayout implements LayoutManager2, Serializable {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The <code>Component</code>s. The topmost layer has the largest index number. **/
  Component[] layers = new Component[0];

  /***********************************************************************************************
  Creates an empty layout with no <code>Component</code>s
  ***********************************************************************************************/
  public LayeredLayout() {
  }

  /***********************************************************************************************
  Creates a layout with a single <code>Component</code>
  @param component the <code>Component</code>
  ***********************************************************************************************/
  public LayeredLayout(Component component) {
    layers = new Component[] { component };
  }

  /***********************************************************************************************
  Creates a layout with a number of layered <code>Component</code>s
  @param components the <code>Component</code>s
  ***********************************************************************************************/
  public LayeredLayout(Component[] components) {
    layers = components;
  }

  /***********************************************************************************************
  Adds a <code>Component</code> to the layout. The <code>Component</code> is added to the topmost
  position
  @param component the <code>Component</code> to add
  ***********************************************************************************************/
  public void addLayoutComponent(Component component) {
    addLayoutComponent(component, layers.length);
  }

  /***********************************************************************************************
  Adds a <code>Component</code> to the layout, in the <code>index</code> position.
  @param component the <code>Component</code> to add
  @param index the position in the <code>layers</code> array to add the <code>Component</code>
  ***********************************************************************************************/
  public void addLayoutComponent(Component component, int index) {
    if (index < 0) {
      index = 0;
    }

    int layerCount = layers.length;
    if (index > layerCount) {
      index = layerCount;
    }

    Component[] oldLayers = layers;
    layers = new Component[layerCount + 1];

    // Copy the first half of the components
    System.arraycopy(oldLayers, 0, layers, 0, index);
    // Add the new component
    layers[index] = component;
    // Copy the second half of the components
    System.arraycopy(oldLayers, index, layers, index + 1, layerCount - index);
  }

  /***********************************************************************************************
  Add a <code>Component</code> to the layout
  @param component the <code>Component</code> to add
  @param position the position to add the <code>Component</code>
  ***********************************************************************************************/
  @Override
  public void addLayoutComponent(Component component, Object position) {
    int index = 0;
    try {
      index = Integer.parseInt((String) position);
    }
    catch (Throwable t) {
      index = layers.length;
    }
    addLayoutComponent(component, index);
  }

  /***********************************************************************************************
  Add a <code>Component</code> to the layout
  @param position the position to add the <code>Component</code>
  @param component the <code>Component</code> to add
  ***********************************************************************************************/
  @Override
  public void addLayoutComponent(String position, Component component) {
    int index = 0;
    try {
      index = Integer.parseInt(position);
    }
    catch (Throwable t) {
      index = layers.length;
    }
    addLayoutComponent(component, index);
  }

  /***********************************************************************************************
  Gets the <code>Component</code> at the <code>index</code> position
  @param index the position in the <code>layers</code> array to add the <code>Component</code>
  @return the <code>Component</code>, or <b>null</b> if the <code>Component</code> does not exist
  ***********************************************************************************************/
  public Component getComponent(int index) {
    if (index < 0) {
      index = 0;
    }
    else if (index >= layers.length) {
      index = layers.length - 1;
    }
    return layers[index];
  }

  /***********************************************************************************************
  Gets the X alignment of the <code>Component</code>s
  @param parent the parent <code>Container</code>
  @return <b>0.5f</b> - the X alignment
  ***********************************************************************************************/
  @Override
  public float getLayoutAlignmentX(Container parent) {
    return 0.5f;
  }

  /***********************************************************************************************
  Gets the Y alignment of the <code>Component</code>s
  @param parent the parent <code>Container</code>
  @return <b>0.5f</b> - the Y alignment
  ***********************************************************************************************/
  @Override
  public float getLayoutAlignmentY(Container parent) {
    return 0.5f;
  }

  /***********************************************************************************************
  Invalidates the layout, ready for a refresh
  @param target the <code>Container</code> that this layout is to be drawn on
  ***********************************************************************************************/
  @Override
  public void invalidateLayout(Container target) {
  }

  /***********************************************************************************************
  Sets the size of each <code>Component</code>
  @param target the <code>Container</code> that this layout is to be drawn on
  ***********************************************************************************************/
  @Override
  public void layoutContainer(Container target) {
    Insets insets = target.getInsets();

    int top = insets.top;
    int left = insets.left;

    int height = target.getHeight() - top - insets.bottom;
    int width = target.getWidth() - left - insets.right;

    int layerCount = layers.length;
    for (int i = 0; i < layerCount; i++) {
      Component component = layers[i];

      Dimension preferredSize;
      //if (i == 0){
      preferredSize = component.getMaximumSize();
      //}
      //else {
      //  preferredSize = component.getPreferredSize();
      //}
      int componentHeight = (int) preferredSize.getHeight();
      int componentWidth = (int) preferredSize.getWidth();
      int componentTop = top;
      int componentLeft = left;

      if (height < componentHeight) {
        componentHeight = height;
      }
      if (width < componentWidth) {
        componentWidth = width;
      }

      componentTop = (height - componentHeight) / 2;
      componentLeft = (width - componentWidth) / 2;

      component.setBounds(componentLeft, componentTop, componentWidth, componentHeight);
      component.setLocation(componentLeft, componentTop);
      component.setSize(componentWidth, componentHeight);
    }
  }

  /***********************************************************************************************
  Gets the maximum layout size when drawn on the <code>target</code> <code>Container</code>
  @param target the <code>Container</code> that this layout is to be drawn on
  @return the maximum layout size
  ***********************************************************************************************/
  @Override
  public Dimension maximumLayoutSize(Container target) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  /***********************************************************************************************
  Gets the minimum layout size when drawn on the <code>target</code> <code>Container</code>
  @param target the <code>Container</code> that this layout is to be drawn on
  @return the minimum layout size
  ***********************************************************************************************/
  @Override
  public Dimension minimumLayoutSize(Container target) {

    int maxWidth = 0;
    int maxHeight = 0;

    int layerCount = layers.length;
    for (int i = 0; i < layerCount; i++) {
      Dimension componentDimension = layers[i].getMinimumSize();
      if (componentDimension.width > maxWidth) {
        maxWidth = componentDimension.width;
      }
      if (componentDimension.height > maxHeight) {
        maxHeight = componentDimension.height;
      }
    }

    Insets insets = target.getInsets();
    maxWidth += insets.left + insets.right;
    maxHeight += insets.top + insets.bottom;

    return new Dimension(maxWidth, maxHeight);

  }

  /***********************************************************************************************
  Gets the preferred layout size when drawn on the <code>target</code> <code>Container</code>
  @param target the <code>Container</code> that this layout is to be drawn on
  @return the preferred layout size
  ***********************************************************************************************/
  @Override
  public Dimension preferredLayoutSize(Container target) {

    int maxWidth = 0;
    int maxHeight = 0;

    int layerCount = layers.length;
    for (int i = 0; i < layerCount; i++) {
      Dimension componentDimension = layers[i].getPreferredSize();
      if (componentDimension.width > maxWidth) {
        maxWidth = componentDimension.width;
      }
      if (componentDimension.height > maxHeight) {
        maxHeight = componentDimension.height;
      }
    }

    Insets insets = target.getInsets();
    maxWidth += insets.left + insets.right;
    maxHeight += insets.top + insets.bottom;

    return new Dimension(maxWidth, maxHeight);

  }

  /***********************************************************************************************
  Removes all <code>Component</code>s from the layout
  ***********************************************************************************************/
  public void removeAllLayoutComponents() {
    layers = new Component[0];
  }

  /***********************************************************************************************
  Removes a <code>Component</code> from the layout
  @param component the <code>Component</code> to remove
  ***********************************************************************************************/
  @Override
  public void removeLayoutComponent(Component component) {
    int layerCount = layers.length;
    for (int i = 0; i < layerCount; i++) {
      if (layers[i] == component) {
        removeLayoutComponent(i);
        return;
      }
    }
  }

  /***********************************************************************************************
  Removes a <code>Component</code> from the layout
  @param component the <code>Component</code> to remove
  @param index the position in the <code>layers</code> array to remove the <code>Component</code>
  ***********************************************************************************************/
  public void removeLayoutComponent(int index) {
    if (index < 0) {
      index = 0;
    }

    int layerCount = layers.length;
    if (index > layerCount) {
      index = layerCount - 1;
    }

    Component[] oldLayers = layers;
    layers = new Component[layerCount - 1];

    // Copy the first half of the components
    System.arraycopy(oldLayers, 0, layers, 0, index);
    int remainingLength = layerCount - index - 1;

    //System.out.println(index + " - " + remainingLength + " - " + layerCount);

    if (remainingLength > 0) {
      // Skip over the removed component, and copy the second half of the components
      System.arraycopy(oldLayers, index + 1, layers, index, remainingLength);
    }
  }

  /***********************************************************************************************
  Gets a <code>String</code> representation of this <code>Object</code>
  @return a <code>String</code> representation of this <code>Object</code>
  ***********************************************************************************************/
  @Override
  public String toString() {
    return getClass().getName() + "[layers=" + layers.length + "]";
  }
}