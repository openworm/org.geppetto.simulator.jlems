/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.simulator.jlems;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.VariableList;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.simulation.IRunConfiguration;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.neuroml.model.NeuroMLDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author matteocantarelli
 * 
 */
@Service
public class NeuroMLSimulatorService extends ASimulator {

	@Autowired
	private SimulatorConfig neuroMLSimulatorConfig;
	private static final String NEUROML_ID = "neuroml";
	private static final String URL_ID = "url";
	public static final String LEMS_ID = "lems";
	
	private Map<String, List<ANode>> visualizationNodes;
	
	// helper class for populating the visual tree of aspect node
	private PopulateVisualTreeVisitor populateVisualTree = new PopulateVisualTreeVisitor();

	@Override
	public void initialize(List<IModel> models,	ISimulatorCallbackListener listener) throws GeppettoInitializationException, GeppettoExecutionException {
		super.initialize(models, listener);
		advanceTimeStep(0);
		visualizationNodes = new HashMap<String, List<ANode>>();
	}

	@Override
	public void simulate(IRunConfiguration arg0, AspectNode aspect)
			throws GeppettoExecutionException {
		advanceTimeStep(0);
		advanceRecordings(aspect);
		notifyStateTreeUpdated();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geppetto.core.simulator.ISimulator#populateVisualTree(org.geppetto
	 * .core.model.runtime.AspectNode)
	 */
	@Override
	public boolean populateVisualTree(AspectNode aspectNode) throws ModelInterpreterException {

		AspectSubTreeNode visualizationTree = (AspectSubTreeNode) aspectNode.getSubTree(AspectTreeType.VISUALIZATION_TREE);

		IModel model = aspectNode.getModel();

		try {
			NeuroMLDocument neuroml = (NeuroMLDocument) ((ModelWrapper) model).getModel(NEUROML_ID);
			if (neuroml != null) {
				URL url = (URL) ((ModelWrapper) model).getModel(URL_ID);
				populateVisualTree.createNodesFromNeuroMLDocument(visualizationTree, neuroml, null, visualizationNodes);
				//If a cell is not part of a network or there is not a target component, add it to to the visualizationtree
				for (List<ANode> visualizationNodesItem : visualizationNodes.values()){
					visualizationTree.addChildren(visualizationNodesItem);
				}
				visualizationTree.setModified(true);
				aspectNode.setModified(true);
				((EntityNode) aspectNode.getParentEntity()).updateParentEntitiesFlags(true);
			}
		} catch (Exception e) {
			throw new ModelInterpreterException(e);
		}
		return true;
	}

	@Override
	public VariableList getForceableVariables() {
		return new VariableList();
	}

	@Override
	public VariableList getWatchableVariables() {
		return new VariableList();
	}

	public void addWatchVariables(List<String> variableNames) {
		super.addWatchVariables(variableNames);
	}

	@Override
	public void startWatch() {
		super.startWatch();
	}

	@Override
	public void stopWatch() {
		super.stopWatch();
	}

	@Override
	public void clearWatchVariables() {
		super.clearWatchVariables();
	}

	@Override
	public String getName() {
		return this.neuroMLSimulatorConfig.getSimulatorName();
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return "neuroMLSimulator";
	}

}
