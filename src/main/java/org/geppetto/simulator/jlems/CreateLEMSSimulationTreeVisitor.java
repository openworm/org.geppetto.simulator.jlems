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

import java.util.StringTokenizer;

import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.typesystem.values.ACompositeValue;
import org.geppetto.core.model.typesystem.values.CompositeValue;
import org.geppetto.core.model.typesystem.values.QuantityValue;
import org.geppetto.core.model.typesystem.values.ValuesFactory;
import org.geppetto.core.model.typesystem.values.VariableValue;
import org.geppetto.core.model.typesystem.visitor.AnalysisVisitor;
import org.lemsml.jlems.api.ALEMSValue;
import org.lemsml.jlems.api.LEMSDoubleValue;
import org.lemsml.jlems.api.interfaces.ILEMSResultsContainer;
import org.lemsml.jlems.api.interfaces.IStateIdentifier;

/**
 * @author Adrian Quintana (adrian.perez@ucl.ac.uk)
 * 
 *         This visitor creates a simulation tree in the runtime model.
 * 
 * 
 */
public class CreateLEMSSimulationTreeVisitor extends AnalysisVisitor
{

	private ILEMSResultsContainer _lemsResults;
	String _variablePath;
	AspectSubTreeNode _simulationTree;
	IStateIdentifier _state;

	public CreateLEMSSimulationTreeVisitor()
	{
		super();
	}

	public CreateLEMSSimulationTreeVisitor(ILEMSResultsContainer lemsResults, AspectSubTreeNode simulationTree, IStateIdentifier state, String variablePath)
	{
		super();
		this._lemsResults = lemsResults;
		this._simulationTree = simulationTree;
		this._state = state;
		this._variablePath = variablePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#inAspectNode (org.geppetto.core.model.runtime.AspectNode)
	 */
	@Override
	public boolean inCompositeNode(CompositeValue node)
	{
		// we only visit the nodes which belong to the same aspect
		return super.inCompositeNode(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.model.state.visitors.DefaultStateVisitor#visitVariableNode (org.geppetto.core.model.runtime.VariableNode)
	 */
	@Override
	public boolean visitVariableNode(VariableValue node)
	{

		if(node.getInstancePath().equals(this._variablePath))
		{
			String post = this._variablePath.replace(this._simulationTree.getInstancePath(), "");
			StringTokenizer tokenizer = new StringTokenizer(post, ".");
			ACompositeValue currentNode = this._simulationTree;
			while(tokenizer.hasMoreElements())
			{
				String current = tokenizer.nextToken();
				boolean found = false;
				for(ANode child : currentNode.getChildren())
				{
					if(child.getId().equals(current))
					{
						if(child instanceof ACompositeValue)
						{
							currentNode = (ACompositeValue) child;
						}
						found = true;
						break;
					}
				}
				if(found)
				{
					continue;
				}
				else
				{
					if(tokenizer.hasMoreElements())
					{
						// not a leaf, create a composite state node
						CompositeValue newNode = new CompositeValue(current);
						newNode.setId(current);
						currentNode.addChild(newNode);
						currentNode = newNode;
					}
					else
					{
						// it's a leaf node
						VariableValue newNode = new VariableValue(current);
						newNode.setId(current);
						// commenting out until it's working
						/*
						 * Unit<? extends Quantity> unit = getUnitFromLEMSDimension (results.getStates ().get(state).getDimension()); newNode.setUnit(unit.toString());
						 * 
						 * UnitConverter r = unit.getConverterTo(unit .getStandardUnit());
						 * 
						 * long factor = 0; if(r instanceof RationalConverter ){ factor = ((RationalConverter) r).getDivisor(); }
						 * 
						 * newNode.setScalingFactor(_df.format(factor ));
						 */
						ALEMSValue lemsValue = this._lemsResults.getStates().get(this._state).getLastValue();
						if(lemsValue instanceof LEMSDoubleValue)
						{
							QuantityValue quantity = new QuantityValue();
							LEMSDoubleValue db = (LEMSDoubleValue) lemsValue;

							quantity.setValue(ValuesFactory.getDoubleValue(db.getAsDouble()));
							newNode.addQuantity(quantity);
						}
						currentNode.addChild(newNode);
					}
				}
			}
		}
		return super.visitVariableNode(node);
	}

}
