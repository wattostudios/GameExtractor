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
This layout is the complete opposite of a <code>BorderLayout</code>. In a <code>BorderLayout</code>,
the <code>Component</code>s on the outside are set to their minimum size, and the CENTER
<code>Component</code> fills the remaining space. However, in a <code>ReverseBorderLayout</code>,
the CENTER <code>Component</code> is set to its minimum size, and the <code>Component</code>s
on the outside fill the remaining space.
<br /><br />
The remaining space around the outside is assigned equally to the associated <code>Component</code>s.
For example, if there is both a NORTH and SOUTH <code>Component</code>, both <code>Component</code>s
will use half of the remaining space. If only 1 of these <code>Component</code>s exists, it will
use all the remaining space.
@see java.awt.BorderLayout
***********************************************************************************************/

public class ReverseBorderLayout implements LayoutManager2, Serializable {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** To position a <code>Component</code> in the center **/
  public static final String CENTER = "Center";
  /** To position a <code>Component</code> in the east **/
  public static final String EAST = "East";

  /** To position a <code>Component</code> in the north **/
  public static final String NORTH = "North";
  /** To position a <code>Component</code> in the south **/
  public static final String SOUTH = "South";
  /** To position a <code>Component</code> in the west **/
  public static final String WEST = "West";
  /** The horizontal gap between <code>Component</code>s **/
  int horizontalGap;
  /** The vertical gap between <code>Component</code>s **/
  int verticalGap;

  /** The center <code>Component</code> **/
  Component center;
  /** The east <code>Component</code> **/
  Component east;
  /** The north <code>Component</code> **/
  Component north;
  /** The south <code>Component</code> **/
  Component south;
  /** The west <code>Component</code> **/
  Component west;

  /***********************************************************************************************
  Creates an empty layout with no horizontal or vertical gap
  ***********************************************************************************************/
  public ReverseBorderLayout() {
    this(0, 0);
  }

  /***********************************************************************************************
  Creates an empty layout with a <code>horizontalGap</code> and <code>verticalGap</code> between
  the <code>Component</code>s
  @param horizontalGap the horizontal gap between the <code>Component</code>s
  @param verticalGap the vertical gap between the <code>Component</code>s
  ***********************************************************************************************/
  public ReverseBorderLayout(int horizontalGap, int verticalGap) {
    this.horizontalGap = horizontalGap;
    this.verticalGap = verticalGap;
  }

  /***********************************************************************************************
  Add a <code>Component</code> to the layout
  @param component the <code>Component</code> to add
  @param position the position to add the <code>Component</code>
  ***********************************************************************************************/
  @Override
  public void addLayoutComponent(Component component, Object position) {
    synchronized (component.getTreeLock()) {
      if ((position == null) || (position instanceof String)) {
        addLayoutComponent((String) position, component);
      }
      else {
        throw new IllegalArgumentException("The position of the component must be a String");
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
      else if ("North".equals(position)) {
        north = component;
      }
      else if ("South".equals(position)) {
        south = component;
      }
      else if ("East".equals(position)) {
        east = component;
      }
      else if ("West".equals(position)) {
        west = component;
      }
      else {
        throw new IllegalArgumentException("Unknown layout position: " + position);
      }
    }
  }

  /***********************************************************************************************
  Gets the <code>horizontalGap</code>
  @return the <code>horizontalGap</code>
  ***********************************************************************************************/
  public int getHorizontalGap() {
    return horizontalGap;
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
  Gets the <code>verticalGap</code>
  @return the <code>verticalGap</code>
  ***********************************************************************************************/
  public int getVerticalGap() {
    return verticalGap;
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

      int top = insets.top;
      int left = insets.left;

      int height = target.getHeight() - top - insets.bottom;
      int width = target.getWidth() - left - insets.right;

      int height2 = 1;
      int width2 = 1;

      int centerHeight = 1;
      int centerWidth = 1;

      int bottom = target.getHeight() + top;
      int right = target.getWidth() + left;

      Component c = null;

      // first determine the size of the center component
      if ((c = center) != null) {
        c.setSize(width, height);
        Dimension d = c.getPreferredSize();

        // determine the remaining width/height around the center component
        centerHeight = d.height;
        centerWidth = d.width;

        height2 = (height - centerHeight) / 2;
        width2 = (width - centerWidth) / 2;
      }
      else {
        // set the horizontalGap and verticalGap to half their size,
        // so that the remaining components have a correct gap between them
        if (verticalGap != 0) {
          verticalGap /= 2;
        }
        if (horizontalGap != 0) {
          horizontalGap /= 2;
        }
      }

      // set the sizes of each remaining component
      if ((c = north) != null) {
        c.setBounds(left, top, width, height2 - verticalGap);
        top += height2;
        height -= height2;
      }
      if ((c = south) != null) {
        c.setBounds(left, bottom - height2 + verticalGap, width, height2 - verticalGap);
        height -= height2;
      }
      if ((c = west) != null) {
        c.setBounds(left, top, width2 - horizontalGap, height);
        left += width2;
        width -= width2;
      }
      if ((c = east) != null) {
        c.setBounds(right - width2 + horizontalGap, top, width2 - horizontalGap, height);
        width -= width2;
      }

      // now set the size of the center component
      if ((c = center) != null) {
        c.setBounds(left, top, width, height);
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

      Component c = null;

      if ((c = east) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width + horizontalGap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = west) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width + horizontalGap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = center) != null) {
        Dimension d = c.getMinimumSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = north) != null) {
        Dimension d = c.getMinimumSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + verticalGap;
      }
      if ((c = south) != null) {
        Dimension d = c.getMinimumSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + verticalGap;
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

      Component c = null;

      if ((c = east) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width + horizontalGap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = west) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width + horizontalGap;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = center) != null) {
        Dimension d = c.getPreferredSize();
        dim.width += d.width;
        dim.height = Math.max(d.height, dim.height);
      }
      if ((c = north) != null) {
        Dimension d = c.getPreferredSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + verticalGap;
      }
      if ((c = south) != null) {
        Dimension d = c.getPreferredSize();
        dim.width = Math.max(d.width, dim.width);
        dim.height += d.height + verticalGap;
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
      else if (component == north) {
        north = null;
      }
      else if (component == south) {
        south = null;
      }
      else if (component == east) {
        east = null;
      }
      else if (component == west) {
        west = null;
      }
    }
  }

  /***********************************************************************************************
  Sets the <code>horizontalGap</code>
  @param horizontalGap the new <code>horizontalGap</code>
  ***********************************************************************************************/
  public void setHorizontalGap(int horizontalGap) {
    this.horizontalGap = horizontalGap;
  }

  /***********************************************************************************************
  Sets the <code>verticalGap</code>
  @param verticalGap the new <code>verticalGap</code>
  ***********************************************************************************************/
  public void setVerticalGap(int verticalGap) {
    this.verticalGap = verticalGap;
  }

  /***********************************************************************************************
  Gets a <code>String</code> representation of this <code>Object</code>
  @return a <code>String</code> representation of this <code>Object</code>
  ***********************************************************************************************/
  @Override
  public String toString() {
    return getClass().getName() + "[horizontalGap=" + horizontalGap + ",verticalGap=" + verticalGap + "]";
  }
}