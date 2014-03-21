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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.geppetto.core.common.GeppettoExecutionException;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModel;
import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.state.StateTreeRoot;
import org.geppetto.simulator.jlems.JLEMSSimulatorService;
import org.lemsml.jlems.core.api.LEMSDocumentReader;
import org.lemsml.jlems.core.api.LEMSRunConfiguration;
import org.lemsml.jlems.core.api.interfaces.ILEMSDocument;
import org.lemsml.jlems.core.api.interfaces.ILEMSDocumentReader;
import org.lemsml.jlems.core.api.interfaces.ILEMSRunConfiguration;
import org.lemsml.jlems.core.sim.ContentError;

import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;


/**
 * @author matteocantarelli
 * 
 */
public class GenerateHDF5Recording
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			URL url = new URL("https://raw.github.com/openworm/org.geppetto.samples/master/LEMS/SingleComponentHH/LEMS_NML2_Ex5_DetCell.xml");
			ModelWrapper lemsWrapper = lemsWrapper = new ModelWrapper(UUID.randomUUID().toString());
			String lemsString = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A").next();

			ILEMSDocumentReader lemsReader = new LEMSDocumentReader();
			ILEMSDocument document = lemsReader.readModel(url);
			lemsWrapper.wrapModel("lems", document);
			JLEMSSimulatorService simulator = new JLEMSSimulatorService();
			List<IModel> models = new ArrayList<IModel>();
			models.add(lemsWrapper);
			ILEMSRunConfiguration runConfig = new LEMSRunConfiguration(0.01, 0.3);
			TestListener listener=new TestListener();
			simulator.initialize(models, listener);
			int step = 0;
			while(step++ < 100)
			{
				simulator.simulate(null);
			}
			writeHDF5File("hdf5test.h5", "hhcell.electrical", listener.getTree());
		}
		catch(GeppettoInitializationException | GeppettoExecutionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(ContentError e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void writeHDF5File(String filename, String path, StateTreeRoot tree)
	{

//		try
//		{
//			NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(Version.netcdf4, filename);
//			StringTokenizer st=new StringTokenizer(path,".");
//			Group parent=null;
//			while(st.hasMoreTokens())
//			{
//				String node=st.nextToken();
//				parent= ncfile.addGroup(parent, node);	
//			}
//			
//			ncfile.close();
//		}
//		catch(IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		  // We are writing 2D data, a 6 x 12 grid.
	       final int NX = 6;
	       final int NY = 12;


	       // Create the file.
	       NetcdfFileWriteable dataFile = null;

	       try {
	           dataFile = NetcdfFileWriteable.createNew(filename, false);

	           // Create netCDF dimensions,
	            Dimension xDim = dataFile.addDimension("x", NX );
	            Dimension yDim = dataFile.addDimension("y", NY );

	            ArrayList dims =  new ArrayList();

	            // define dimensions
	            dims.add( xDim);
	            dims.add( yDim);


	           // Define a netCDF variable. The type of the variable in this case
	           // is ncInt (32-bit integer).
	           dataFile.addVariable("data", DataType.INT, dims);

	            // This is the data array we will write. It will just be filled
	            // with a progression of numbers for this example.
	           ArrayInt.D2 dataOut = new ArrayInt.D2( xDim.getLength(), yDim.getLength());

	           // Create some pretend data. If this wasn't an example program, we
	           // would have some real data to write, for example, model output.
	           int i,j;

	           for (i=0; i<xDim.getLength(); i++) {
	                for (j=0; j<yDim.getLength(); j++) {
	                    dataOut.set(i,j, i * NY + j);
	                }
	           }

	           // create the file
	           dataFile.create();


	           // Write the pretend data to the file. Although netCDF supports
	           // reading and writing subsets of data, in this case we write all
	           // the data in one operation.
	          dataFile.write("data", dataOut);


	       } catch (IOException e) {
	              e.printStackTrace();
	       } catch (InvalidRangeException e) {
	              e.printStackTrace();
	       } finally {
	            if (null != dataFile)
	            try {
	                dataFile.close();
	            } catch (IOException ioe) {
	                ioe.printStackTrace();
	            }
	       }

	}

}
