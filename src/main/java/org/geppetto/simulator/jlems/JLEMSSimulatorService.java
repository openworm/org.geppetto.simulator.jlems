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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.features.IVariableWatchFeature;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.services.GeppettoFeature;
import org.geppetto.core.services.ModelFormat;
import org.geppetto.core.services.registry.ServicesRegistry;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.geppetto.core.simulator.AVariableWatchFeature;
import org.geppetto.core.utilities.VariablePathSerializer;
import org.lemsml.jlems.api.LEMSBuildConfiguration;
import org.lemsml.jlems.api.LEMSBuildException;
import org.lemsml.jlems.api.LEMSBuildOptions;
import org.lemsml.jlems.api.LEMSBuildOptionsEnum;
import org.lemsml.jlems.api.LEMSBuilder;
import org.lemsml.jlems.api.LEMSDocumentReader;
import org.lemsml.jlems.api.LEMSExecutionException;
import org.lemsml.jlems.api.LEMSResultsContainer;
import org.lemsml.jlems.api.LEMSSimulator;
import org.lemsml.jlems.api.interfaces.ILEMSBuildConfiguration;
import org.lemsml.jlems.api.interfaces.ILEMSBuildOptions;
import org.lemsml.jlems.api.interfaces.ILEMSBuilder;
import org.lemsml.jlems.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.api.interfaces.ILEMSResultsContainer;
import org.lemsml.jlems.api.interfaces.ILEMSRunConfiguration;
import org.lemsml.jlems.api.interfaces.ILEMSSimulator;
import org.lemsml.jlems.api.interfaces.ILEMSStateInstance;
import org.lemsml.jlems.api.interfaces.IStateIdentifier;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author matteocantarelli
 * 
 */
@Service
public class JLEMSSimulatorService extends ASimulator
{

	private static Log _logger = LogFactory.getLog(JLEMSSimulatorService.class);
	private ILEMSSimulator _simulator = null;
	private ILEMSRunConfiguration _runConfig;
	private DecimalFormat _df = new DecimalFormat("0.E0");

	@Autowired
	private SimulatorConfig jlemsSimulatorConfig;

