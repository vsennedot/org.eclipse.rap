/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala - bug 187762
 *     Mohamed Tarief - tarief@eg.ibm.com - IBM - Bug 174481
 *******************************************************************************/
package org.eclipse.ui.dialogs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IContentProvider;
//import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
//import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
//import org.eclipse.swt.accessibility.ACC;
//import org.eclipse.swt.accessibility.AccessibleAdapter;
//import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
//import org.eclipse.swt.events.KeyAdapter;
//import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
//import org.eclipse.swt.events.SelectionAdapter;
//import org.eclipse.swt.events.SelectionEvent;
//import org.eclipse.swt.events.TraverseEvent;
//import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A simple control that provides a text widget and a tree viewer. The contents
 * of the text widget are used to drive a PatternFilter that is on the viewer.
 *
 * @see org.eclipse.ui.dialogs.PatternFilter
 * @since 1.0
 */
public class FilteredTree extends Composite {

	/**
	 * The filter text widget to be used by this tree. This value may be
	 * <code>null</code> if there is no filter widget, or if the controls have
	 * not yet been created.
	 */
	protected Text filterText;

	/**
	 * The control representing the clear button for the filter text entry. This
	 * value may be <code>null</code> if no such button exists, or if the
	 * controls have not yet been created.
	 * <p>
	 * <strong>Note:</strong> As of 3.5, this is not used if the new look is chosen.
	 * </p>
	 */
	protected ToolBarManager filterToolBar;

	/**
	 * The control representing the clear button for the filter text entry. This
	 * value may be <code>null</code> if no such button exists, or if the
	 * controls have not yet been created.
	 * <p>
	 * <strong>Note:</strong> This is only used if the new look is chosen.
	 * </p>
	 * 
	 * @since 3.5
	 */
	protected Control clearButtonControl;
	
	
	/**
	 * The viewer for the filtered tree. This value should never be
	 * <code>null</code> after the widget creation methods are complete.
	 */
	protected TreeViewer treeViewer;

	/**
	 * The Composite on which the filter controls are created. This is used to
	 * set the background color of the filter controls to match the surrounding
	 * controls.
	 */
	protected Composite filterComposite;

	/**
	 * The pattern filter for the tree. This value must not be <code>null</code>.
	 */
	private PatternFilter patternFilter;

	/**
	 * The text to initially show in the filter text control.
	 */
	protected String initialText = ""; //$NON-NLS-1$

	/**
	 * The job used to refresh the tree.
	 */
	private Job refreshJob;

	/**
	 * The parent composite of the filtered tree.
	 */
	protected Composite parent;

	/**
	 * Whether or not to show the filter controls (text and clear button). The
	 * default is to show these controls. This can be overridden by providing a
	 * setting in the product configuration file. The setting to add to not show
	 * these controls is:
	 * 
	 * org.eclipse.ui/SHOW_FILTERED_TEXTS=false
	 */
	protected boolean showFilterControls;

	protected Composite treeComposite;

	/**
	 * Tells whether to use the pre 3.5 or the new look.
	 * 
	 * @since 1.4
	 */
	private boolean useNewLook = false;

	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String CLEAR_ICON = "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for disabled clear button.
	 */
	private static final String DISABLED_CLEAR_ICON= "org.eclipse.ui.internal.dialogs.DCLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Maximum time spent expanding the tree after the filter text has been
	 * updated (this is only used if we were able to at least expand the visible
	 * nodes)
	 */
	private static final long SOFT_MAX_EXPAND_TIME = 200;

	/**
	 * Get image descriptors for the clear button.
	 */
// RAP [rh] JFaceResources is a session-singleton, hence its initialization must 	
//     happen in session scope. See also initializeimageDscriptors()
//	static {
//		ImageDescriptor descriptor = AbstractUIPlugin
//				.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
//						"$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
//		if (descriptor != null) {
//			JFaceResources.getImageRegistry().put(CLEAR_ICON, descriptor);
//		}
//		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
//				PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif"); //$NON-NLS-1$
//		if (descriptor != null) {
//			JFaceResources.getImageRegistry().put(DCLEAR_ICON, descriptor);
//		}
//	}

