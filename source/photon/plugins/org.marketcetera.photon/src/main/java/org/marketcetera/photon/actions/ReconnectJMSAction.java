package org.marketcetera.photon.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.marketcetera.core.ClassVersion;
import org.marketcetera.photon.IImageKeys;
import org.marketcetera.photon.PhotonPlugin;

/**
 * RCP platform action responsible for initiating a reconnect of the 
 * application's connection to the JMS server.
 * 
 * @author gmiller
 * @see ReconnectJMSJob#schedule()
 */
@ClassVersion("$Id$")
public class ReconnectJMSAction extends Action implements IWorkbenchAction {

	public static final String ID = "org.marketcetera.photon.actions.ReconnectJMSAction";
	/**
	 * Create the default instance of ReconnectJMSAction, setting the ID, text,
	 * tool-tip text, and image to the defaults.
	 */
	public ReconnectJMSAction(IWorkbenchWindow window){
		setId(ID);
		setText("&Reconnect Message Connection");
		setToolTipText("Reconnect to the message server");
		setImageDescriptor(PhotonPlugin.getImageDescriptor(IImageKeys.RECONNECT_JMS_HISTORY));
	}
	/**
	 *  
	 * Default implementation does nothing.
	 * 
	 * @see org.eclipse.ui.actions.ActionFactory$IWorkbenchAction#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub
	}

	/**
	 * Attempt to reconnect to the JMS server.
	 * 
	 */
	public void run() {
		ReconnectJMSJob job = new ReconnectJMSJob("Reconnect message server");
		job.schedule();
	}

	
}