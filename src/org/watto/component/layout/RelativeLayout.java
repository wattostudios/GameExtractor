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
import java.awt.Point;
import java.io.Serializable;

/***********************************************************************************************
This <code>Layout</code> positions a <code>Component</code> relative to a particular point on its
parent, and at its preferred size rather than being expanded to fill the parent area. For example,
the <code>Component</code> could be positioned at (10,10) from the bottom right corner of its
parent <code>Component</code>
***********************************************************************************************/

public class RelativeLayout implements LayoutManager2, Serializable {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The center <code>Component</code> **/
  Component center;

  /** The relative X position on the parent <code>Component</code>. Positive numbers are from the left of the parent <code>Component</code>, and negative numbers are from the right **/
  public int xPosition = 0;
  /** The relative Y position on the parent <code>Component</code>. Positive numbers are from the top of the parent <code>Component</code>, and negative numbers are from the bottom **/
  public int yPosition = 0;

  /** Whether to expand the height of the <code>center</code> <code>Component</code> to the height of the parent <code>Container</code> or not **/
  boolean fillHeight = false;
  /** Whether to expand the width of the <code>center</code> <code>Component</code> to the width of the parent <code>Container</code> or not **/
  boolean fillWidth = false;

  /***********************************************************************************************
  Creates an empty layout
  ***********************************************************************************************/
  public RelativeLayout() {
  }

  /***********************************************************************************************
  Sets the relative position of the <code>Component</code>
  @param xPosition The relative X position on the parent <code>Component</code>. Positive numbers
                   are from the left of the parent <code>Component</code>, and negative numbers are
                   from the right
  @param yPosition The relative Y position on the parent <code>Component</code>. Positive numbers
                   are from the top of the parent <code>Component</code>, and negative numbers are
                   from the bottom
  ***********************************************************************************************/
  public RelativeLayout(int xPosition, int yPosition) {
    this.xPosition = xPosition;
    this.yPosition = yPosition;
  }

  /***********************************************************************************************
  Sets the relative position of the <code>Component</code>, and whether the layout should expand
  to fill to the parent size in width or height
  @param xPosition The relative X position on the parent <code>Component</code>. Positive numbers
                   are from the left of the parent <code>Component</code>, and negative numbers are
                   from the right
  @param yPosition The relative Y position on the parent <code>Component</code>. Positive numbers
                   are from the top of the parent <code>Component</code>, and negative numbers are
                   from the bottom
  @param fillWidth <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                                width of the parent <code>Container</code><br />
                    <b>false</b> to keep the <code>center</code> <code>Component</code>s width
  @param fillHeight <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                                height of the parent <code>Container</code><br />
                    <b>false</b> to keep the <code>center</code> <code>Component</code>s height
  ***********************************************************************************************/
  public RelativeLayout(int xPosition, int yPosition, boolean fillWidth, boolean fillHeight) {
    this.xPosition = xPosition;
    this.yPosition = yPosition;
    this.fillWidth = fillWidth;
    this.fillHeight = fillHeight;
  }

  /***********************************************************************************************
  Add a <code>Component</code> to the layout
  @param component the <code>Component</code> to add
  @param position the position to add the <code>Component</code>
  ***********************************************************************************************/
  @Override
  public void addLayoutComponent(Component component, Object position) {
    synchronized (component.getTreeLock()) {
      if (position == null) {
        addLayoutComponent((String) null, component);
      }
      else if (position instanceof Point) {
        Point positionPoint = (Point) position;
        xPosition = positionPoint.x;
        yPosition = positionPoint.y;
        addLayoutComponent((String) null, component);
      }
      else if (position instanceof Dimension) {
        Dimension positionDimension = (Dimension) position;
        xPosition = positionDimension.width;
        yPosition = positionDimension.height;
        addLayoutComponent((String) null, component);
      }
      else {
        throw new IllegalArgumentException("The position of the component must be a Point or a Dimension");
      }
    }
  }

  /***********************************************************************************************
  Add a <code>Component</code> to the layout
  @param position the position to add the <code>Component</code>
  @param component the <code>Component</code> to add
  ***********************************************************************************************/
  @Override
  public void addLayoutComponent(String position, Component component) {
    synchronized (component.getTreeLock()) {
      /* Special case:  treat null the same as "Center". */
      if (position == null) {
        position = "Center";
      }

      /* Assign the component to one of the known regions of the layout. */
      if ("Center".equals(position)) {
        center = component;
      }
      else {
        throw new IllegalArgumentException("Unknown layout position: " + position);
      }
    }
  }

  /***********************************************************************************************
  Gets whether to expand the height of the <code>center</code> <code>Component</code> to the height
  of the parent <code>Container</code> or not
  @return <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                      height of the parent <code>Container</code><br />
          <b>false</b> to keep the <code>center</code> <code>Component</code>s height
  ***********************************************************************************************/
  public boolean getFillHeight() {
    return fillHeight;
  }