	/**
	 * Create a new instance of the receiver.
	 *
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @param treeStyle
	 *            the style bits for the <code>Tree</code>
	 * @param filter
	 *            the filter to be used
	 * 
	 * @deprecated As of 3.5, replaced by
	 *             {@link #FilteredTree(Composite, int, PatternFilter, boolean)} where using the new
	 *             look is encouraged
	 */
	public FilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, SWT.NONE);
		this.parent = parent;
// RAP [rh] initialize image descriptors in session scope  
		initializeImageDescriptors();
		init(treeStyle, filter);
	}

	/**
	 * Create a new instance of the receiver.
	 * 
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @param treeStyle
	 *            the style bits for the <code>Tree</code>
	 * @param filter
	 *            the filter to be used
	 * @param useNewLook
	 *            <code>true</code> if the new 3.5 look should be used
	 * @since 3.5
	 */
	public FilteredTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
		super(parent, SWT.NONE);
		this.parent = parent;
		this.useNewLook= useNewLook;
		// RAP [rh] initialize image descriptors in session scope  
		initializeImageDescriptors();
		init(treeStyle, filter);
	}

	/**
	 * Create a new instance of the receiver. Subclasses that wish to override
	 * the default creation behavior may use this constructor, but must ensure
	 * that the <code>init(composite, int, PatternFilter)</code> method is
	 * called in the overriding constructor.
	 *
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @see #init(int, PatternFilter)
	 * @deprecated As of 3.5, replaced by {@link #FilteredTree(Composite, boolean)} where using the
	 *             look is encouraged
	 */
	protected FilteredTree(Composite parent) {
		super(parent, SWT.NONE);
		this.parent = parent;
// RAP [rh] initialize image descriptors in session scope  
		initializeImageDescriptors();
	}
	
	/**
	 * Create a new instance of the receiver. Subclasses that wish to override
	 * the default creation behavior may use this constructor, but must ensure
	 * that the <code>init(composite, int, PatternFilter)</code> method is
	 * called in the overriding constructor.
	 * 
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @param useNewLook
	 *            <code>true</code> if the new 3.5 look should be used
	 * @see #init(int, PatternFilter)
	 * 
	 * @since 1.4
	 */
	protected FilteredTree(Composite parent, boolean useNewLook) {
		super(parent, SWT.NONE);
		this.parent = parent;
		this.useNewLook = useNewLook;
		// RAP [rh] initialize image descriptors in session scope  
		initializeImageDescriptors();
	}
	
