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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.beans.SimulatorConfig;
import org.geppetto.core.common.ArrayUtils;
import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.data.model.AVariable;
import org.geppetto.core.data.model.ArrayVariable;
import org.geppetto.core.data.model.SimpleType;
import org.geppetto.core.data.model.SimpleType.Type;
import org.geppetto.core.data.model.StructuredType;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.data.DataModelFactory;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AspectTreeNode;
import org.geppetto.core.model.runtime.CompositeVariableNode;
import org.geppetto.core.model.runtime.StateVariableNode;
import org.geppetto.core.model.runtime.AspectTreeNode.ASPECTTREE;
import org.geppetto.core.model.values.ValuesFactory;
import org.geppetto.core.simulation.IRunConfiguration;
import org.geppetto.core.simulation.ISimulatorCallbackListener;
import org.geppetto.core.simulator.ASimulator;
import org.lemsml.jlems.core.api.ALEMSValue;
import org.lemsml.jlems.core.api.LEMSBuildConfiguration;
import org.lemsml.jlems.core.api.LEMSBuildException;
import org.lemsml.jlems.core.api.LEMSBuildOptions;
import org.lemsml.jlems.core.api.LEMSBuildOptionsEnum;
import org.lemsml.jlems.core.api.LEMSBuilder;
import org.lemsml.jlems.core.api.LEMSDocumentReader;
import org.lemsml.jlems.core.api.LEMSDoubleValue;
import org.lemsml.jlems.core.api.LEMSExecutionException;
import org.lemsml.jlems.core.api.LEMSResultsContainer;
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
import org.lemsml.jlems.core.api.interfaces.IStateRecord;
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
	private SimulatorConfig simulatorConfig;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ASimulator#initialize(org.geppetto.core.model.IModel, org.geppetto.core.simulation.ISimulatorCallbackListener)
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
			ILEMSDocument lemsDocument = (ILEMSDocument) ((ModelWrapper) models.get(0)).getModel("lems");

			builder.addDocument(lemsDocument);

			ILEMSBuildOptions options = new LEMSBuildOptions();
			options.addBuildOption(LEMSBuildOptionsEnum.FLATTEN);

			ILEMSBuildConfiguration config = new LEMSBuildConfiguration();
			builder.build(config, options); // pre-build to read the run configuration and target from the file

			_runConfig = LEMSDocumentReader.getLEMSRunConfiguration(lemsDocument);
			config = new LEMSBuildConfiguration(LEMSDocumentReader.getTarget(lemsDocument));
			Collection<ILEMSStateInstance> stateInstances = builder.build(config, options); // real build for our specific target

			_simulator = new LEMSSimulator();
			for(ILEMSStateInstance instance : stateInstances)
			{
				_simulator.initialize(instance, _runConfig);
			}
			setWatchableVariables();
			ILEMSResultsContainer results = new LEMSResultsContainer();
			getListener().stateTreeUpdated(populateStateTree(results));
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

	public ILEMSRunConfiguration getRunConfig()
	{
		return _runConfig;
	}

	public void setRunConfig(ILEMSRunConfiguration runConfig)
	{
		this._runConfig = runConfig;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.geppetto.core.simulator.ISimulator#simulate(org.geppetto.core.simulation.IRunConfiguration)
	 */
	@Override
	public void simulate(IRunConfiguration runConfiguration) throws GeppettoExecutionException
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

		populateStateTree(results);
		notifyStateTreeUpdated();
	}

	/**
	 * @param results
	 * @return
	 * @throws GeppettoExecutionException
	 */
	private AspectTreeNode populateStateTree(ILEMSResultsContainer results) throws GeppettoExecutionException
	{

		if(_stateTree == null)
		{
			// TODO Refactor simulators to deal with more than one model!
			_stateTree = new AspectTreeNode(_models.get(0).getId());
		}
		try
		{
			advanceTimeStep(_runConfig.getTimestep());
			if(isWatching())
			{
				ACompositeNode watchTree = _stateTree.getSubTree(ASPECTTREE.WATCH_TREE);
				if(watchTree.getChildren().isEmpty() || watchListModified())
				{
					watchListModified(false);
					for(IStateIdentifier state : results.getStates().keySet())
					{
						// for every state found in the results add a node in the tree
						String fullPath = _models.get(0).getInstancePath() + "." + state.getStatePath().replace("/", ".");
						if(getWatchList().contains(fullPath))
						{
							StringTokenizer tokenizer = new StringTokenizer(fullPath, ".");
							ACompositeNode node = watchTree;
							while(tokenizer.hasMoreElements())
							{
								String current = tokenizer.nextToken();
								boolean found = false;
								for(ANode child : node.getChildren())
								{
									if(child.getName().equals(current))
									{
										if(child instanceof ACompositeNode)
										{
											node = (ACompositeNode) child;
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
										CompositeVariableNode newNode = new CompositeVariableNode(current);
										node.addChild(newNode);
										node = newNode;
									}
									else
									{
										// it's a leaf node
										StateVariableNode newNode = new StateVariableNode(current);
										//commenting out until it's working
										/*
										Unit<? extends Quantity> unit = getUnitFromLEMSDimension(results.getStates().get(state).getDimension());
										newNode.setUnit(unit.toString());
										
										UnitConverter r = unit.getConverterTo(unit.getStandardUnit());

										long factor = 0; 
										if(r instanceof RationalConverter ){
											factor = ((RationalConverter) r).getDivisor();
										}
										
										newNode.setScalingFactor(_df.format(factor));
										*/
										ALEMSValue lemsValue = results.getStates().get(state).getLastValue();
										if(lemsValue instanceof LEMSDoubleValue)
										{
											newNode.addValue(ValuesFactory.getDoubleValue(((LEMSDoubleValue) lemsValue).getAsDouble()));
										}
										node.addChild(newNode);
									}
								}
							}
						}
					}
				}
				else
				{
					UpdateLEMSStateTreeVisitor updateStateTreeVisitor = new UpdateLEMSStateTreeVisitor(results, _models.get(0).getInstancePath());
					watchTree.apply(updateStateTreeVisitor);
					if(updateStateTreeVisitor.getError() != null)
					{
						throw new GeppettoExecutionException(updateStateTreeVisitor.getError());
					}
				}
			}
		}
		catch(Exception e)
		{
			throw new GeppettoExecutionException(e);
		}
		return _stateTree;
	}

	/**
	 * @param dimension
	 * @return
	 */
	public Unit<? extends Quantity> getUnitFromLEMSDimension(String dimension)
	{
		// the dimension string is a comma-separated list of dimension powers in the order
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
			resultingUnit = resultingUnit.times(getUnit(length,SI.METER));
		}
		float time = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(time != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(time,SI.SECOND));
		}
		float current = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(current != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(current,SI.AMPERE));
		}
		float temperature = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(temperature != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(temperature,SI.CELSIUS));
		}
		float amount = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(amount != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(amount,SI.MOLE));
		}
		float brightness = getDecimalNumber(Integer.parseInt(st.nextToken()));
		if(brightness != 0)
		{
			resultingUnit = resultingUnit.times(getUnit(brightness,SI.CANDELA));
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

	/**
	 * 
	 */
	public void setWatchableVariables()
	{

		SimpleType floatType = DataModelFactory.getSimpleType(Type.FLOAT);

		for(IStateRecord state : _runConfig.getRecordedStates())
		{
			List<AVariable> listToCheck = getWatchableVariables().getVariables();
			StringTokenizer stok = new StringTokenizer(state.getState().getStatePath(), "/");

			while(stok.hasMoreTokens())
			{
				String s = stok.nextToken();
				String searchVar = s;

				if(ArrayUtils.isArray(s))
				{
					searchVar = ArrayUtils.getArrayName(s);
				}

				AVariable v = ASimulator.getVariable(searchVar, listToCheck);

				if(v == null)
				{
					if(stok.hasMoreTokens())
					{
						StructuredType structuredType = new StructuredType();
						structuredType.setName(searchVar + "T");

						if(ArrayUtils.isArray(s))
						{
							v = DataModelFactory.getArrayVariable(searchVar, structuredType, ArrayUtils.getArrayIndex(s) + 1);
						}
						else
						{
							v = DataModelFactory.getSimpleVariable(searchVar, structuredType);
						}
						listToCheck.add(v);
						listToCheck = structuredType.getVariables();
					}
					else
					{
						if(ArrayUtils.isArray(s))
						{
							v = DataModelFactory.getArrayVariable(searchVar, floatType, ArrayUtils.getArrayIndex(s) + 1);
						}
						else
						{
							v = DataModelFactory.getSimpleVariable(searchVar, floatType);
						}
						listToCheck.add(v);
					}
				}
				else
				{
					if(stok.hasMoreTokens())
					{
						listToCheck = ((StructuredType) v.getType()).getVariables();
						if(ArrayUtils.isArray(s))
						{
							if(ArrayUtils.getArrayIndex(s) + 1 > ((ArrayVariable) v).getSize())
							{
								((ArrayVariable) v).setSize(ArrayUtils.getArrayIndex(s) + 1);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public String getName()
	{
		return this.simulatorConfig.getSimulatorName();
	}
}
