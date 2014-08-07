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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AVisualObjectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.CylinderNode;
import org.geppetto.core.model.runtime.SphereNode;
import org.geppetto.core.model.runtime.TextMetadataNode;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.visualisation.model.Point;
import org.neuroml.model.BaseCell;
import org.neuroml.model.Cell;
import org.neuroml.model.IafCell;
import org.neuroml.model.Include;
import org.neuroml.model.Instance;
import org.neuroml.model.Location;
import org.neuroml.model.Member;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Population;
import org.neuroml.model.PopulationTypes;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.util.NeuroMLConverter;

/**
 * Helper class to populate visualization tree for neuroml models
 * 
 * @author  Jesus R. Martinez (jesus@metacell.us)
 *
 */
public class PopulateVisualTree
{
	private static final String GROUP_PROPERTY = "group";
	private Map<String, BaseCell> _discoveredCells = new HashMap<String, BaseCell>();
	private static final int MAX_ATTEMPTS = 3;

	/**
	 * @param componentId
	 * @param url
	 * @return
	 */
	private BaseCell retrieveNeuroMLCell(String componentId, URL url) throws Exception
	{
		if(_discoveredCells.containsKey(componentId))
		{
			return _discoveredCells.get(componentId);
		}
		NeuroMLConverter neuromlConverter = new NeuroMLConverter();
		boolean attemptConnection = true;
		String baseURL = url.getFile();
		if(url.getFile().endsWith("nml"))
		{
			baseURL = baseURL.substring(0, baseURL.lastIndexOf("/") + 1);
		}
		int attempts = 0;
		NeuroMLDocument neuromlDocument = null;
		while(attemptConnection)
		{
			try
			{
				attemptConnection = false;
				attempts++;
				URL componentURL = new URL(url.getProtocol() + "://" + url.getAuthority() + baseURL + componentId + ".nml");

				neuromlDocument = neuromlConverter.urlToNeuroML(componentURL);

				List<Cell> cells = neuromlDocument.getCell();
				if(cells != null)
				{
					for(Cell c : cells)
					{
						_discoveredCells.put(componentId, c);
						if(c.getId().equals(componentId))
						{
							return c;
						}
					}
				}
			}
			catch(MalformedURLException e)
			{
				throw e;
			}
			catch(UnmarshalException e)
			{
				if(e.getLinkedException() instanceof IOException)
				{
					if(attempts < MAX_ATTEMPTS)
					{
						attemptConnection = true;
					}
				}
			}
			catch(Exception e)
			{
				throw e;
			}
		}
		return null;
	}
	
	/**
	 * @param allSegments 
	 * @param list
	 * @param id 
	 * @return
	 */
	private CompositeNode getVisualObjectsFromListOfSegments(List<Segment> list, String id)
	{
		CompositeNode visualGroup = new CompositeNode();
		Map<String, Point3DWithDiam> distalPoints = new HashMap<String, Point3DWithDiam>();
		for(Segment s : list)
		{
			String idSegmentParent = null;
			Point3DWithDiam parentDistal = null;
			if(s.getParent() != null)
			{
				idSegmentParent = s.getParent().getSegment().toString();
			}
			if(distalPoints.containsKey(idSegmentParent))
			{
				parentDistal = distalPoints.get(idSegmentParent);
			}
			visualGroup.setName(idSegmentParent);
			visualGroup.addChild(getCylinderFromSegment(s, parentDistal));
			distalPoints.put(s.getId().toString(), s.getDistal());
		}	
		
		return visualGroup;
	}

	/**
	 * @param neuroml
	 * @return
	 */
	public void createNodesFromNeuroMLDocument(AspectSubTreeNode visualizationTree, NeuroMLDocument neuroml)
	{
		List<Morphology> morphologies = neuroml.getMorphology();
		if(morphologies != null)
		{
			for(Morphology m : morphologies)
			{
				visualizationTree.addChild(getVisualObjectsFromListOfSegments(m.getSegment(), m.getId()));
			}
		}
		List<Cell> cells = neuroml.getCell();
		if(cells != null)
		{
			for(Cell c : cells)
			{
				_discoveredCells.put(c.getId(), c);
				Morphology cellmorphology = c.getMorphology();
				createNodesFromMorphologyBySegmentGroup(visualizationTree,cellmorphology,c.getId());
			}
		}
		List<IafCell> iafCells = neuroml.getIafCell();
		if(iafCells != null)
		{
			for(IafCell iafCell : iafCells)
			{
				_discoveredCells.put(iafCell.getId(), iafCell);
			}
		}
	}