// RAP [rh] initialize image descriptors in session scope  
	private void initializeImageDescriptors() {
	  ImageRegistry registry = JFaceResources.getImageRegistry();
    ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin( PlatformUI.PLUGIN_ID,
                                                                             "$nl$/icons/full/etool16/clear_co.gif" ); //$NON-NLS-1$
    if( descriptor != null && registry.getDescriptor( CLEAR_ICON ) == null ) {
      registry.put( CLEAR_ICON, descriptor );
    }
    descriptor = AbstractUIPlugin.imageDescriptorFromPlugin( PlatformUI.PLUGIN_ID,
                                                             "$nl$/icons/full/dtool16/clear_co.gif" ); //$NON-NLS-1$
    if( descriptor != null && registry.getDescriptor( DISABLED_CLEAR_ICON ) == null ) {
      registry.put( DISABLED_CLEAR_ICON, descriptor );
    }
  }
	
	/**
	 * Create the filtered tree.
	 *
	 * @param treeStyle
	 *            the style bits for the <code>Tree</code>
	 * @param filter
	 *            the filter to be used
	 */
	protected void init(int treeStyle, PatternFilter filter) {
		patternFilter = filter;
		showFilterControls = PlatformUI.getPreferenceStore().getBoolean(
				IWorkbenchPreferenceConstants.SHOW_FILTERED_TEXTS);
		createControl(parent, treeStyle);
		createRefreshJob();
		setInitialText(WorkbenchMessages.get().FilteredTree_FilterMessage);
		setFont(parent.getFont());

	}

	/**
	 * Create the filtered tree's controls. Subclasses should override.
	 *
	 * @param parent
	 * @param treeStyle
	 */
	protected void createControl(Composite parent, int treeStyle) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		if (parent.getLayout() instanceof GridLayout) {
		    setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		if (showFilterControls) {
			if (!useNewLook || useNativeSearchField(parent)) {
				filterComposite= new Composite(this, SWT.NONE);
			} else {
				filterComposite= new Composite(this, SWT.BORDER);
				filterComposite.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			}
			GridLayout filterLayout= new GridLayout(2, false);
			filterLayout.marginHeight= 0;
			filterLayout.marginWidth= 0;
			filterComposite.setLayout(filterLayout);
			filterComposite.setFont(parent.getFont());

			createFilterControls(filterComposite);
			filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING,
					true, false));
		}

		treeComposite = new Composite(this, SWT.NONE);
		GridLayout treeCompositeLayout = new GridLayout();
		treeCompositeLayout.marginHeight = 0;
		treeCompositeLayout.marginWidth = 0;
		treeComposite.setLayout(treeCompositeLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeComposite.setLayoutData(data);
		createTreeControl(treeComposite, treeStyle);
	}
	
	private static Boolean useNativeSearchField;
	
	private static boolean useNativeSearchField(Composite composite) {
		if (useNativeSearchField == null) {
			useNativeSearchField = Boolean.FALSE;
			Text testText = null;
			try {
				testText = new Text(composite, SWT.SEARCH | SWT.ICON_CANCEL);
// RAP [if] Remove comment when SWT.ICON_CANCEL is supported in the Web client
//				useNativeSearchField = new Boolean((testText.getStyle() & SWT.ICON_CANCEL) != 0);
			} finally {
				if (testText != null) {
					testText.dispose();
				}
			}
				
		}
		return useNativeSearchField.booleanValue();
	}
	

	/**
	 * Create the filter controls. By default, a text and corresponding tool bar
	 * button that clears the contents of the text is created. Subclasses may
	 * override.
	 *
	 * @param parent
	 *            parent <code>Composite</code> of the filter controls
	 * @return the <code>Composite</code> that contains the filter controls
	 */
	protected Composite createFilterControls(Composite parent) {
		createFilterText(parent);
		if (useNewLook)
			createClearTextNew(parent);
		else
			createClearTextOld(parent);
		if (clearButtonControl != null) {
			// initially there is no text to clear
			clearButtonControl.setVisible(false);
		}
		if (filterToolBar != null) {
			filterToolBar.update(false);
			// initially there is no text to clear
			filterToolBar.getControl().setVisible(false);
		}
		return parent;
	}

	/**
	 * Creates and set up the tree and tree viewer. This method calls
	 * {@link #doCreateTreeViewer(Composite, int)} to create the tree viewer.
	 * Subclasses should override {@link #doCreateTreeViewer(Composite, int)}
	 * instead of overriding this method.
	 *
	 * @param parent
	 *            parent <code>Composite</code>
	 * @param style
	 *            SWT style bits used to create the tree
	 * @return the tree
	 */
	protected Control createTreeControl(Composite parent, int style) {
		treeViewer = doCreateTreeViewer(parent, style);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeViewer.getControl().setLayoutData(data);
		treeViewer.getControl().addDisposeListener(new DisposeListener() {
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			public void widgetDisposed(DisposeEvent e) {
				refreshJob.cancel();
			}
		});
		if (treeViewer instanceof NotifyingTreeViewer) {
			patternFilter.setUseCache(true);
		}
		treeViewer.addFilter(patternFilter);
		return treeViewer.getControl();
	}

	/**
	 * Creates the tree viewer. Subclasses may override.
	 *
	 * @param parent
	 *            the parent composite
	 * @param style
	 *            SWT style bits used to create the tree viewer
	 * @return the tree viewer
	 */
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		return new NotifyingTreeViewer(parent, style);
	}

