/**
Copyright (c)  2016, California Institute of Technology ("Caltech"). U.S. Government
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
 * 
 */
package gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ParameterDirectionKind;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ParameterDirectionKindEnum;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.simulation.execution.session.SimulationSession;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.magicdraw.ui.browser.actions.DefaultBrowserAction;
import com.nomagic.magicdraw.uml.ConvertElementInfo;
import com.nomagic.magicdraw.uml.Refactoring;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallOperationAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.InputPin;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.OutputPin;
import com.nomagic.uml2.ext.magicdraw.actions.mdcompleteactions.StartObjectBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Operation;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Parameter;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.StructuredClassifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.impl.ElementsFactory;

/**
 * @author sherzig
 *
 */
public class ConvertActivityAction extends DefaultBrowserAction {

	public ConvertActivityAction() {
		super("", "(Convert Call Actions to Call Operation Actions)", null, null);
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		Tree tree = getTree();
		
		// Get the selected object
		Node node = tree.getSelectedNode();
		Object userObject = node.getUserObject();
		
		// Check whether the selected object is a SimulationConfiguration
		if (userObject instanceof Activity) {
			// Convert all call actions into call operation actions, except if they are already call operation actions, or have behavior associated with them that is non-empty
			
			// Otherwise create an activity (method) under the designated TYPE of the swimlane (if property, get type) - check for existence first, though
			// Also create an operation (with inputs and outputs), and assign method of op to be activity
			// Then create call operation action
			ThreadGroup tg = new ThreadGroup("MonteCarloRunGroupConversion"){
				
				/*
				 * Catch uncaught exception during thread run 
				 */
				public void uncaughtException(Thread t, Throwable e){
					if (SessionManager.getInstance().isSessionCreated()) {
						SessionManager.getInstance().closeSession();
					}
					
					if (e instanceof Exception) {
						Application.getInstance().getGUILog().log("MonteCarlo Simulation Plugin - Fatal Error: " + e.getMessage());
						
						for (StackTraceElement st : e.getStackTrace()) {
							Application.getInstance().getGUILog().log(st.toString());
						}
					}
				}
				
			};
			
			Runnable conversionThread = new Runnable() { 
	        	public void run() {
	        		SessionManager sessionManager = SessionManager.getInstance();
	        		sessionManager.createSession("Conversion Session");
	        		
					ConvertActivityAction.convertActivity((Activity) userObject);
					
					sessionManager.closeSession();
	        	}
			};
			
			// Start thread
			new Thread(tg, conversionThread).start();
		}
	}
	
	/**
	 * Recursive function to convert actions in activity to
	 * call operation actions.
	 * 
	 * @param act
	 */
	private static void convertActivity(Activity act) {
		// if activity containing nodes, convert each call action
		if (act.getNode() != null) {
			ArrayList<ActivityNode> nodes = new ArrayList<ActivityNode>();
			nodes.addAll(act.getNode());
			
			for (ActivityNode n : nodes) {
				// Skip opaque actions? That way we wont have calculation actions?
				if (n instanceof CallAction) {
					convertCallAction((CallAction) n);
				}
			}
		}
		// If activity without any nodes, then convert straight to call op action
	}
	
