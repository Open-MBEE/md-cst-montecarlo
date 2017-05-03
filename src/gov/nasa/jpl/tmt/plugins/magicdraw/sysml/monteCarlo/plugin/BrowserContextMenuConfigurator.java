/**
 * 
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
 */
package gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.plugin;

import java.util.List;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.ui.browser.Tree;

import gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.actions.ConvertActivityAction;
import gov.nasa.jpl.tmt.plugins.magicdraw.sysml.monteCarlo.actions.RunMonteCarloAction;

/**
 * @author sherzig
 *
 */
public class BrowserContextMenuConfigurator implements BrowserContextAMConfigurator {

	private final MDAction mcAction = new RunMonteCarloAction();
	private final MDAction convertAction = new ConvertActivityAction();
	
	@Override
	public int getPriority() {
		return AMConfigurator.MEDIUM_PRIORITY;
	}

	@Override
	public void configure(ActionsManager mngr, Tree arg1) {
		ActionsCategory simCat = null;

		simCat = new ActionsCategory("Monte Carlo Tools", "Monte Carlo Tools");
		simCat.setNested(true);
		simCat.addAction(mcAction);
		simCat.addAction(convertAction);
		mngr.addCategory(simCat);
	}

}
