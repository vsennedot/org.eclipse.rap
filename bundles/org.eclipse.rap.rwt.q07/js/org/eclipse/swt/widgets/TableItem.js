/*******************************************************************************
 * Copyright (c) 2007, 2009 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 ******************************************************************************/

/**
 * This class provides the client-side counterpart for
 * org.eclipse.swt.widgets.TableItem.
 */
qx.Class.define( "org.eclipse.swt.widgets.TableItem", {
  extend : qx.core.Object,

  construct : function( parent, index ) {
    this.base( arguments );
    this._parent = parent;
    this._cached = true;
    this._checked = false;
    this._grayed = false;
    this._texts = new Array();
    this._images = new Array();
    this._background = null;
    this._foreground = null;
    this._font = null;
    this._cellBackgrounds = null;
    this._cellForegrounds = null;
    this._cellFonts = null;
    // HACK: Table needs one 'emptyItem' (draws the remaining space that is not
    //       occupied by actual items) and a 'virtualItem' (represents a not
    //       yet resolved items)
    // Those have an index of -1
    if ( index >= 0 ) {
      parent._addItem( this, index );
    }
  },

  destruct : function() {
    // When changing this, re-check destructor of Table.js as well as TableLCA
    // and TableItemLCA
    // [if] The qx.core.Object.inGlobalDispose() is used to skip table rendering
    // on browser refresh. See bug:
    // 272686: [Table] Javascript error during table disposal
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=272686
    if( !this._parent.getDisposed() && !qx.core.Object.inGlobalDispose() ) {
      this._parent._removeItem( this );
    }
    org.eclipse.swt.WidgetManager.getInstance().remove( this );
  },

  statics : {

    PX : "px"

  },

  members : {

    getCached : function() {
      return this._cached;
    },

    setChecked : function( value ) {
      if( this._checked != value ) {
        this._checked = value;
      }
    },

    getChecked : function() {
      return this._checked;
    },

    setGrayed : function( value ) {
      if( this._grayed != value ) {
        this._grayed = value;
      }
    },

    getGrayed : function() {
      return this._grayed;
    },

    setSelection : function( value ) {
      // TODO [rh] improve this: don't access internal structures of Table
      var index = this._getIndex();
      if( value ) {
        this._parent._selectItem( index, false );
        // reset selection start index when selection changes server-side
        this._parent._resetSelectionStart();
      } else {
        this._parent._deselectItem( index, false );
      }
    },

    setTexts : function( texts ) {
      this._texts = texts;
    },

    setImages : function( images ) {
      this._images = images;
    },

    setBackground : function( background ) {
      this._background = background;
    },

    setForeground : function( foreground ) {
      this._foreground = foreground;
    },

    setFont : function( font ) {
      this._font = font;
    },

    setCellBackgrounds : function( backgrounds ) {
      this._cellBackgrounds = backgrounds;
    },

    setCellForegrounds : function( foregrounds ) {
      this._cellForegrounds = foregrounds;
    },

    setCellFonts : function( fonts ) {
      this._cellFonts = fonts;
    },

    update : function() {
      this._cached = true;
      this._parent.updateItem( this._getIndex(), true );
    },

    clear : function() {
      this._cached = false;
      this._checked = false;
      this._grayed = false;
      this._texts = new Array();
      this._images = new Array();
      this._background = null;
      this._foreground = null;
      this._font = null;
      this._cellBackgrounds = null;
      this._cellForegrounds = null;
      this._cellFonts = null;
    },

    /**
     * Called by Table when updating visible rows.
     */
    _render : function( row ) {
      var element = row.getElement();
      var parent = this._parent;
      var pos = 0;
      var left = 0;
      var width = 0;
      var columnCount = parent.getColumnCount();
      if( columnCount == 0 ) {
        columnCount = 1;
      }
      var leftOffset = 0;
      if( parent.hasCheckBoxes() ) {
        leftOffset = org.eclipse.swt.widgets.Table.CHECK_WIDTH;
      }
      // Row background color
      if( this._drawColors() && this._background != null ) {
        row.setBackgroundColor( this._background );
      } else {
        row.resetBackgroundColor();
      }
      for( var i = 0; i < columnCount; i++ ) {
        var text = "";
        var font = "";
        var foreground = "";
        var background = "";
        // Font
        if( this._cellFonts && this._cellFonts[ i ] ) {
          font = this._cellFonts[ i ];
        } else if( this._font != null ) {
          font = this._font;
        }
        // Foreground and background color
        if( this._drawColors() ) {
          if( this._cellForegrounds && this._cellForegrounds[ i ] ) {
            foreground = this._cellForegrounds[ i ];
          } else if( this._foreground != null ) {
            foreground = this._foreground;
          }
          if( this._cellBackgrounds && this._cellBackgrounds[ i ] ) {
            background = this._cellBackgrounds[ i ];
          }
        }
        // Draw image
        if( this._images && this._images[ i ] ) {
          left = parent.getItemImageLeft( i );
          width = parent.getItemImageWidth( i );
          var node = this._getChildNode( element, pos );
          pos++;
          this._renderImage( node, this._images[ i ], left, width, background );
        }
        // Draw text
        if( this._texts[ i ] !== undefined ) {
          text = this._texts[ i ];
        }
        left = parent.getItemTextLeft( i );
        width = parent.getItemTextWidth( i );
        var align = qx.constant.Layout.ALIGN_LEFT;
        var column = parent.getColumn( i );
        if( column ) {
          align = column.getHorizontalChildrenAlign();
        }
        var node = this._getChildNode( element, pos );
        pos++;
        this._renderText( node, text, left, width, align, font, foreground, background );
      }
      this._deleteRemainingChildNodes( element, pos );
    },

    _getChildNode : function( element, pos ) {
      var result;
      if( element.childNodes.length > pos ) {
        result = element.childNodes[ pos ];
      } else {
        result = document.createElement( "div" );
        element.appendChild( result );
      }
      return result;
    },

    _deleteRemainingChildNodes : function( element, start ) {
      for( var i = element.childNodes.length - 1; i >= start; i-- ) {
        element.removeChild( element.childNodes[ i ] );
      }
    },

    _drawColors : function() {
      var enabled = this._parent.getEnabled();
      var selected = this._parent._isItemSelected( this._getIndex() );
      return enabled && ( this._parent._hideSelection || !selected );
    },

    _renderImage : function( node, image, left, width, background ) {
      node.innerHTML = "";
      node.style.position = "absolute";
      node.style.overflow = "hidden";
      node.style.top = "0px";
      node.style.left = left + org.eclipse.swt.widgets.TableItem.PX;
      node.style.width = width + org.eclipse.swt.widgets.TableItem.PX;
      var height = this._parent.getItemHeight() + org.eclipse.swt.widgets.TableItem.PX;
      node.style.height = height;
      // set line height to enable vertical centering
      node.style.lineHeight = height;
      node.style.backgroundColor = background;
      node.style.backgroundImage = "url(" + image + ")";
      node.style.backgroundRepeat = "no-repeat";
      node.style.backgroundPosition = "center";
    },

    _renderText : function( node, text, left, width, align, font, foreground, background )
    {
      node.innerHTML = text;
      node.style.position = "absolute";
      node.style.overflow = "hidden";
      node.style.top = "0px";
      node.style.textAlign = align;
      node.style.verticalAlign = "middle";
      node.style.whiteSpace = "nowrap";
      node.style.left = left + org.eclipse.swt.widgets.TableItem.PX;
      node.style.width = width + org.eclipse.swt.widgets.TableItem.PX;
      var height = this._parent.getItemHeight() + org.eclipse.swt.widgets.TableItem.PX;
      node.style.height = height;
      // set line height to enable vertical centering
      node.style.lineHeight = height;
      if( font != "" ) {
        node.style.font = font;
      } else {
        // Resetting style.font causes errors in IE with any of these syntaxes:
        // node.style.font = null | undefined | "inherit" | "";
        node.style.fontFamily = "";
        node.style.fontSize = "";
        node.style.fontVariant = "";
        node.style.fontStyle = "";
        node.style.fontWeight = "";
      }
      node.style.color = foreground;
      node.style.backgroundColor = background;
      node.style.backgroundImage = "none";
    },

    _getIndex : function() {
      // TODO [rh] improve this: don't access internal structures of Table
      return this._parent._items.indexOf( this );
    }
  }
});
