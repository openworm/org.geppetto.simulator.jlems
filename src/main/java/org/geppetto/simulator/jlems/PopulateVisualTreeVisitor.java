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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geppetto.core.model.ModelWrapper;
import org.geppetto.core.model.quantities.PhysicalQuantity;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ANode;
import org.geppetto.core.model.runtime.AVisualObjectNode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode;
import org.geppetto.core.model.runtime.AspectSubTreeNode.AspectTreeType;
import org.geppetto.core.model.runtime.CompositeNode;
import org.geppetto.core.model.runtime.CylinderNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.SphereNode;
import org.geppetto.core.model.runtime.VisualGroupElementNode;
import org.geppetto.core.model.runtime.VisualGroupNode;
import org.geppetto.core.model.values.FloatValue;
import org.geppetto.core.utilities.VariablePathSerializer;
import org.geppetto.core.visualisation.model.Point;
import org.neuroml.model.Base;
import org.neuroml.model.BaseCell;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
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

/**
 * Helper class to populate visualization tree for neuroml models
 * 
 * @author Jesus R. Martinez (jesus@metacell.us)
 * 
 */
public class PopulateVisualTreeVisitor
{
	private String type = "static";
	private String highSpectrum = "0XFF0000";
	private String lowSpectrum = "0XFFFF00";

	/**
	 * @param allSegments
	 * @param list
	 * @param list2 
	 * @param id
	 * @return
	 */
	private CompositeNode getVisualObjectsFromListOfSegments(List<Segment> segments, List<SegmentGroup> segmentsGroup, String id)
	{
		//Create map with segment ids, keeping track of groups they correspond to 
		HashMap<String, List<String>> segmentsMap = new HashMap<String, List<String>>();
		HashMap<String, List<String>> segmentsGroupsMap = new HashMap<String, List<String>>();

		//Get all the segment groups from morphology
		for(SegmentGroup g : segmentsGroup){
			
			//segment found
			String segmentGroupID = g.getId();
			
			//segment not in map, add with new list for groups
			if(!segmentsGroupsMap.containsKey(segmentGroupID)){
				List<String> includeGroups = new ArrayList<String>();
				segmentsGroupsMap.put(segmentGroupID,includeGroups);
			}
			
			//traverse through group segments finding segments inside
			for(Member i : g.getMember()){
				//segment found
				String segmentID = i.getSegment().toString();
				//segment not in map, add with new list for groups
				if(!segmentsMap.containsKey(segmentID)){
					List<String> groups = new ArrayList<String>();
					groups.add(g.getId());
					segmentsMap.put(segmentID,groups);
				}
				//segment in mpa, get list and replace with updated one for groups
				else{
					List<String> groups = segmentsMap.get(segmentID);
				    groups.add(g.getId());
					segmentsMap.replace(segmentID,groups);
				}
				
				List<String> groups = segmentsGroupsMap.get(segmentGroupID);
			    groups.add(segmentID);
				segmentsGroupsMap.replace(segmentGroupID,groups);
			}
			//traverse through group segments finding segments inside
			for(Include i : g.getInclude()){
				//segment found
				String sg = i.getSegmentGroup();
				//segment not in map, add with new list for groups
				if(segmentsGroupsMap.containsKey(sg)){
					List<String> segmentsMembers = segmentsGroupsMap.get(sg);
					for(String key : segmentsMembers){
						List<String> groups = segmentsMap.get(key);
					    groups.add(segmentGroupID);
						segmentsMap.replace(key,groups);
					}
				}
			}
		}
		
		CompositeNode groupNode = new CompositeNode(id);
		HashMap<String, Point3DWithDiam> distalPoints = new HashMap<String, Point3DWithDiam>();
		for(Segment s : segments)
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
			groupNode.setName(idSegmentParent);
			AVisualObjectNode cyl = getCylinderFromSegment(s, parentDistal);
			
			if(segmentsMap.containsKey(cyl.getId())){
				//get groups list for segment and put it in visual objects
				cyl.setGroupElementsMap(segmentsMap.get(cyl.getId()));
			}
			
			groupNode.addChild(cyl);
			distalPoints.put(s.getId().toString(), s.getDistal());			
		}

