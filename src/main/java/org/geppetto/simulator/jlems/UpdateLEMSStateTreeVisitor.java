/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011, 2013 OpenWorm.
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

import org.geppetto.core.model.state.StateVariableNode;
import org.geppetto.core.model.state.visitors.DefaultStateVisitor;
import org.geppetto.core.model.values.DoubleValue;
import org.lemsml.jlems.core.api.ALEMSValue;
import org.lemsml.jlems.core.api.LEMSDoubleValue;
import org.lemsml.jlems.core.api.StateIdentifier;
import org.lemsml.jlems.core.api.interfaces.ILEMSResultsContainer;

/**
 * @author matteocantarelli
 * 
 * This method updates the particles already present in the tree
 * adding new values as found on the position pointer
 */
public class UpdateLEMSStateTreeVisitor extends DefaultStateVisitor
{

	private ILEMSResultsContainer _lemsResults;
	private String _instancePath;
	private String _errorMessage=null;

	public UpdateLEMSStateTreeVisitor(ILEMSResultsContainer lemsResults,String instancePath)
	{
		_lemsResults=lemsResults;
		_instancePath=instancePath;
	}

	@Override
	public boolean visitStateVariableNode(StateVariableNode node)
	{
		String lemsState=node.getFullName().replace(_instancePath+".", "").replace(".", "/");
		StateIdentifier stateId=new StateIdentifier(lemsState);
		if(!_lemsResults.getStates().containsKey(stateId))
		{
			_errorMessage=stateId+" not found in LEMS results:"+_lemsResults.getStates();
		}
		ALEMSValue lemsValue=_lemsResults.getState(stateId).getLastValue();
		if(lemsValue instanceof LEMSDoubleValue)
		{
			node.addValue(new DoubleValue(((LEMSDoubleValue)lemsValue).getAsDouble()));
		}
		return super.visitStateVariableNode(node);
	}

	/**
	 * @return
	 */
	public String getError()
	{
		return _errorMessage;
	}
}