  /***********************************************************************************************
  Gets whether to expand the width of the <code>center</code> <code>Component</code> to the width
  of the parent <code>Container</code> or not
  @return <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                      width of the parent <code>Container</code><br />
          <b>false</b> to keep the <code>center</code> <code>Component</code>s width
  ***********************************************************************************************/
  public boolean getFillWidth() {
    return fillWidth;
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
  Gets the relative X position of this <code>Component</code> in its parent
  @return the <code>xPosition</code>
  ***********************************************************************************************/
  public int getXPosition() {
    return xPosition;
  }

  /***********************************************************************************************
  Gets the relative Y position of this <code>Component</code> in its parent
  @return the <code>yPosition</code>
  ***********************************************************************************************/
  public int getYPosition() {
    return yPosition;
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
    synchronized (target.getTreeLock()) {
      Insets insets = target.getInsets();

      int parentHeight = target.getHeight();
      int parentWidth = target.getWidth();

      int parentHeightInsets = target.getHeight() - insets.top - insets.bottom;
      int parentWidthInsets = target.getWidth() - insets.left - insets.right;

      // first determine the size of the center component
      if (center != null) {
        Dimension size = center.getPreferredSize();

        int width = (int) size.getWidth();
        int height = (int) size.getHeight();

        // don't make the component bigger than its parent
        if (width > parentWidthInsets) {
          width = parentWidthInsets;
        }
        if (height > parentHeightInsets) {
          height = parentHeightInsets;
        }

        // apply the "fill" settings
        if (fillHeight) {
          height = parentHeightInsets;
        }
        if (fillWidth) {
          width = parentWidthInsets;
        }

        // determine the top-left corner of the component
        int left = xPosition;
        int top = yPosition;

        if (left < 0) {
          left = (parentWidth - width + left);
        }
        if (top < 0) {
          top = (parentHeight - height + top);
        }

        center.setSize(width, height);
        center.setBounds(left, top, width, height);
      }

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
    synchronized (target.getTreeLock()) {
      Dimension dim = new Dimension(0, 0);

      if (center != null) {
        Dimension d = center.getMinimumSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }

      Insets insets = target.getInsets();
      dim.width += insets.left + insets.right;
      dim.height += insets.top + insets.bottom;

      return dim;
    }
  }

  /***********************************************************************************************
  Gets the preferred layout size when drawn on the <code>target</code> <code>Container</code>
  @param target the <code>Container</code> that this layout is to be drawn on
  @return the preferred layout size
  ***********************************************************************************************/
  @Override
  public Dimension preferredLayoutSize(Container target) {
    synchronized (target.getTreeLock()) {
      Dimension dim = new Dimension(0, 0);

      if (center != null) {
        Dimension d = center.getPreferredSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }

      Insets insets = target.getInsets();
      dim.width += insets.left + insets.right;
      dim.height += insets.top + insets.bottom;

      return dim;
    }
  }

  /***********************************************************************************************
  Removes a <code>Component</code> from the layout
  @param component the <code>Component</code> that is to be removed
  ***********************************************************************************************/
  @Override
  public void removeLayoutComponent(Component component) {
    synchronized (component.getTreeLock()) {
      if (component == center) {
        center = null;
      }
    }
  }

  /***********************************************************************************************
  Sets whether to expand the height of the <code>center</code> <code>Component</code> to the height
  of the parent <code>Container</code> or not
  @param fillHeight <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                                height of the parent <code>Container</code><br />
                    <b>false</b> to keep the <code>center</code> <code>Component</code>s height
  ***********************************************************************************************/
  public void setFillHeight(boolean fillHeight) {
    this.fillHeight = fillHeight;
  }

  /***********************************************************************************************
  Sets whether to expand the width of the <code>center</code> <code>Component</code> to the width
  of the parent <code>Container</code> or not
  @param fillWidth <b>true</b> to expand the <code>center</code> <code>Component</code> to the
                                width of the parent <code>Container</code><br />
                    <b>false</b> to keep the <code>center</code> <code>Component</code>s width
  ***********************************************************************************************/
  public void setFillWidth(boolean fillWidth) {
    this.fillWidth = fillWidth;
  }

  /***********************************************************************************************
  Sets the relative X position of this <code>Component</code> in its parent
  @param xPosition the relative X position of this <code>Component</code> in its parent
  ***********************************************************************************************/
  public void setXPosition(int xPosition) {
    this.xPosition = xPosition;
  }

  /***********************************************************************************************
  Sets the relative Y position of this <code>Component</code> in its parent
  @param yPosition the relative Y position of this <code>Component</code> in its parent
  ***********************************************************************************************/
  public void setYPosition(int yPosition) {
    this.yPosition = yPosition;
  }

  /***********************************************************************************************
  Gets a <code>String</code> representation of this <code>Object</code>
  @return a <code>String</code> representation of this <code>Object</code>
  ***********************************************************************************************/
  @Override
  public String toString() {
    return getClass().getName();
  }
}