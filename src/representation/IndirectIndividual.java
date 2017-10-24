package representation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import moead.Individual;
import moead.MOEAD;
import moead.Service;

public class IndirectIndividual extends Individual {
	private double availability;
	private double reliability;
	private double time;
	private double cost;
	private Service[] genome;
	private double[] objectives;
	private MOEAD init;
	private int rank;
	private double crowdingDistance;

	@Override
	public Individual generateIndividual() {
		Collections.shuffle(init.relevantList, init.random);

		IndirectIndividual newInd = new IndirectIndividual();
		newInd.genome = new Service[init.relevantList.size()];
		init.relevantList.toArray(newInd.genome);
		newInd.setInit(init);

		newInd.evaluate();
		if (MOEAD.dynamicNormalisation)
			newInd.finishCalculatingFitness();
		return newInd;
	}

	@Override
	public Individual clone() {
		IndirectIndividual newInd = new IndirectIndividual();

		// Shallow cloning is fine in this approach
		newInd.genome = genome.clone();

		newInd.availability = availability;
		newInd.reliability = reliability;
		newInd.time = time;
		newInd.cost = cost;
		newInd.init = init;
		newInd.objectives = new double[objectives.length];

		System.arraycopy(objectives, 0, newInd.objectives, 0, objectives.length);

		return newInd;
	}

	@Override
	public double[] getObjectiveValues() {
		return objectives;
	}

	@Override
	public void setObjectiveValues(double[] newObjectives) {
		objectives = newObjectives;
	}

	@Override
	public double getAvailability() {
		return availability;
	}

	@Override
	public double getReliability() {
		return reliability;
	}

	@Override
	public double getTime() {
		return time;
	}

	@Override
	public double getCost() {
		return cost;
	}

	public Service[] getGenome() {
		return genome;
	}

	public void createNewGenome() {
		genome = new Service[init.relevantList.size()];
	}

	@Override
	public String toString() {
		Graph g = createNewGraph(init.numLayers, init.startServ, init.endServ, genome);
		return g.toString();
	}

	@Override
	public void evaluate() {
		calculateSequenceFitness(init.numLayers, init.endServ, genome);
	}

   public void calculateSequenceFitness(int numLayers, Service end, Service[] sequence) {
        Set<Service> solution = new HashSet<Service>();

        cost = 0.0;
        availability = 1.0;
        reliability = 1.0;

        // Populate inputs to satisfy with end node's inputs
        List<InputTimeLayerTrio> nextInputsToSatisfy = new ArrayList<InputTimeLayerTrio>();
        double t = end.getQos()[MOEAD.TIME];
        for (String input : end.getInputs()){
            nextInputsToSatisfy.add( new InputTimeLayerTrio(input, t, numLayers) );
        }

        // Fulfil inputs layer by layer
        for (int currLayer = numLayers; currLayer > 0; currLayer--) {
            // Filter out the inputs from this layer that need to fulfilled
            List<InputTimeLayerTrio> inputsToSatisfy = new ArrayList<InputTimeLayerTrio>();
            for (InputTimeLayerTrio p : nextInputsToSatisfy) {
               if (p.layer == currLayer)
                   inputsToSatisfy.add( p );
            }
            nextInputsToSatisfy.removeAll( inputsToSatisfy );

            int index = 0;
            while (!inputsToSatisfy.isEmpty()){
                // If all nodes have been attempted, inputs must be fulfilled with start node
                if (index >= sequence.length) {
                    nextInputsToSatisfy.addAll(inputsToSatisfy);
                    inputsToSatisfy.clear();
                }
                else {
                Service nextNode = sequence[index++];
                if (nextNode.layer < currLayer) {

   	                List<InputTimeLayerTrio> satisfied = getInputsSatisfied(inputsToSatisfy, nextNode, init);
   	                if (!satisfied.isEmpty()) {
                           double[] qos = nextNode.getQos();
                           if (!solution.contains( nextNode )) {
                               solution.add(nextNode);
                               cost += qos[MOEAD.COST];
                               availability *= qos[MOEAD.AVAILABILITY];
                               reliability *= qos[MOEAD.RELIABILITY];
                           }
                           t = qos[MOEAD.TIME];
                           inputsToSatisfy.removeAll(satisfied);

                           double highestT = findHighestTime(satisfied);

                           for(String input : nextNode.getInputs()) {
                               nextInputsToSatisfy.add( new InputTimeLayerTrio(input, highestT + t, nextNode.layer) );
                           }
                       }
	               }
                }
            }
        }

        // Find the highest overall time
        time = findHighestTime(nextInputsToSatisfy);
        if (!MOEAD.dynamicNormalisation)
        	finishCalculatingFitness();
    }

   @Override
   public void finishCalculatingFitness() {
	   objectives = calculateFitness(cost, time, availability, reliability);
   }

	public double findHighestTime(List<InputTimeLayerTrio> satisfied) {
	    double max = Double.MIN_VALUE;

	    for (InputTimeLayerTrio p : satisfied) {
	        if (p.time > max)
	            max = p.time;
	    }

	    return max;
	}

	public double[] calculateFitness(double c, double t, double a, double r) {
        a = normaliseAvailability(a, init);
        r = normaliseReliability(r, init);
        t = normaliseTime(t, init);
        c = normaliseCost(c, init);

        double[] objectives = new double[2];
        objectives[0] = t + c;
        objectives[1] = a + r;

        return objectives;
	}

	private double normaliseAvailability(double availability, MOEAD init) {
		if (init.maxAvailability - init.minAvailability == 0.0)
			return 1.0;
		else
			return (init.maxAvailability - availability)/(init.maxAvailability - init.minAvailability);
	}