// RAP [rh] unused code; was used by traverse-listener
//	/**
//	 * Return the first item in the tree that matches the filter pattern.
//	 *
//	 * @param items
//	 * @return the first matching TreeItem
//	 */
//	private TreeItem getFirstMatchingItem(TreeItem[] items) {
//		for (int i = 0; i < items.length; i++) {
//			if (patternFilter.isLeafMatch(treeViewer, items[i].getData())
//					&& patternFilter.isElementSelectable(items[i].getData())) {
//				return items[i];
//			}
//			return getFirstMatchingItem(items[i].getItems());
//		}
//		return null;
//	}

	/**
	 * Create the refresh job for the receiver.
	 *
	 */
	private void createRefreshJob() {
		refreshJob = doCreateRefreshJob();
		refreshJob.setSystem(true);
	}

	/**
	 * Creates a workbench job that will refresh the tree based on the current filter text.
	 * Subclasses may override.
	 *
	 * @return a workbench job that can be scheduled to refresh the tree
	 */
	protected WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (treeViewer.getControl().isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				String text = getFilterString();
				if (text == null) {
					return Status.OK_STATUS;
				}

				boolean initial = initialText != null
						&& initialText.equals(text);
				if (initial) {
					patternFilter.setPattern(null);
				} else if (text != null) {
					patternFilter.setPattern(text);
				}

				Control redrawFalseControl = treeComposite != null ? treeComposite
						: treeViewer.getControl();
				try {
					// don't want the user to see updates that will be made to
					// the tree
					// we are setting redraw(false) on the composite to avoid
					// dancing scrollbar
					redrawFalseControl.setRedraw(false);
// RAP [if] workaround for bug 262504
//					if (!narrowingDown) {
					if (!narrowingDown && text.length() > 0) {
						// collapse all
						TreeItem[] is = treeViewer.getTree().getItems();
						for (int i = 0; i < is.length; i++) {
							TreeItem item = is[i];
							if (item.getExpanded()) {
								treeViewer.setExpandedState(item.getData(),
										false);
							}
						}
					}
					treeViewer.refresh(true);

					if (text.length() > 0 && !initial) {
						/*
						 * Expand elements one at a time. After each is
						 * expanded, check to see if the filter text has been
						 * modified. If it has, then cancel the refresh job so
						 * the user doesn't have to endure expansion of all the
						 * nodes.
						 */
						TreeItem[] items = getViewer().getTree().getItems();
						int treeHeight = getViewer().getTree().getBounds().height;
						int numVisibleItems = treeHeight
								/ getViewer().getTree().getItemHeight();
						long stopTime = SOFT_MAX_EXPAND_TIME
								+ System.currentTimeMillis();
						boolean cancel = false;
						if (items.length > 0
								&& recursiveExpand(items, monitor, stopTime,
										new int[] { numVisibleItems })) {
							cancel = true;
						}

						// enabled toolbar - there is text to clear
						// and the list is currently being filtered
						updateToolbar(true);
						
						if (cancel) {
							return Status.CANCEL_STATUS;
						}
					} else {
						// disabled toolbar - there is no text to clear
						// and the list is currently not filtered
						updateToolbar(false);
					}
				} finally {
					// done updating the tree - set redraw back to true
					TreeItem[] items = getViewer().getTree().getItems();
					if (items.length > 0
							&& getViewer().getTree().getSelectionCount() == 0) {
						treeViewer.getTree().setTopItem(items[0]);
					}
					redrawFalseControl.setRedraw(true);
				}
				return Status.OK_STATUS;
			}

			/**
			 * Returns true if the job should be canceled (because of timeout or
			 * actual cancellation).
			 *
			 * @param items
			 * @param monitor
			 * @param cancelTime
			 * @param numItemsLeft
			 * @return true if canceled
			 */
			private boolean recursiveExpand(TreeItem[] items,
					IProgressMonitor monitor, long cancelTime,
					int[] numItemsLeft) {
				boolean canceled = false;
				for (int i = 0; !canceled && i < items.length; i++) {
					TreeItem item = items[i];
					boolean visible = numItemsLeft[0]-- >= 0;
					if (monitor.isCanceled()
							|| (!visible && System.currentTimeMillis() > cancelTime)) {
						canceled = true;
					} else {
						Object itemData = item.getData();
						if (itemData != null) {
							if (!item.getExpanded()) {
								// do the expansion through the viewer so that
								// it can refresh children appropriately.
								treeViewer.setExpandedState(itemData, true);
							}
							TreeItem[] children = item.getItems();
							if (items.length > 0) {
								canceled = recursiveExpand(children, monitor,
										cancelTime, numItemsLeft);
							}
						}
					}
				}
				return canceled;
			}

		};
	}

	protected void updateToolbar(boolean visible) {
		if (clearButtonControl != null) {
			clearButtonControl.setVisible(visible);
		}
		if (filterToolBar != null) {
			filterToolBar.getControl().setVisible(visible);
		}
	}

	/**
	 * Creates the filter text and adds listeners. This method calls
	 * {@link #doCreateFilterText(Composite)} to create the text control.
	 * Subclasses should override {@link #doCreateFilterText(Composite)} instead
	 * of overriding this method.
	 *
	 * @param parent
	 *            <code>Composite</code> of the filter text
	 */
	protected void createFilterText(Composite parent) {
		filterText = doCreateFilterText(parent);
// RAP [rh] Accessibility API missing
//		filterText.getAccessible().addAccessibleListener(
//				new AccessibleAdapter() {
//					/*
//					 * (non-Javadoc)
//					 *
//					 * @see org.eclipse.swt.accessibility.AccessibleListener#getName(org.eclipse.swt.accessibility.AccessibleEvent)
//					 */
//					public void getName(AccessibleEvent e) {
//						String filterTextString = filterText.getText();
//						if (filterTextString.length() == 0
//								|| filterTextString.equals(initialText)) {
//							e.result = initialText;
//						} else {
//							e.result = NLS
//									.bind(
//											WorkbenchMessages.FilteredTree_AccessibleListenerFiltered,
//											new String[] {
//													filterTextString,
//													String
//															.valueOf(getFilteredItemsCount()) });
//						}
//					}
//
//					/**
//					 * Return the number of filtered items
//					 * @return int
//					 */
//					private int getFilteredItemsCount() {
//						int total = 0;
//						TreeItem[] items = getViewer().getTree().getItems();
//						for (int i = 0; i < items.length; i++) {
//							total += itemCount(items[i]);
//
//						}
//						return total;
//					}
//
//					/**
//					 * Return the count of treeItem and it's children to infinite depth.
//					 * @param treeItem
//					 * @return int
//					 */
//					private int itemCount(TreeItem treeItem) {
//						int count = 1;
//						TreeItem[] children = treeItem.getItems();
//						for (int i = 0; i < children.length; i++) {
//							count += itemCount(children[i]);
//
//						}
//						return count;
//					}
//				});

		filterText.addFocusListener(new FocusAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
				if (!useNewLook) {
					/*
					 * Running in an asyncExec because the selectAll() does not appear to work when
					 * using mouse to give focus to text.
					 */
					Display display= filterText.getDisplay();
					display.asyncExec(new Runnable() {
						public void run() {
							if (!filterText.isDisposed()) {
								if (getInitialText().equals(
										filterText.getText().trim())) {
									filterText.selectAll();
								}
							}
						}
					});
					return;
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
			 */

			public void focusLost(FocusEvent e) {
				if (!useNewLook) {
					return;
				}
				if (filterText.getText().equals(initialText)) {
					setFilterText(""); //$NON-NLS-1$
					textChanged();
				}
			}
		});



		if (useNewLook) {
			filterText.addMouseListener(new MouseAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
				 */
				public void mouseDown(MouseEvent e) {
					if (filterText.getText().equals(initialText)) {
						// XXX: We cannot call clearText() due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=260664
						setFilterText(""); //$NON-NLS-1$
						textChanged();
					}
				}
			});
		}

