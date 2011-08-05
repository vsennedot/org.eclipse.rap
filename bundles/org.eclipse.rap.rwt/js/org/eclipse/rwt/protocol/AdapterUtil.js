/*******************************************************************************
 * Copyright (c) 2011 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/

namespace( "org.eclipse.rwt.protocol" );

org.eclipse.rwt.protocol.AdapterUtil = {
    
  callbackForTargetId : function( id, fun ) {
    var wm = org.eclipse.swt.WidgetManager.getInstance();
    if( id === null ) {
      fun( null );
    } else {
      var target = org.eclipse.swt.WidgetManager.getInstance().findWidgetById( id );
      if( target ) {
        fun( target );
      } else {
        wm.addRegistrationCallback( id, fun );
      }
    }
  }

};