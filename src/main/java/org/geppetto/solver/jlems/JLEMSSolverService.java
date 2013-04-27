/**
 * 
 */
package org.geppetto.solver.jlems;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.simulation.ITimeConfiguration;
import org.geppetto.core.solver.ISolver;
import org.lemsml.jlems.core.api.LEMSBuildConfiguration;
import org.lemsml.jlems.core.api.LEMSBuildException;
import org.lemsml.jlems.core.api.LEMSBuildOptions;
import org.lemsml.jlems.core.api.LEMSBuildOptionsEnum;
import org.lemsml.jlems.core.api.LEMSBuilder;
import org.lemsml.jlems.core.api.LEMSExecutionException;
import org.lemsml.jlems.core.api.LEMSResultsContainer;
import org.lemsml.jlems.core.api.LEMSRunConfiguration;
import org.lemsml.jlems.core.api.LEMSSimulator;
import org.lemsml.jlems.core.api.StateIdentifier;
import org.lemsml.jlems.core.api.StateRecord;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuildOptions;
import org.lemsml.jlems.core.api.interfaces.ILEMSBuilder;
import org.lemsml.jlems.core.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.core.api.interfaces.ILEMSResultsContainer;
import org.lemsml.jlems.core.api.interfaces.ILEMSRunConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSSimulator;
import org.lemsml.jlems.core.api.interfaces.ILEMSStateInstance;
import org.lemsml.jlems.core.api.interfaces.IStateIdentifier;

/**
 * @author matteocantarelli
 * 
 */
public class JLEMSSolverService implements ISolver
{

	private static Log logger = LogFactory.getLog(JLEMSSolverService.class);
	private boolean _initialized=false;
	private ILEMSSimulator _simulator=null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openworm.simulationengine.core.solver.ISolver#solve(java.util.List, org.openworm.simulationengine.core.simulation.ITimeConfiguration)
	 */
	@Override
	public List<List<IModel>> solve(List<IModel> models, ITimeConfiguration timeConfiguration)
	{
		if(!_initialized)
		{
			init(models);
		}
		ILEMSResultsContainer results = new LEMSResultsContainer();
		try
		{
			_simulator.run(results);
		}
		catch(LEMSExecutionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	
	}

	private void init(List<IModel> models)
	{
		try
		{
			ILEMSBuilder builder = new LEMSBuilder();
			for(IModel model : models)
			{
				builder.addDocument((ILEMSDocument) ((ModelWrapper) model).getModel());
			}

			ILEMSBuildOptions options = new LEMSBuildOptions();
			options.addBuildOption(LEMSBuildOptionsEnum.FLATTEN);

			ILEMSBuildConfiguration config = new LEMSBuildConfiguration("net1");

			Collection<ILEMSStateInstance> stateInstances = builder.build(config, options);

			ILEMSRunConfiguration runConfig = new LEMSRunConfiguration(0.00005d, 0.08d);

			IStateIdentifier tsince = new StateIdentifier("p1[0]/tsince");
			IStateIdentifier p3v = new StateIdentifier("p3[0]/v");
			IStateIdentifier hhpopv = new StateIdentifier("hhpop[0]/v");

			runConfig.addStateRecord(new StateRecord(tsince));
			runConfig.addStateRecord(new StateRecord(p3v));
			runConfig.addStateRecord(new StateRecord(hhpopv));

			
			_simulator = new LEMSSimulator();
			for(ILEMSStateInstance instance : stateInstances)
			{
				_simulator.initialize(instance,runConfig);
			}
		}
		catch(LEMSBuildException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(LEMSExecutionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