	private double normaliseReliability(double reliability, MOEAD init) {
		if (init.maxReliability- init.minReliability == 0.0)
			return 1.0;
		else
			return (init.maxReliability - reliability)/(init.maxReliability - init.minReliability);
	}

	private double normaliseTime(double time, MOEAD init) {
		if (init.maxTime - init.minTime == 0.0)
			return 1.0;
		else
			return (time - init.minTime)/(init.maxTime - init.minTime);
	}

	private double normaliseCost(double cost, MOEAD init) {
		if (init.maxCost - init.minCost == 0.0)
			return 1.0;
		else
			return (cost - init.minCost)/(init.maxCost - init.minCost);
	}

	public List<InputTimeLayerTrio> getInputsSatisfied(List<InputTimeLayerTrio> inputsToSatisfy, Service n, MOEAD init) {
	    List<InputTimeLayerTrio> satisfied = new ArrayList<InputTimeLayerTrio>();
	    for(InputTimeLayerTrio p : inputsToSatisfy) {
            if (init.taxonomyMap.get(p.input).servicesWithOutput.contains( n ))
                satisfied.add( p );
        }
	    return satisfied;
	}

	public Graph createNewGraph(int numLayers, Service start, Service end, Service[] sequence) {
		Node endNode = new Node(end);
		Node startNode = new Node(start);

        Graph graph = new Graph();
        graph.nodeMap.put(endNode.getName(), endNode);

        // Populate inputs to satisfy with end node's inputs
        List<InputNodeLayerTrio> nextInputsToSatisfy = new ArrayList<InputNodeLayerTrio>();

        for (String input : end.getInputs()){
            nextInputsToSatisfy.add( new InputNodeLayerTrio(input, end.getName(), numLayers) );
        }

        // Fulfil inputs layer by layer
        for (int currLayer = numLayers; currLayer > 0; currLayer--) {

            // Filter out the inputs from this layer that need to fulfilled
            List<InputNodeLayerTrio> inputsToSatisfy = new ArrayList<InputNodeLayerTrio>();
            for (InputNodeLayerTrio p : nextInputsToSatisfy) {
               if (p.layer == currLayer)
                   inputsToSatisfy.add( p );
            }
            nextInputsToSatisfy.removeAll( inputsToSatisfy );

            int index = 0;
            while (!inputsToSatisfy.isEmpty()){

                if (index >= sequence.length) {
                    nextInputsToSatisfy.addAll( inputsToSatisfy );
                    inputsToSatisfy.clear();
                }
                else {
                	Service nextNode = sequence[index++];
                	if (nextNode.layer < currLayer) {
	                    Node n = new Node(nextNode);
	                    //int nLayer = nextNode.layerNum;

	                    List<InputNodeLayerTrio> satisfied = getInputsSatisfiedGraphBuilding(inputsToSatisfy, n, init);

	                    if (!satisfied.isEmpty()) {
	                        if (!graph.nodeMap.containsKey( n.getName() )) {
	                            graph.nodeMap.put(n.getName(), n);
	                        }

	                        // Add edges
	                        createEdges(n, satisfied, graph);
	                        inputsToSatisfy.removeAll(satisfied);


	                        for(String input : n.getInputs()) {
	                            nextInputsToSatisfy.add( new InputNodeLayerTrio(input, n.getName(), n.getLayer()) );
	                        }
	                    }
	                }
                }
            }
        }

        // Connect start node
        graph.nodeMap.put(startNode.getName(), startNode);
        createEdges(startNode, nextInputsToSatisfy, graph);

        return graph;
    }

	public void createEdges(Node origin, List<InputNodeLayerTrio> destinations, Graph graph) {
		// Order inputs by destination
		Map<String, Set<String>> intersectMap = new HashMap<String, Set<String>>();
		for(InputNodeLayerTrio t : destinations) {
			addToIntersectMap(t.service, t.input, intersectMap);
		}

		for (Entry<String,Set<String>> entry : intersectMap.entrySet()) {
			Edge e = new Edge(entry.getValue());
			origin.getOutgoingEdgeList().add(e);
			Node destination = graph.nodeMap.get(entry.getKey());
			destination.getIncomingEdgeList().add(e);
			e.setFromNode(origin);
        	e.setToNode(destination);
        	graph.edgeList.add(e);
		}
	}

	private void addToIntersectMap(String destination, String input, Map<String, Set<String>> intersectMap) {
		Set<String> intersect = intersectMap.get(destination);
		if (intersect == null) {
			intersect = new HashSet<String>();
			intersectMap.put(destination, intersect);
		}
		intersect.add(input);
	}

	public List<InputNodeLayerTrio> getInputsSatisfiedGraphBuilding(List<InputNodeLayerTrio> inputsToSatisfy, Node n, MOEAD init) {
	    List<InputNodeLayerTrio> satisfied = new ArrayList<InputNodeLayerTrio>();
	    for(InputNodeLayerTrio p : inputsToSatisfy) {
            if (init.taxonomyMap.get(p.input).servicesWithOutput.contains( n.getService()))
                satisfied.add( p );
        }
	    return satisfied;
	}

	@Override
	public void setInit(MOEAD init) {
		this.init = init;
	}

	@Override
	public int getRank() {
		return rank;
	}

	@Override
	public void setRank(int rank) {
		this.rank = rank;
	}

	@Override
	public double getCrowdingDistance() {
		return crowdingDistance;
	}

	@Override
	public void setCrowdingDistance(double crowdingDistance) {
		this.crowdingDistance = crowdingDistance;
	}

}
