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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.StateInstancePath;
import org.geppetto.core.model.StateSet;
import org.geppetto.core.model.values.AValue;
import org.geppetto.core.model.values.DoubleValue;
import org.geppetto.core.simulation.IRunConfiguration;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.lemsml.jlems.core.api.ALEMSValue;
import org.lemsml.jlems.core.api.LEMSBuildConfiguration;
import org.lemsml.jlems.core.api.LEMSBuildException;
import org.lemsml.jlems.core.api.LEMSBuildOptions;
import org.lemsml.jlems.core.api.LEMSBuildOptionsEnum;
import org.lemsml.jlems.core.api.LEMSBuilder;
import org.lemsml.jlems.core.api.LEMSDoubleValue;
import org.lemsml.jlems.core.api.LEMSExecutionException;
import org.lemsml.jlems.core.api.LEMSResultsContainer;
import org.lemsml.jlems.core.api.LEMSRunConfiguration;
import org.lemsml.jlems.core.api.LEMSSimulator;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildOptions;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuilder;
import org.lemsml.jlems.core.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.core.api.interfaces.ILEMSResultsContainer;
import org.lemsml.jlems.core.api.interfaces.ILEMSRunConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSSimulator;
import org.lemsml.jlems.core.api.interfaces.ILEMSStateInstance;
import org.lemsml.jlems.core.api.interfaces.IStateIdentifier;
import org.springframework.stereotype.Service;

/**
 * @author matteocantarelli
 * 
 */
@Service
public class JLEMSSimulatorService extends ASimulator
{

	private static Log logger = LogFactory.getLog(JLEMSSimulatorService.class);
	private ILEMSSimulator _simulator = null;

	/* (non-Javadoc)
	 * @see org.geppetto.core.simulator.ASimulator#initialize(org.geppetto.core.model.IModel, org.geppetto.core.simulation.ISimulatorCallbackListener)
	 */
	@Override
	public void initialize(IModel model, ISimulatorCallbackListener listener) throws GeppettoInitializationException
	{
		super.initialize(model, listener);
		try
		{
			ILEMSBuilder builder = new LEMSBuilder();

			builder.addDocument((ILEMSDocument) ((ModelWrapper) model).getModel("lems"));

			ILEMSBuildOptions options = new LEMSBuildOptions();
			options.addBuildOption(LEMSBuildOptionsEnum.FLATTEN);

			ILEMSBuildConfiguration config = new LEMSBuildConfiguration("net1");

			Collection<ILEMSStateInstance> stateInstances = builder.build(config, options);

			ILEMSRunConfiguration runConfig = new LEMSRunConfiguration(0.00005d, 0.08d);

//			IStateIdentifier tsince = new StateIdentifier("p1[0]/tsince");
//			IStateIdentifier p3v = new StateIdentifier("p3[0]/v");
//			IStateIdentifier hhpopv = new StateIdentifier("hhpop[0]/v");
//
//			runConfig.addStateRecord(new StateRecord(tsince));
//			runConfig.addStateRecord(new StateRecord(p3v));
//			runConfig.addStateRecord(new StateRecord(hhpopv));

			_simulator = new LEMSSimulator();
			for(ILEMSStateInstance instance : stateInstances)
			{
				_simulator.initialize(instance, runConfig);
			}
		}
		catch(LEMSBuildException e)
		{
			throw new GeppettoInitializationException(e);
		}
		catch(LEMSExecutionException e)
		{
			throw new GeppettoInitializationException(e);
		}
		logger.info("jLEMS Simulator initialized");
	}

	/* (non-Javadoc)
	 * @see org.geppetto.core.simulator.ISimulator#simulate(org.geppetto.core.simulation.IRunConfiguration)
	 */
	@Override
	public void simulate(IRunConfiguration runConfiguration) throws GeppettoExecutionException
	{
		ILEMSResultsContainer results = new LEMSResultsContainer();
		try
		{
			_simulator.run(results);
		}
		catch(LEMSExecutionException e)
		{
			throw new GeppettoExecutionException(e);
		}
		getListener().stateSetReady(getGeppettoStateSet(results));
	}

	/**
	 * @param results
	 * @return
	 */
	private StateSet getGeppettoStateSet(ILEMSResultsContainer results)
	{
		StateSet stateSet = new StateSet(_model.getId());
		for(IStateIdentifier state : results.getStates().keySet())
		{
			stateSet.addStateValues(new StateInstancePath(state.getStatePath()), getGeppettoValues(results.getStates().get(state)));
		}
		return stateSet;
	}

	/**
	 * @param lemsValues
	 * @return
	 */
	private List<AValue> getGeppettoValues(List<ALEMSValue> lemsValues)
	{
		List<AValue> geppettoValues = new ArrayList<AValue>();
		for(ALEMSValue value : lemsValues)
		{
			geppettoValues.add(new DoubleValue(((LEMSDoubleValue) value).getAsDouble()));
		}
		return geppettoValues;
	}

}
