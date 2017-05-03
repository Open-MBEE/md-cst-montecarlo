/**
 * 
 * Copyright (c)  2016, California Institute of Technology ("Caltech"). U.S. Government
sponsorship acknowledged. All rights reserved.
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
package gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.simulation.execution.session.SimulationSession;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;

/**
 * @author sherzig
 *
 */
public class RunMonteCarloAction extends DefaultBrowserAction {

	private int numRuns = 30;
	
	public RunMonteCarloAction() {
		super("", "Run Monte Carlo", null, null);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		Tree tree = getTree();
		
		// Get the selected object
		Node node = tree.getSelectedNode();
		Object userObject = node.getUserObject();
		
		// Check whether the selected object is a SimulationConfiguration
		if (userObject instanceof Classifier
				&& StereotypesHelper.hasStereotype((Classifier) userObject, "SimulationConfig")) {
			String resp = JOptionPane.showInputDialog("Please enter the number of runs desired: ", this.numRuns+"");
			
			if (resp != null && !resp.equals("")) {
				try {
					int parsedRuns = Integer.parseInt(resp);
					
					if (parsedRuns < 0)
						JOptionPane.showMessageDialog(null, "Please enter a positive integer.");
					else {
						ThreadGroup tg = new ThreadGroup("MonteCarloRunGroup"){
							
							/*
							 * Catch uncaught exception during thread run 
							 */
							public void uncaughtException(Thread t, Throwable e){
								if(SessionManager.getInstance().isSessionCreated()) {
									SessionManager.getInstance().closeSession();
								}
								
								if(e instanceof Exception) {
									Application.getInstance().getGUILog().log("MonteCarlo Simulation Plugin - Fatal Error: " + e.getMessage());
									
									for(StackTraceElement st : e.getStackTrace()){
										Application.getInstance().getGUILog().log(st.toString());
									}
								}
							}
							
						};
						
						Runnable solverThread = new Runnable() { 
				        	public void run() {
								numRuns = parsedRuns;
								
								// If so, invoke Cameo Simulation Toolkit's execution engine in a loop
								for (int i=0; i<numRuns; i++) {
									//SessionManager.getInstance().createSession("Monte Carlo Run Session");
									SimulationSession session = com.nomagic.magicdraw.simulation.SimulationManager.execute((Classifier) userObject, true, true);
									
									System.out.println("Simulation starting...");
									
									while(!session.isClosed()) {     
										//System.out.println("Simulation not yet done...");
									    try {
											Thread.sleep(2000);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
									//SessionManager.getInstance().closeSession();
								}
						
				        	}
						};
						
						// Start thread
						new Thread(tg, solverThread).start();
					}
				} catch(NumberFormatException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Please enter a valid integer.");
				}
			}
		}
	}
	
}