	private static void convertCallAction(CallAction a) {
		if (a instanceof CallBehaviorAction
				&& (((CallBehaviorAction) a).getBehavior() instanceof Activity)) {
			if (((Activity) ((CallBehaviorAction) a).getBehavior()).getNode() != null
					&& containsActionNodes(((Activity) ((CallBehaviorAction) a).getBehavior()).getNode())) {
				// if the action contains nodes, then go deeper
				convertActivity((Activity) ((CallBehaviorAction) a).getBehavior());
			}
			else {
				try {
					Operation op = createOperation(a, ((CallBehaviorAction) a).getBehavior());
					refactorCallAction(a, op);
				} catch (ReadOnlyElementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else if (!(a instanceof StartObjectBehaviorAction)
				&& !(a instanceof CallOperationAction)) {
			try {
				Operation op = createOperation(a, null);
				refactorCallAction(a, op);
			} catch (ReadOnlyElementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static boolean containsActionNodes(Collection<ActivityNode> nodes) {
		for (ActivityNode n : nodes)
			if (n instanceof CallAction)
				return true;
		
		return false;
	}
	
	private static Element refactorCallAction(CallAction a, Operation op) throws ReadOnlyElementException {
		if (a instanceof CallOperationAction || op == null)
			return null;
		
		// Converts the element to an interface.
		ConvertElementInfo info = new ConvertElementInfo(CallOperationAction.class);
		// Preserves the old element ID for the new element.
		info.setPreserveElementID(true);
		Element conversionTarget = Refactoring.Converting.convert(a, info);
		((CallOperationAction) conversionTarget).setOperation(op);
		
		ArrayList<InputPin> toRem = new ArrayList<InputPin>();
		
		for (InputPin p : ((CallOperationAction) conversionTarget).getInput())
			if (p.getName().equals("target")) {
				toRem.add(p);
				break;
			}
		
		ModelElementsManager.getInstance().removeElement(toRem.get(0));
		//((CallOperationAction) conversionTarget).getInput().removeAll(toRem);
		
		return conversionTarget;
	}
	
	private static Operation createOperation(CallAction a, Behavior opMethod) {
		Element owningElement = a.getContext();//a.getOwner().getOwner();		// Activity -> Class
		
		// Make sure that if we are deeply nested inside a state, we can still find an appropriate owner
		//while (owningElement.getOwner() != null
		//		&& !(owningElement instanceof StructuredClassifier))
		//	owningElement = owningElement.getOwner();
		
		if (a.getInPartition() != null && a.getInPartition().size() > 0) {
			ActivityPartition p = a.getInPartition().iterator().next();
			
			if (p.getRepresents() instanceof Classifier) {
				owningElement = p.getRepresents();
			}
			else if (p.getRepresents() instanceof Property) {
				owningElement = ((Property) p.getRepresents()).getType();
			}
		}
		
		ElementsFactory elementsFactory = Application.getInstance().getProjectsManager().getActiveProject().getElementsFactory();
		
		if (owningElement != null) {
			Operation op = elementsFactory.createOperationInstance();
			
			if (a.getName().contentEquals("") && a instanceof CallBehaviorAction)
				op.setName(((CallBehaviorAction) a).getBehavior().getName());
			else
				op.setName(a.getName());
			
			if (owningElement.canAdd(op)) {
				op.setOwner(owningElement);
				

				// Input params
				if (a.getInput() != null) {
					for (InputPin p : a.getInput()) {
						Parameter param = p.getParameter();
						
						if (param == null) {
							param = elementsFactory.createParameterInstance();
							
							if (p.getType() != null)
								param.setType(p.getType());
							
							param.setName(p.getName());
							param.setDirection(ParameterDirectionKindEnum.IN);
						}
						
						op.getOwnedParameter().add(param);
					}
				}
				
				// Ouput params
				if (a.getOutput() != null) {
					for (OutputPin p : a.getOutput()) {
						Parameter param = p.getParameter();
						
						if (param == null) {
							param = elementsFactory.createParameterInstance();
							
							if (p.getType() != null)
								param.setType(p.getType());
							
							param.setName(p.getName());
							param.setDirection(ParameterDirectionKindEnum.OUT);
						}
						
						op.getOwnedParameter().add(param);
					}
				}
				
				if (opMethod == null)
					opMethod = createOpMethod(a, owningElement);
				
				op.getMethod().add(opMethod);
				
				return op;
			}
			else {
				try {
					ModelElementsManager.getInstance().removeElement(op);
				} catch (ReadOnlyElementException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Application.getInstance().getGUILog().log("[ERROR] Failed to add operation \"" + op.getName() + "\" to " + owningElement.getHumanName());
			}
		}
		else
			Application.getInstance().getGUILog().log("[ERROR] Failed to convert \"" + a.getName() + "\" (context is null - owner is likely a package? See containing activity \"" + a.getActivity().getQualifiedName() + "\")");
		
		return null;
	}
	
	private static Behavior createOpMethod(CallAction a, Element owner) {
		ElementsFactory elementsFactory = Application.getInstance().getProjectsManager().getActiveProject().getElementsFactory();
		
		Activity newActivity = elementsFactory.createActivityInstance();
		newActivity.setName(a.getName());
		newActivity.setOwner(owner);
		
		return newActivity;
	}

}
