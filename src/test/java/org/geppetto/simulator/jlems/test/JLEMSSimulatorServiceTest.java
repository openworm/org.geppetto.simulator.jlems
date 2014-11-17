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
package org.geppetto.simulator.jlems.test;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import junit.framework.Assert;

import org.geppetto.core.data.model.SimpleType;
import org.geppetto.core.data.model.SimpleVariable;
import org.geppetto.core.data.model.StructuredType;
import org.geppetto.core.data.model.VariableList;
import org.geppetto.core.model.data.DataModelFactory;
import org.geppetto.simulator.jlems.JLEMSSimulatorService;
import org.junit.Test;
import org.lemsml.jlems.api.LEMSRunConfiguration;
import org.lemsml.jlems.api.StateIdentifier;
import org.lemsml.jlems.api.StateRecord;
import org.lemsml.jlems.api.interfaces.ILEMSRunConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author matteocantarelli
 * 
 */
public class JLEMSSimulatorServiceTest
{

	ILEMSRunConfiguration _runConfig = new LEMSRunConfiguration(0.01, 0.3);

	public void setup() throws Exception
	{
		_runConfig.addStateRecord(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/naChans/na/m/q")));
		_runConfig.addStateRecord(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/naChans/na/h/q")));
		_runConfig.addStateRecord(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/kChans/k/n/q")));
		_runConfig.addStateRecord(new StateRecord(new StateIdentifier("hhpop[0]/v")));
		
		
		
		
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/spiking")));
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/debugVal")));
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/naChans/iDensity")));
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/kChans/iDensity")));
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/naChans/gDensity")));
		// _runConfig.getRecordedStates().add(new StateRecord(new StateIdentifier("hhpop[0]/bioPhys1/membraneProperties/kChans/gDensity")));
	}

	//FIXME: This test will require integrated tests since it would have to use the model interpreter
	//	@Test
	//	public void testWatchVariables() throws Exception
	//	{
	//		setup();
	//		JLEMSSimulatorService simulator = new JLEMSSimulatorService();
	//		
	//
	//		List<String> watchList=new ArrayList<String>();
	//		watchList.add("hhpop[0].bioPhys1.membraneProperties.naChans.na.m.q");
	//		watchList.add("hhpop[0].bioPhys1.membraneProperties.naChans.na.h.q");
	//		watchList.add("hhpop[0].bioPhys1.membraneProperties.kChans.k.n.q");
	//		watchList.add("hhpop[0].v");
	//		simulator.addWatchVariables(watchList);
	//		simulator.startWatch();
	//	}
	
	/**
	 * Test method for {@link org.geppetto.simulator.jlems.JLEMSSimulatorService#getWatchableVariables()}.
	 * @throws Exception 
	 */
	@Test
	public void testGetWatchableVariables() throws Exception
	{
		setup();
		JLEMSSimulatorService simulator = new JLEMSSimulatorService();
		simulator.setRunConfig(_runConfig);
		simulator.setWatchableVariables();
		
		SimpleType ft = DataModelFactory.getSimpleType(SimpleType.Type.FLOAT);

		SimpleVariable mq = DataModelFactory.getSimpleVariable("q", ft);
		SimpleVariable hq = DataModelFactory.getSimpleVariable("q", ft);
		StructuredType m = DataModelFactory.getStructuredType("mT");
		m.getVariables().add(mq);
		StructuredType h = DataModelFactory.getStructuredType("hT");
		h.getVariables().add(hq);
		SimpleVariable nq = DataModelFactory.getSimpleVariable("q", ft);
		StructuredType n = DataModelFactory.getStructuredType("nT");
		n.getVariables().add(nq);

		StructuredType na = DataModelFactory.getStructuredType("naT");
		na.getVariables().add(DataModelFactory.getSimpleVariable("m", m));
		na.getVariables().add(DataModelFactory.getSimpleVariable("h", h));

		StructuredType k = DataModelFactory.getStructuredType("kT");
		k.getVariables().add(DataModelFactory.getSimpleVariable("n", n));

		StructuredType naChans = DataModelFactory.getStructuredType("naChansT");
		naChans.getVariables().add(DataModelFactory.getSimpleVariable("na", na));

		StructuredType kChans = DataModelFactory.getStructuredType("kChansT");
		kChans.getVariables().add(DataModelFactory.getSimpleVariable("k", k));

		StructuredType membraneProperties = DataModelFactory.getStructuredType("membranePropertiesT");
		membraneProperties.getVariables().add(DataModelFactory.getSimpleVariable("naChans", naChans));
		membraneProperties.getVariables().add(DataModelFactory.getSimpleVariable("kChans", kChans));

		StructuredType bioPhys1 = DataModelFactory.getStructuredType("bioPhys1T");
		bioPhys1.getVariables().add(DataModelFactory.getSimpleVariable("membraneProperties", membraneProperties));

		StructuredType hhpop = DataModelFactory.getStructuredType("hhpopT");
		hhpop.getVariables().add(DataModelFactory.getSimpleVariable("bioPhys1", bioPhys1));
		hhpop.getVariables().add(DataModelFactory.getSimpleVariable("v", ft));

		VariableList expectedList = new VariableList();
		expectedList.getVariables().add(DataModelFactory.getArrayVariable("hhpop", hhpop, 1));

		ObjectMapper mapper = new ObjectMapper();

		Assert.assertEquals(mapper.writer().writeValueAsString(expectedList), mapper.writer().writeValueAsString(simulator.getWatchableVariables()));
	}
	
	@Test
	public void testGetUnitFromLEMSDimension()
	{
		//mass, length, time, current, temperature, amount, brightness
		JLEMSSimulatorService sim=new JLEMSSimulatorService();
		Unit<? extends Quantity> unit=sim.getUnitFromLEMSDimension("0,1,0,0,0,0,0");
		Assert.assertEquals("m",unit.toString());
		unit=sim.getUnitFromLEMSDimension("0,0,-3,0,0,0,0");
		Assert.assertEquals("ms",unit.toString());
		// 1 kg·m2·s-3·A-1
		//unit=sim.getUnitFromLEMSDimension("3,2,-3,-1,0,0,0");
		//Assert.assertEquals("V",unit.alternate("V"));
	}

}