// RAP [rh] missing key yevents
//		filterText.addKeyListener(new KeyAdapter() {
//			/*
//			 * (non-Javadoc)
//			 *
//			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
//			 */
//			public void keyPressed(KeyEvent e) {
//				// on a CR we want to transfer focus to the list
//				boolean hasItems = getViewer().getTree().getItemCount() > 0;
//				if (hasItems && e.keyCode == SWT.ARROW_DOWN) {
//					treeViewer.getTree().setFocus();
//				} else if (e.character == SWT.CR) {
//					return;
//				}
//			}
//		});

		// enter key set focus to tree
// RAP [rh] Traverse events not supported
//		filterText.addTraverseListener(new TraverseListener() {
//			public void keyTraversed(TraverseEvent e) {
//				if (e.detail == SWT.TRAVERSE_RETURN) {
//					e.doit = false;
//					if (getViewer().getTree().getItemCount() == 0) {
//						Display.getCurrent().beep();
//					} else {
//						// if the initial filter text hasn't changed, do not try
//						// to match
//						boolean hasFocus = getViewer().getTree().setFocus();
//						boolean textChanged = !getInitialText().equals(
//								filterText.getText().trim());
//						if (hasFocus && textChanged
//								&& filterText.getText().trim().length() > 0) {
//							TreeItem item = getFirstMatchingItem(getViewer()
//									.getTree().getItems());
//							if (item != null) {
//								getViewer().getTree().setSelection(
//										new TreeItem[] { item });
//								ISelection sel = getViewer().getSelection();
//								getViewer().setSelection(sel, true);
//							}
//						}
//					}
//				}
//			}
//		});

		filterText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e) {
				textChanged();
			}
		});

		// if we're using a field with built in cancel we need to listen for
		// default selection changes (which tell us the cancel button has been
		// pressed)
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0) {
			filterText.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetDefaultSelected(SelectionEvent e) {
					if (e.detail == SWT.ICON_CANCEL)
						clearText();
				}
			});
		}

		GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		// if the text widget supported cancel then it will have it's own
		// integrated button. We can take all of the space.
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0)
			gridData.horizontalSpan = 2;
		filterText.setLayoutData(gridData);
	}

	/**
	 * Creates the text control for entering the filter text. Subclasses may
	 * override.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the text widget
	 */
	protected Text doCreateFilterText(Composite parent) {
		if (!useNewLook || useNativeSearchField(parent)) {
			return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH
					| SWT.ICON_CANCEL);
		}
		return new Text(parent, SWT.SINGLE);
	}

	private String previousFilterText;

	private boolean narrowingDown;

	/**
	 * Update the receiver after the text has changed.
	 */
	protected void textChanged() {
		narrowingDown = previousFilterText == null
				|| previousFilterText
						.equals(WorkbenchMessages.get().FilteredTree_FilterMessage)
				|| getFilterString().startsWith(previousFilterText);
		previousFilterText = getFilterString();
		// cancel currently running job first, to prevent unnecessary redraw
		refreshJob.cancel();
		refreshJob.schedule(getRefreshJobDelay());
	}
	
	/**
	 * Return the time delay that should be used when scheduling the
	 * filter refresh job.  Subclasses may override.
	 * 
	 * @return a time delay in milliseconds before the job should run
	 * 
	 * @since 1.4
	 */
	protected long getRefreshJobDelay() {
		return 200;
	}

	/**
	 * Set the background for the widgets that support the filter text area.
	 * 
	 * @param background
	 *            background <code>Color</code> to set
	 */
	public void setBackground(Color background) {
		super.setBackground(background);
		if (filterComposite != null && (!useNewLook || useNativeSearchField(filterComposite))) {
			filterComposite.setBackground(background);
		}
		if (filterToolBar != null && filterToolBar.getControl() != null) {
			filterToolBar.getControl().setBackground(background);
		}
	}

	/**
	 * Create the button that clears the text.
	 * 
	 * @param parent
	 *            parent <code>Composite</code> of toolbar button
	 */
	private void createClearTextOld(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively
		if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) {
			filterToolBar= new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
			filterToolBar.createControl(parent);

			IAction clearTextAction= new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.action.Action#run()
				 */
				public void run() {
					clearText();
				}
			};

			clearTextAction
					.setToolTipText(WorkbenchMessages.get().FilteredTree_ClearToolTip);
			clearTextAction.setImageDescriptor(JFaceResources
					.getImageRegistry().getDescriptor(CLEAR_ICON));
			clearTextAction.setDisabledImageDescriptor(JFaceResources
.getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON));

			filterToolBar.add(clearTextAction);
		}
	}

	/**
	 * Create the button that clears the text.
	 * 
	 * @param parent parent <code>Composite</code> of toolbar button
	 */
	private void createClearTextNew(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively
		if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) {
			final Image inactiveImage= JFaceResources.getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON).createImage();
			final Image activeImage= JFaceResources.getImageRegistry().getDescriptor(CLEAR_ICON).createImage();
			// RAP [bm] IMAGE_GRAY