		return groupNode;
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
				ANode node = getVisualObjectsFromListOfSegments(m.getSegment(),m.getSegmentGroup(), m.getId());
				node.setParent(visualizationTree);
				visualizationTree.addChild(node);
			}
		}
		List<Cell> cells = neuroml.getCell();
		if(cells != null)
		{
			for(Cell c : cells)
			{
				createNodesFromMorphologyBySegmentGroup(visualizationTree, c, c.getId());				
			}
		}
	}

	/**
	 * @param c
	 * @param id
	 * @return
	 */
	private AVisualObjectNode getVisualObjectForCell(BaseCell c, String id)
	{
		SphereNode sphere = new SphereNode(id);
		sphere.setRadius(1d);
		Point origin = new Point();
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
		AspectNode aspect = (AspectNode) visualizationTree.getParent();
		List<Network> networks = neuroml.getNetwork();
		if(networks.size() == 1)
		{
			addNetworkTo(networks.get(0), visualizationTree, aspect);
		}
		else
		{
			for(Network n : networks)
			{
				CompositeNode networkNode = new CompositeNode(n.getId(), n.getId());
				addNetworkTo(networks.get(0), networkNode, aspect);
			}
		}
	}

	/**
	 * @param n
	 * @param composite
	 * @param aspect
	 */
	private void addNetworkTo(Network n, ACompositeNode composite, AspectNode aspect)
	{
		for(Population p : n.getPopulation())
		{
			ModelWrapper model = (ModelWrapper) aspect.getModel();
			// the components have already been read by the model interpreter and stored inside a map in the ModelWrapper
			BaseCell cell = getNeuroMLComponent(p.getComponent(), model);

			if(p.getType() != null && p.getType().equals(PopulationTypes.POPULATION_LIST))
			{
				int i = 0;
				for(Instance instance : p.getInstance())
				{
					AVisualObjectNode visualObject = getVisualObjectForCell(cell, p.getId());

					if(instance.getLocation() != null)
					{
						visualObject.setPosition(getPoint(instance.getLocation()));
					}
					visualObject.setId(p.getId());
					addVisualObjectToVizTree(VariablePathSerializer.getArrayName(p.getId(), i), visualObject, composite, aspect, model);

				}
				i++;
			}
			else
			{
				int size = p.getSize().intValue();

				for(int i = 0; i < size; i++)
				{
					// FIXME the position of the population within the network needs to be specified in neuroml
					AVisualObjectNode visualObject = getVisualObjectForCell(cell, cell.getId());
					visualObject.setId(cell.getId());
					addVisualObjectToVizTree(VariablePathSerializer.getArrayName(p.getId(), i), visualObject, composite, aspect, model);
				}
			}
		}

	}

	/**
	 * @param componentId
	 * @param model
	 * @return
	 */
	private BaseCell getNeuroMLComponent(String componentId, ModelWrapper model)
	{
		HashMap<String, Base> discoveredComponents = (HashMap<String, Base>) model.getModel("discoveredComponents");
		if(discoveredComponents.containsKey(componentId))
		{
			return (BaseCell)discoveredComponents.get(componentId);
		}
		return null;
	}
	
	/**
	 * @param id
	 * @param visualObject
	 * @param composite
	 * @param aspect
	 * @param model
	 */
	private void addVisualObjectToVizTree(String id, AVisualObjectNode visualObject, ACompositeNode composite, AspectNode aspect, ModelWrapper model)
	{

		HashMap<String, EntityNode> entitiesMapping = (HashMap<String, EntityNode>) model.getModel("entitiesMapping");
		if(entitiesMapping.containsKey(id))
		{
			EntityNode e = entitiesMapping.get(id);
			for(AspectNode a : e.getAspects())
			{
				if(a.getId().equals(aspect.getId()))
				{
					// we are in the same aspect of the subentity, now we can fetch the visualization tree
					AspectSubTreeNode subEntityVizTree = a.getSubTree(AspectTreeType.VISUALIZATION_TREE);
					if(composite instanceof AspectSubTreeNode)
					{
						subEntityVizTree.addChild(visualObject);
					}
					else if(composite instanceof CompositeNode)
					{
						getCompositeNode(subEntityVizTree, composite.getId()).addChild(visualObject);
					}
				}
			}
		}
		else
		{
			composite.addChild(visualObject);
		}

	}

	/**
	 * @param subEntityVizTree
	 * @param compositeId
	 * @return
	 */
	private CompositeNode getCompositeNode(AspectSubTreeNode subEntityVizTree, String compositeId)
	{
		for(ANode child : subEntityVizTree.getChildren())
		{
			if(child.getId().equals(compositeId) && child instanceof CompositeNode)
			{
				return (CompositeNode) child;
			}
		}
		CompositeNode composite = new CompositeNode(compositeId, compositeId);
		subEntityVizTree.addChild(composite);
		return composite;
	}




	/**
	 * @param somaGroup
	 * @param segmentGeometries
	 */
	private void createVisualModelForMacroGroup(SegmentGroup macroGroup, HashMap<String, List<AVisualObjectNode>> segmentGeometries, List<AVisualObjectNode> allSegments)
	{
		// TODO: This method was part of previous visualizer but wasn't used, leaving here in case is needed

		// TextMetadataNode text = new TextMetadataNode();
		// text.setAdditionalProperty(GROUP_PROPERTY, macroGroup.getId());
		// visualModel.addChild(text);
		//
		// for(Include i : macroGroup.getInclude())
		// {
		// if(segmentGeometries.containsKey(i.getSegmentGroup()))
		// {
		// visualModel.getObjects().addAll(segmentGeometries.get(i.getSegmentGroup()));
		// }
		// }
		// for(Member m : macroGroup.getMember())
		// {
		// for(AVisualObjectNode g : allSegments)
		// {
		// if(g.getId().equals(m.getSegment().toString()))
		// {
		// visualModel.getObjects().add(g);
		// allSegments.remove(g);
		// break;
		// }
		// }
		// }
		// segmentGeometries.remove(macroGroup.getId());
		// return visualModel;
	}

	/**
	 * @param visualizationTree
	 * @param list
	 * @return
	 */
	private void createNodesFromMorphologyBySegmentGroup(AspectSubTreeNode visualizationTree, Cell cell, String cellId)
	{
		Morphology cellmorphology = cell.getMorphology();

		HashMap<String, VisualGroupNode> groupsMap = new HashMap<String,VisualGroupNode>();
		
		for(ChannelDensity density : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensity()){
			if(!groupsMap.containsKey(density.getIonChannel())){
				VisualGroupNode vis = new VisualGroupNode(density.getIonChannel());
				vis.setName(density.getSegmentGroup());
				vis.setType(type);
				vis.setHighSpectrumColor(highSpectrum);
				vis.setHighSpectrumColor(lowSpectrum);
				vis.setParent(visualizationTree);

				VisualGroupElementNode element = new VisualGroupElementNode(density.getId());

				String regExp = "\\s*([0-9-]*\\.?[0-9]*[eE]?[-+]?[0-9]+)?\\s*(\\w*)";
				Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(density.getCondDensity());
				if(matcher.find()){
					PhysicalQuantity physicalQuantity = new PhysicalQuantity();
					physicalQuantity.setValue(new FloatValue(Float.parseFloat(matcher.group(1))));
					physicalQuantity.setUnit(matcher.group(2));
					element.setParameter(physicalQuantity);
				}

				element.setParent(vis);
				element.setDefaultColor("0XFF3300");
				vis.getVisualGroupElements().add(element);

				visualizationTree.addChild(vis);
				groupsMap.put(density.getIonChannel(), vis);
			}
			else{
				VisualGroupNode vis = groupsMap.get(density.getIonChannel());
				
				VisualGroupElementNode element = new VisualGroupElementNode(density.getId());
				String regExp = "\\s*([0-9-]*\\.?[0-9]*[eE]?[-+]?[0-9]+)?\\s*(\\w*)";
				Pattern pattern = Pattern.compile(regExp);
				Matcher matcher = pattern.matcher(density.getCondDensity());
				if(matcher.find()){
					PhysicalQuantity physicalQuantity = new PhysicalQuantity();
					physicalQuantity.setValue(new FloatValue(Float.parseFloat(matcher.group(1))));
					physicalQuantity.setUnit(matcher.group(2));
					element.setParameter(physicalQuantity);
				}

				element.setParent(vis);
				element.setDefaultColor("0XFF1919");
				vis.getVisualGroupElements().add(element);

				visualizationTree.addChild(vis);
				groupsMap.replace(density.getIonChannel(), vis);
			}
		}
		
		CompositeNode cellNode = new CompositeNode(cellId);
		
		CompositeNode allSegments = getVisualObjectsFromListOfSegments(cellmorphology.getSegment(),
										cellmorphology.getSegmentGroup(), cellmorphology.getId());
				
		HashMap<String, List<AVisualObjectNode>> segmentGeometries = new HashMap<String, List<AVisualObjectNode>>();

		if(!cellmorphology.getSegmentGroup().isEmpty())
		{
			HashMap<String, List<String>> subgroupsMap = new HashMap<String, List<String>>();
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
//					TextMetadataNode text = new TextMetadataNode("segment_groups");
//					text.setValue(getAllGroupsString(sg, subgroupsMap, ""));
				}
			}

			// this adds all segment groups not contained in the macro groups if any
			for(String sgId : segmentGeometries.keySet())
			{
				List<AVisualObjectNode> segments = segmentGeometries.get(sgId);

				for(AVisualObjectNode s : segments){
					s.setParent(cellNode);
				}
				cellNode.getChildren().addAll(segments);
			}

		}

		cellNode.setParent(visualizationTree);
		visualizationTree.addChild(cellNode);
	}

	/**
	 * @param targetSg
	 * @param subgroupsMap
	 * @param allGroupsStringp
	 * @return a semicolon separated string containing all the subgroups that contain a given subgroup
	 */
	private String getAllGroupsString(String targetSg, HashMap<String, List<String>> subgroupsMap, String allGroupsStringp)
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

			for(ANode g : segments)
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
	 * @param visualGroupNode 
	 * @return
	 */
	private AVisualObjectNode getCylinderFromSegment(Segment s,Point3DWithDiam parentDistal)
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
