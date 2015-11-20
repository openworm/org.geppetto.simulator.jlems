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

import java.util.Map;

import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.typesystem.AspectNode;
import org.geppetto.core.model.typesystem.values.DoubleValue;
import org.geppetto.core.model.typesystem.values.QuantityValue;
import org.geppetto.core.model.typesystem.values.VariableValue;
import org.geppetto.core.model.typesystem.visitor.AnalysisVisitor;
import org.lemsml.jlems.api.ALEMSValue;
import org.lemsml.jlems.api.LEMSDoubleValue;
import org.lemsml.jlems.api.StateIdentifier;
import org.lemsml.jlems.api.interfaces.ILEMSResultsContainer;

/**
 * @author matteocantarelli
 * 
 *         This method updates the particles already present in the tree adding
 *         new values as found on the position pointer
 */
public class UpdateLEMSimulationTreeVisitor extends AnalysisVisitor {

	private ILEMSResultsContainer _lemsResults;
	private String _errorMessage = null;
	private Map<String, String> _geppettoToLems;
	private AspectNode _aspect;
	private boolean _modifiedSimulationTree = false;

	public UpdateLEMSimulationTreeVisitor(ILEMSResultsContainer lemsResults,
			AspectNode aspect, Map<String, String> geppettoToLems) {
		_lemsResults = lemsResults;
		_geppettoToLems = geppettoToLems;
		_aspect = aspect;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geppetto.core.model.state.visitors.DefaultStateVisitor#inAspectNode
	 * (org.geppetto.core.model.runtime.AspectNode)
	 */
	@Override
	public boolean inAspectNode(AspectNode node) {
		// we only visit the nodes which belong to the same aspect
		if (node.getId().equals(_aspect.getId())) {
			return super.inAspectNode(node);
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#
	 * outAspectSubTreeNode(org.geppetto.core.model.runtime.AspectSubTreeNode)
	 */
	@Override
	public boolean outAspectSubTreeNode(AspectSubTreeNode node) {
		if (node.getType().equals(AspectTreeType.SIMULATION_TREE)
				&& _modifiedSimulationTree) {
			node.setModified(true);
			_modifiedSimulationTree = false;
			AspectNode aspectNode = (AspectNode) node.getParent();
			aspectNode.setModified(true);
			((EntityNode) aspectNode.getParentEntity())
					.updateParentEntitiesFlags(true);
		}
		return super.outAspectSubTreeNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.geppetto.core.model.state.visitors.DefaultStateVisitor#visitVariableNode
	 * (org.geppetto.core.model.runtime.VariableNode)
	 */
	@Override
	public boolean visitVariableNode(VariableValue node) {
		if (node.isWatched()){
			if(node.getId().equals("time")){
				return super.visitVariableNode(node);
			}
			String lemsState = _geppettoToLems.get(node.getInstancePath()).replace(
					".", "/");
			StateIdentifier stateId = new StateIdentifier(lemsState);
			if (!_lemsResults.getStates().containsKey(stateId)) {
				_errorMessage = stateId + " not found in LEMS results:"
						+ _lemsResults.getStates();
			}
			ALEMSValue lemsValue = _lemsResults.getState(stateId).getLastValue();
			if (lemsValue instanceof LEMSDoubleValue) {
				QuantityValue quantity = new QuantityValue();
				quantity.setValue(new DoubleValue(((LEMSDoubleValue) lemsValue)
						.getAsDouble()));
				node.addQuantity(quantity);
				_modifiedSimulationTree = true;
			}
		}
		return super.visitVariableNode(node);
	}

	/**
	 * @return
	 */
	public String getError() {
		return _errorMessage;
	}
}