//			final Image pressedImage= new Image(getDisplay(), activeImage, SWT.IMAGE_GRAY);
			final Image pressedImage= new Image(getDisplay(), activeImage, SWT.IMAGE_COPY);
			
			final Label clearButton= new Label(parent, SWT.NONE);
			clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			clearButton.setImage(inactiveImage);
			clearButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			clearButton.setToolTipText(WorkbenchMessages.get().FilteredTree_ClearToolTip);
			clearButton.addMouseListener(new MouseAdapter() {
				private MouseMoveListener fMoveListener;

				public void mouseDown(MouseEvent e) {
					clearButton.setImage(pressedImage);
					fMoveListener= new MouseMoveListener() {
						private boolean fMouseInButton= true;

						public void mouseMove(MouseEvent e) {
							boolean mouseInButton= isMouseInButton(e);
							if (mouseInButton != fMouseInButton) {
								fMouseInButton= mouseInButton;
								clearButton.setImage(mouseInButton ? pressedImage : inactiveImage);
							}
						}
					};
					// RAP [bm] mouse move listener
//					clearButton.addMouseMoveListener(fMoveListener);
				}

				public void mouseUp(MouseEvent e) {
					if (fMoveListener != null) {
						// RAP [bm] mouse move listener
//						clearButton.removeMouseMoveListener(fMoveListener);
						fMoveListener= null;
						boolean mouseInButton= isMouseInButton(e);
						clearButton.setImage(mouseInButton ? activeImage : inactiveImage);
						if (mouseInButton) {
							clearText();
							filterText.setFocus();
						}
					}
				}
				
				private boolean isMouseInButton(MouseEvent e) {
					Point buttonSize = clearButton.getSize();
					return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
				}
			});
			// RAP [bm] MouseTrackListener