	/**
	 * @param c
	 * @param id 
	 * @return
	 */
	private AVisualObjectNode getEntityNodefromCell(BaseCell c, String id)
	{
		SphereNode sphere = new SphereNode(id);
		sphere.setRadius(1d);
		Point origin=new Point();
		origin.setX(0d);
		origin.setY(0d);
		origin.setZ(0d);
		sphere.setPosition(origin);
		sphere.setId(id);
		return sphere;
	}
	
	/**
	 * @param neuroml
	 * @param scene
	 * @param url
	 * @throws Exception
	 */
	public void createNodesFromNetwork(AspectSubTreeNode visualizationTree, NeuroMLDocument neuroml, URL url) throws Exception
	{
		Map<String, AVisualObjectNode> objects = new HashMap<String, AVisualObjectNode>();
		List<Network> networks = neuroml.getNetwork();

		for(Network n : networks)
		{
			CompositeNode networkNode = new CompositeNode(n.getId());
			
			for(Population p : n.getPopulation())
			{
				BaseCell cell = retrieveNeuroMLCell(p.getComponent(), url);

				if(p.getType() != null && p.getType().equals(PopulationTypes.POPULATION_LIST))
				{
					int i = 0;
					for(Instance instance : p.getInstance())
					{
						AVisualObjectNode e = getEntityNodefromCell(cell, p.getId());

						if(instance.getLocation() != null)
						{
							e.setPosition(getPoint(instance.getLocation()));
						}
						if(p.getInstance().size()>1)
						{
							e.setId(p.getId()+"["+i+"]");
						}
						else
						{
							e.setId(p.getId());
						}						
						objects.put(e.getId(), e);
						networkNode.addChild(e);
					}
					i++;

				}
				else
				{
					int size = p.getSize().intValue();

					for(int i = 0; i < size; i++)
					{
						// FIXME the position of the population within the network needs to be specified in neuroml
						AVisualObjectNode e = getEntityNodefromCell(cell, cell.getId());

						e.setId(e.getId() + "[" + i + "]");
						objects.put(e.getId(), e);
						networkNode.addChild(e);
					}
				}
			}
			
			visualizationTree.addChild(networkNode);
		}
	}
	
	/**
	 * @param somaGroup
	 * @param segmentGeometries
	 */
	private void createVisualModelForMacroGroup(SegmentGroup macroGroup, Map<String, List<AVisualObjectNode>> segmentGeometries, List<AVisualObjectNode> allSegments)
	{	
		//TODO: This method was part of previous visualizer but wasn't used, leaving here in case is needed
		
//		TextMetadataNode text = new TextMetadataNode();
//		text.setAdditionalProperty(GROUP_PROPERTY, macroGroup.getId());
//		visualModel.addChild(text);
//		
//		for(Include i : macroGroup.getInclude())
//		{
//			if(segmentGeometries.containsKey(i.getSegmentGroup()))
//			{
//				visualModel.getObjects().addAll(segmentGeometries.get(i.getSegmentGroup()));
//			}
//		}
//		for(Member m : macroGroup.getMember())
//		{
//			for(AVisualObjectNode g : allSegments)
//			{
//				if(g.getId().equals(m.getSegment().toString()))
//				{
//					visualModel.getObjects().add(g);
//					allSegments.remove(g);
//					break;
//				}
//			}
//		}
//		segmentGeometries.remove(macroGroup.getId());
//		return visualModel;
	}

	/**
	 * @param visualizationTree 
	 * @param list
	 * @return
	 */
	private void createNodesFromMorphologyBySegmentGroup(AspectSubTreeNode visualizationTree, Morphology cellmorphology, String cellId)
	{		
		CompositeNode cellNode = new CompositeNode(cellId);

		CompositeNode allSegments = getVisualObjectsFromListOfSegments(cellmorphology.getSegment(), cellmorphology.getId());
		
		Map<String, List<AVisualObjectNode>> segmentGeometries = new HashMap<String, List<AVisualObjectNode>>();

		if(!cellmorphology.getSegmentGroup().isEmpty())
		{
			Map<String, List<String>> subgroupsMap = new HashMap<String, List<String>>();
			for(SegmentGroup sg : cellmorphology.getSegmentGroup())
			{
				for(Include include : sg.getInclude())
				{
					// the map is <containedGroup,containerGroup>
					if(!subgroupsMap.containsKey(include.getSegmentGroup()))
					{
						subgroupsMap.put(include.getSegmentGroup(), new ArrayList<String>());
					}
					subgroupsMap.get(include.getSegmentGroup()).add(sg.getId());
				}
				if(!sg.getMember().isEmpty())
				{
					segmentGeometries.put(sg.getId(), getVisualObjectsForGroup(sg, allSegments));
				}
			}
			for(String sg : segmentGeometries.keySet())
			{
				for(AVisualObjectNode vo : segmentGeometries.get(sg))
				{
					TextMetadataNode text = new TextMetadataNode();
					text.setAdditionalProperty("segment_groups", getAllGroupsString(sg, subgroupsMap, ""));	
				}
			}

			// this adds all segment groups not contained in the macro groups if any
			for(String sgId : segmentGeometries.keySet())
			{								
				List<AVisualObjectNode> segments = segmentGeometries.get(sgId);
				
				cellNode.getChildren().addAll(segments);
			}

		}
		
		visualizationTree.addChild(cellNode);
	}