	private Map<String, String> _lemsToGeppetto = new HashMap<String, String>();
	private Map<String, String> _geppettoToLems = new HashMap<String, String>();
	private ILEMSDocument _lemsDocument = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ASimulator#initialize(org.geppetto.core.model .IModel, org.geppetto.core.simulation.ISimulatorCallbackListener)
	 */
	@Override
	public void initialize(List<IModel> models, ISimulatorCallbackListener listener) throws GeppettoInitializationException, GeppettoExecutionException
	{
		super.initialize(models, listener);
		setTimeStepUnit("s");
		try
		{
			ILEMSBuilder builder = new LEMSBuilder();
			// TODO Refactor simulators to deal with more than one model!
			_lemsDocument = (ILEMSDocument) ((ModelWrapper) models.get(0)).getModel(ServicesRegistry.getModelFormat("LEMS"));
			builder.addDocument(_lemsDocument);

			ILEMSBuildOptions options = new LEMSBuildOptions();
			options.addBuildOption(LEMSBuildOptionsEnum.FLATTEN);

			ILEMSBuildConfiguration config = new LEMSBuildConfiguration();
			builder.build(config, options); // pre-build to read the run
											// configuration and target from the
											// file

			_runConfig = LEMSDocumentReader.getLEMSRunConfiguration(_lemsDocument);
			config = new LEMSBuildConfiguration(LEMSDocumentReader.getTarget(_lemsDocument));
			Collection<ILEMSStateInstance> stateInstances = builder.build(config, options); // real build for our specific target

			_simulator = new LEMSSimulator();
			for(ILEMSStateInstance instance : stateInstances)
			{
				_simulator.initialize(instance, _runConfig);
			}


			if(this.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE) == null)
			{
				// add variable watch feature
				this.addFeature(new AVariableWatchFeature());
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
		catch(ContentError e)
		{
			throw new GeppettoInitializationException(e);
		}
		catch(ParseError e)
		{
			throw new GeppettoInitializationException(e);
		}
		_logger.info("jLEMS Simulator initialized");
	}

	/**
	 * @return
	 */
	public ILEMSRunConfiguration getRunConfig()
	{
		return _runConfig;
	}

	/**
	 * @param runConfig
	 */
	public void setRunConfig(ILEMSRunConfiguration runConfig)
	{
		this._runConfig = runConfig;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ISimulator#simulate(org.geppetto.core.simulation .IRunConfiguration)
	 */
	@Override
	public void simulate(IAspectConfiguration aspectConfiguration, AspectNode aspect) throws GeppettoExecutionException
	{
		ILEMSResultsContainer results = new LEMSResultsContainer();
		try
		{
			_simulator.advance(results);
		}
		catch(LEMSExecutionException e)
		{
			throw new GeppettoExecutionException(e);
		}

		updateSimulationTree(results, aspect);
		notifySimulatorHasStepped(aspect);
	}

	/**
	 * @param results
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private void updateSimulationTree(ILEMSResultsContainer results, AspectNode aspect) throws GeppettoExecutionException
	{
		IVariableWatchFeature watchFeature = ((IVariableWatchFeature) this.getFeature(GeppettoFeature.VARIABLE_WATCH_FEATURE));
		advanceTimeStep(_runConfig.getTimestep(), aspect);

		if(watchFeature.watchListModified())
		{
			for(IStateIdentifier state : results.getStates().keySet())
			{
				String statePath = state.getStatePath().replace("/", ".");

				AspectSubTreeNode simulationTree = getSimulationTreeFor(statePath, aspect.getSubTree(AspectTreeType.SIMULATION_TREE));
				simulationTree.setModified(true);
				AspectNode aspectNode = (AspectNode) simulationTree.getParent();
				aspectNode.setModified(true);
				((EntityNode) aspectNode.getParentEntity()).updateParentEntitiesFlags(true);

				// For every state found in the results add a node in the tree
				CreateLEMSSimulationTreeVisitor createLEMSSimulationTreeVisitor = new CreateLEMSSimulationTreeVisitor(results, simulationTree, state, _lemsToGeppetto.get(statePath));
				simulationTree.apply(createLEMSSimulationTreeVisitor);
				watchFeature.setWatchListModified(false);
			}
		}
		else
		{
			UpdateLEMSimulationTreeVisitor updateStateTreeVisitor = new UpdateLEMSimulationTreeVisitor(results, aspect, _geppettoToLems);
			aspect.getParent().apply(updateStateTreeVisitor);
			if(updateStateTreeVisitor.getError() != null)
			{
				throw new GeppettoExecutionException(updateStateTreeVisitor.getError());
			}
		}
	}

	/**
	 * @param statePath
	 * @param simulationTree
	 * @return
	 */
	private AspectSubTreeNode getSimulationTreeFor(String statePath, AspectSubTreeNode simulationTree)
	{
		StringTokenizer st = new StringTokenizer(statePath, ".");
		AspectNode parentAspect = (AspectNode) simulationTree.getParent();
		EntityNode parentEntity = (EntityNode) parentAspect.getParent();

		String pre = "";
		String nt1 = "", nt2 = "";
		while(st.hasMoreTokens())
		{
			if(nt1 == "") nt1 = st.nextToken();
			if(st.hasMoreTokens())
			{
				if(nt2 == "") nt2 = st.nextToken();
			}
			for(ANode e : parentEntity.getChildren())
			{
				if(e.getId().equals(nt1))
				{
					if(pre != "")
					{
						pre += ".";
					}
					pre += nt1;
					parentEntity = (EntityNode) e;
					nt1 = nt2;
					nt2 = "";
					break;
				}
				else if(e.getId().equals(VariablePathSerializer.getArrayName(nt1, nt2)) && isNumeric(nt2))
				{
					if(pre != "")
					{
						pre += ".";
					}
					pre += nt1 + "." + nt2;
					parentEntity = (EntityNode) e;
					nt1 = nt2 = "";
					break;
				}
			}
			for(AspectNode a : parentEntity.getAspects())
			{
				if(a.getId().equals(parentAspect.getId()))
				{
					String post = statePath.substring(statePath.indexOf(pre) + pre.length());
					if(post.charAt(0) == '.')
					{
						post = post.substring(1);
					}
					// We replace the pattern .digits. with [digits] as Geppetto doesn't support nodes that have numbers as names
					post = post.replaceAll("\\.(\\d*)\\.", "\\[$1\\]\\.");
					_lemsToGeppetto.put(statePath, a.getSubTree(AspectTreeType.SIMULATION_TREE).getInstancePath() + "." + post);
					_geppettoToLems.put(a.getSubTree(AspectTreeType.SIMULATION_TREE).getInstancePath() + "." + post, statePath);
					return a.getSubTree(AspectTreeType.SIMULATION_TREE);
				}
			}
			return null;
		}

		return null;
	}

	/**
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str)
	{
		try
		{
			double d = Integer.parseInt(str);
		}
		catch(NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}

	/**
	 * @param dimension
	 * @return
	 */
	public Unit<? extends Quantity> getUnitFromLEMSDimension(String dimension)
	{
		// the dimension string is a comma-separated list of dimension powers in
		// the order
		// mass, length, time, current, temperature, amount, brightness
		StringTokenizer st = new StringTokenizer(dimension, ",");

		Unit<? extends Quantity> resultingUnit = Unit.ONE;
		float mass = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(mass != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(mass, SI.GRAM));
		}
		float length = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(length != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(length, SI.METER));
		}
		float time = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(time != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(time, SI.SECOND));
		}
		float current = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(current != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(current, SI.AMPERE));
		}
		float temperature = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(temperature != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(temperature, SI.CELSIUS));
		}
		float amount = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(amount != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(amount, SI.MOLE));
		}
		float brightness = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(brightness != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(brightness, SI.CANDELA));
		}
		return resultingUnit;
	}

	/**
	 * @param noZeros
	 * @param unit
	 * @return
	 */
	private Unit<?> getUnit(Float scaling, Unit<?> unit)
	{
		switch(scaling.intValue())
		{
			case -12:
				return SI.PICO(unit);
			case -9:
				return SI.NANO(unit);
			case -6:
				return SI.MICRO(unit);
			case -3:
				return SI.MILLI(unit);
			case -2:
				return SI.CENTI(unit);
			case -1:
				return SI.DECI(unit);
			case 12:
				return SI.TERA(unit);
			case 6:
				return SI.MEGA(unit);
			case 3:
				return SI.KILO(unit);
			case 2:
				return SI.HECTO(unit);
			case 1:
				return unit;
			default:
				return unit.times(scaling);
		}
	}

	/**
	 * @param noZeros
	 * @return
	 */
	private float getDecimalNumber(int noZeros)
	{
		if(noZeros > 0)
		{
			char[] zeros = {};
			if(noZeros > 1)
			{
				zeros = new char[noZeros];
			}
			Arrays.fill(zeros, '0');
			return Float.parseFloat("1" + String.valueOf(zeros));
		}
		else if(noZeros < 0)
		{
			char[] zeros = new char[Math.abs(noZeros + 1)];
			Arrays.fill(zeros, '0');
			return Float.parseFloat("0." + String.valueOf(zeros) + "1");
		}
		else
		{
			return 0f;
		}
	}

	@Override
	public String getName()
	{
		return this.jlemsSimulatorConfig.getSimulatorName();
	}

	@Override
	public String getId()
	{
		return this.jlemsSimulatorConfig.getSimulatorID();
	}

	@Override
	public void registerGeppettoService()
	{
		List<ModelFormat> modelFormats = new ArrayList<ModelFormat>(Arrays.asList(ServicesRegistry.registerModelFormat("LEMS")));
		ServicesRegistry.registerSimulatorService(this, modelFormats);
	}
}