//			clearButton.addMouseTrackListener(new MouseTrackListener() {
//				public void mouseEnter(MouseEvent e) {
//					clearButton.setImage(activeImage);
//				}
//
//				public void mouseExit(MouseEvent e) {
//					clearButton.setImage(inactiveImage);
//				}
//
//				public void mouseHover(MouseEvent e) {
//				}
//			});
			clearButton.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					inactiveImage.dispose();
					activeImage.dispose();
					pressedImage.dispose();
				}
			});
			// RAP [bm] Accessibility
//			clearButton.getAccessible().addAccessibleListener(
//				new AccessibleAdapter() {
//					public void getName(AccessibleEvent e) {
//						e.result= WorkbenchMessages.FilteredTree_AccessibleListenerClearButton;
//					}
//			});
//			clearButton.getAccessible().addAccessibleControlListener(
//				new AccessibleControlAdapter() {
//					public void getRole(AccessibleControlEvent e) {
//						e.detail= ACC.ROLE_PUSHBUTTON;
//					}
//			});
			this.clearButtonControl= clearButton;
		}
	}

	/**
	 * Clears the text in the filter text widget.
	 */
	protected void clearText() {
		setFilterText(""); //$NON-NLS-1$
		textChanged();
	}

	/**
	 * Set the text in the filter control.
	 * 
	 * @param string
	 */
	protected void setFilterText(String string) {
		if (filterText != null) {
			filterText.setText(string);
			selectAll();
		}
	}

	/**
	 * Returns the pattern filter used by this tree.
	 * 
	 * @return The pattern filter; never <code>null</code>.
	 */
	public final PatternFilter getPatternFilter() {
		return patternFilter;
	}

	/**
	 * Get the tree viewer of the receiver.
	 * 
	 * @return the tree viewer
	 */
	public TreeViewer getViewer() {
		return treeViewer;
	}

	/**
	 * Get the filter text for the receiver, if it was created. Otherwise return
	 * <code>null</code>.
	 * 
	 * @return the filter Text, or null if it was not created
	 */
	public Text getFilterControl() {
		return filterText;
	}

	/**
	 * Convenience method to return the text of the filter control. If the text
	 * widget is not created, then null is returned.
	 * 
	 * @return String in the text, or null if the text does not exist
	 */
	protected String getFilterString() {
		return filterText != null ? filterText.getText() : null;
	}

	/**
	 * Set the text that will be shown until the first focus. A default value is
	 * provided, so this method only need be called if overriding the default
	 * initial text is desired.
	 * 
	 * @param text
	 *            initial text to appear in text field
	 */
	public void setInitialText(String text) {
		initialText = text;
		if (useNewLook && filterText != null) {
			filterText.setMessage(text);
			if (filterText.isFocusControl()) {
				setFilterText(initialText);
				textChanged();
			} else {
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (!filterText.isDisposed() && filterText.isFocusControl()) {
							setFilterText(initialText);
							textChanged();
						}
					}
				});
			}
		} else {
			setFilterText(initialText);
			textChanged();
		}
	}

	/**
	 * Select all text in the filter text field.
	 * 
	 */
	protected void selectAll() {
		if (filterText != null) {
			filterText.selectAll();
		}
	}

	/**
	 * Get the initial text for the receiver.
	 * 
	 * @return String
	 */
	protected String getInitialText() {
		return initialText;
	}

	/**
	 * Return a bold font if the given element matches the given pattern.
	 * Clients can opt to call this method from a Viewer's label provider to get
	 * a bold font for which to highlight the given element in the tree.
	 * 
	 * @param element
	 *            element for which a match should be determined
	 * @param tree
	 *            FilteredTree in which the element resides
	 * @param filter
	 *            PatternFilter which determines a match
	 *
	 * @return bold font
	 */
	public static Font getBoldFont(Object element, FilteredTree tree,
			PatternFilter filter) {
		String filterText = tree.getFilterString();

		if (filterText == null) {
			return null;
		}

		// Do nothing if it's empty string
		String initialText = tree.getInitialText();
		if (!filterText.equals("") && !filterText.equals(initialText)) {//$NON-NLS-1$
			if (tree.getPatternFilter() != filter) {
				boolean initial= initialText != null
					&& initialText.equals(filterText);
				if (initial) {
					filter.setPattern(null);
				} else if (filterText != null) {
					filter.setPattern(filterText);
				}
			}
			if (filter.isElementVisible(tree.getViewer(), element)
					&& filter.isLeafMatch(tree.getViewer(), element)) {
				return JFaceResources.getFontRegistry().getBold(
						JFaceResources.DIALOG_FONT);
			}
		}
		return null;
	}

	/**
	 * Custom tree viewer subclass that clears the caches in patternFilter on
	 * any change to the tree. See bug 187200.
	 */
	class NotifyingTreeViewer extends TreeViewer {

		/**
		 * @param parent
		 * @param style
		 */
		public NotifyingTreeViewer(Composite parent, int style) {
			super(parent, style);
		}

		public void add(Object parentElementOrTreePath, Object childElement) {
			getPatternFilter().clearCaches();
			super.add(parentElementOrTreePath, childElement);
		}

		public void add(Object parentElementOrTreePath, Object[] childElements) {
			getPatternFilter().clearCaches();
			super.add(parentElementOrTreePath, childElements);
		}

		protected void inputChanged(Object input, Object oldInput) {
			getPatternFilter().clearCaches();
			super.inputChanged(input, oldInput);
		}

		public void insert(Object parentElementOrTreePath, Object element,
				int position) {
			getPatternFilter().clearCaches();
			super.insert(parentElementOrTreePath, element, position);
		}

		public void refresh() {
			getPatternFilter().clearCaches();
			super.refresh();
		}

		public void refresh(boolean updateLabels) {
			getPatternFilter().clearCaches();
			super.refresh(updateLabels);
		}

		public void refresh(Object element) {
			getPatternFilter().clearCaches();
			super.refresh(element);
		}

		public void refresh(Object element, boolean updateLabels) {
			getPatternFilter().clearCaches();
			super.refresh(element, updateLabels);
		}

		public void remove(Object elementsOrTreePaths) {
			getPatternFilter().clearCaches();
			super.remove(elementsOrTreePaths);
		}

		public void remove(Object parent, Object[] elements) {
			getPatternFilter().clearCaches();
			super.remove(parent, elements);
		}

		public void remove(Object[] elementsOrTreePaths) {
			getPatternFilter().clearCaches();
			super.remove(elementsOrTreePaths);
		}

		public void replace(Object parentElementOrTreePath, int index,
				Object element) {
			getPatternFilter().clearCaches();
			super.replace(parentElementOrTreePath, index, element);
		}

		public void setChildCount(Object elementOrTreePath, int count) {
			getPatternFilter().clearCaches();
			super.setChildCount(elementOrTreePath, count);
		}

		public void setContentProvider(IContentProvider provider) {
			getPatternFilter().clearCaches();
			super.setContentProvider(provider);
		}

		public void setHasChildren(Object elementOrTreePath, boolean hasChildren) {
			getPatternFilter().clearCaches();
			super.setHasChildren(elementOrTreePath, hasChildren);
		}

	}

}