	/**
	 * @param targetSg
	 * @param subgroupsMap
	 * @param allGroupsStringp
	 * @return a semicolon separated string containing all the subgroups that contain a given subgroup
	 */
	private String getAllGroupsString(String targetSg, Map<String, List<String>> subgroupsMap, String allGroupsStringp)
	{
		if(subgroupsMap.containsKey(targetSg))
		{
			StringBuilder allGroupsString = new StringBuilder(allGroupsStringp);
			for(String containerGroup : subgroupsMap.get(targetSg))
			{
				allGroupsString.append(containerGroup + "; ");
				allGroupsString.append(getAllGroupsString(containerGroup, subgroupsMap, ""));
			}
			return allGroupsString.toString();
		}
		return allGroupsStringp.trim();
	}

	/**
	 * @param cellId
	 * @param segmentGroupId
	 * @return
	 */
	private String getGroupId(String cellId, String segmentGroupId)
	{
		return cellId + "." + segmentGroupId;
	}


	/**
	 * @param sg
	 * @param allSegments
	 * @return
	 */
	private List<AVisualObjectNode> getVisualObjectsForGroup(SegmentGroup sg, CompositeNode allSegments)
	{
		List<AVisualObjectNode> geometries = new ArrayList<AVisualObjectNode>();
		for(Member m : sg.getMember())
		{
			List<ANode> segments = allSegments.getChildren();
			
			for(ANode g : segments )
			{
				if(((AVisualObjectNode) g).getId().equals(m.getSegment().toString()))
				{
					geometries.add((AVisualObjectNode) g);
				}
			}
		}
		return geometries;
	}

	
	/**
	 * @param p1
	 * @param p2
	 * @return
	 */
	private boolean samePoint(Point3DWithDiam p1, Point3DWithDiam p2)
	{
		return p1.getX() == p2.getX() && p1.getY() == p2.getY() && p1.getZ() == p2.getZ() && p1.getDiameter() == p2.getDiameter();
	}

	/**
	 * @param s
	 * @param parentDistal
	 * @return
	 */
	private AVisualObjectNode getCylinderFromSegment(Segment s, Point3DWithDiam parentDistal)
	{

		Point3DWithDiam proximal = s.getProximal() == null ? parentDistal : s.getProximal();
		Point3DWithDiam distal = s.getDistal();

		if(samePoint(proximal, distal)) // ideally an equals but the objects
										// are generated. hassle postponed.
		{
			SphereNode sphere = new SphereNode(s.getName());
			sphere.setRadius(proximal.getDiameter() / 2);
			sphere.setPosition(getPoint(proximal));
			sphere.setId(s.getId().toString());
			return sphere;
		}
		else
		{
			CylinderNode cyl = new CylinderNode(s.getName());
			cyl.setId(s.getId().toString());
			if(proximal != null)
			{
				cyl.setPosition(getPoint(proximal));
				cyl.setRadiusBottom(proximal.getDiameter() / 2);
			}

			if(distal != null)
			{
				cyl.setRadiusTop(s.getDistal().getDiameter() / 2);
				cyl.setDistal(getPoint(distal));
				cyl.setHeight(0d);
			}
			return cyl;
		}

	}

	/**
	 * @param distal
	 * @return
	 */
	private Point getPoint(Point3DWithDiam distal)
	{
		Point point = new Point();
		point.setX(distal.getX());
		point.setY(distal.getY());
		point.setZ(distal.getZ());
		return point;
	}

	/**
	 * @param location
	 * @return
	 */
	private Point getPoint(Location location)
	{
		Point point = new Point();
		point.setX(location.getX().doubleValue());
		point.setY(location.getY().doubleValue());
		point.setZ(location.getZ().doubleValue());
		return point;
	}
}
