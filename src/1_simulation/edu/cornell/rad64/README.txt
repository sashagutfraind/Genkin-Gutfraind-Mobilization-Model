============================
1.  Parameters
============================
1.1 Model Parameters
--------------------
attritionRate
	for each tick t and node j, the probability that node j would be removed from the simulation
	and be replaced by a new node (population is maintained constant for simplicity)
	
fractionExposedToMagnets
	for each magnet i, the fraction of the nodes which attend this magnet.
	for example, at 0.5 each node approximately attends half the magnets
	
fractionInitRadicals
	the long-run number of nodes which will be created at radical zeal level (=1.0)
	when attrition removes nodes, new nodes have fractionInitRadicals of being initially radical

fractionInitPacifists
	the long-run number of nodes which will be created at pacifist zeal level (=-1.0)
	when attrition removes nodes, new nodes have fractionInitRadicals of being initially radical

LayoutType: 
	which layout method is used to display the social network

numMagnetsN
	number of neutral magnets
	
numMagnetsR
	number of radical magnets

numMagnetsP
	number of pacifist magnets

magneticEncounterRate
	for each magnet i and node j, the probability per tick that j would be introduced to another node through i
	
networkRecordTic
	the tic where the network snapshop will be made
	setting to -1 leads to recording of network at the end of the simulation
	setting to -2 (or other negative value) will omit this function

outputDirectory
	where the output files are to be placed (including the network)

population
	number of agents in the simulation
	
radicalsAttritionIncrement
	attrition effect for radical nodes only: the probability that a radical node j 
	would be removed from the simulation each tick is attritionRate+radicalsAttritionIncrement
	
runLength
	lenght of the simulation in tics.  min=1

showNet 
	check shows the social network graph.  significantly slows down the simulation.
showEnergyPlot
	check shows the energy of the network (the lower the energy, the closer the nodes
	tied to each other).  significantly slows down the simulation.

showStatsPlot
	check shows various statistics about radicalization
 
SimName
	a name for the simulation to be recorded in the output files


1.2 Node and Edge Parameters
----------------------------
avgInitTieStrength
	the initial strength of all newly-formed ties
	
avgPressurability
	the average susceptibility of nodes to peer pressure on their variable attributes and zeal.
	if p is the peer pressure (average of peer nodes weighted by tie strength), 
	then the "input" on the node is given by:
			input = peerPressurability/(1+e^(-switchPressFactor*p))
	the attribute or zeal changes state in the positive direction if (u is a uniform variate in [0,1])		
	  	input > 0.5 + u*switchRandomness
	
avgDegree
	average of the maximum number of ties a nodes can carry - its degree cap.  
	once a node is created the degree cap is fixed
	at the creation of a node, the simulation randomly attempts to find that many friends
	
	
degreeSD
	the standard deviation in the normal distribution of degree cap, used when creating new nodes
	
diversity
	probability that a non-zeal attribute will be randomly selected.
	if u is a uniform variate in [0,1], the state of each attribute =-1 if u>diversity/2 and otherwise=1  
	
fixedAttribSalience
	multiplicative factor in the range [0..inf) weighing the importance of fixed attributes 
	in the agreement between two nodes. expressed relative to a variable attribute.
	
initTieStrengthSD
	the standard deviation in the normal distribution of initial tie strength, used when creating new nodes

linkAgeBonusFactor
	a rate expressing how fast a relationship approach its maxLinkAgeBonus addition to agreement:
	bonus to agreement = maxLinkAgeBonus*age/(age+linkAgeBonusFactor)
	
maxLinkAgeBonus
	the bonus (similar to salience) gained to agreement level of two nodes, 
	approached in the limit of infinitely-old relationship
	
numZealLevels
	integer in 2,3,....  
	the possible zeal levels are -1+(2/numZealLevels)*0, -1+(2/numZealLevels)*1, .., +1
	when =2, only zeals +1 (radicals) and -1 (pacifists) are possible.
	when =even, moderates are not allowed
	
numFixedAttributes
	number of attributes not susceptible to peer pressure
	
numVarAttributes
	number of attributes susceptible to peer pressure (zeal is also susceptible and is not included in the count)
	
pressurabilitySD
	the standard deviation in the normal distribution of pressurability, used when creating new nodes

repellingTies
	when checked, ties are allowed to carry negative strength (repulsion)
	enabling this feature leads to numerous conceptual and factual challenges,
	and it is retained mostly for semblence of compatibility with the original Hopfield model
	
safeNewFriends
	when unchecked, when node that is exposed to a prospective friend, the friend is added.
	if the degree cap was saturated, the worst existing friend is dropped
	
strengthUpdateRate
	in [0,1] how fast does tie 
	
switchRandomness
	in [0,1], the weight of randomness in causing a switch of zeal of a variable attribute
	
switchPressFactor
	in [0,1000], multiplicative factor of the peer pressure.  the higher, the more pressure has effect.
	
transitiveFriendship
	probability per tick that the current node will introduce to each other two of its friends
	(the process can repeat even if the two friends have already been introduced before)
	
zealSalience
	multiplicative factor in the range [0..inf) weighing the importance of zeal in the agreement between
	two nodes. expressed relative to a variable attribute.
	


============================
2.	METRICS
============================
Note: to avoid confusion the equations ommit the dash in the definition, e.g. RdyadRatio refers to R-dyadRatio

Note: 
radical := any node with zeal >= 0.5
pacifist := any node with zeal <= -0.5
otherwise the node is a moderate

"All-clusteringSoffer" = like "R-clusteringSoffer" but applied to all nodes of the network
"All-clusteringWatts"  = like "R-clusteringWatts" but applied to all nodes of the network

"avgTieStrength" = the average strength of ties in the network

"energy"		 = the energy of the Hopfield network with adjustments for salience (typically 3-level attribute), age bonus and salience
				 * zeal contributes (s=strength of the tie):
				 	-(1-abs(friend's zeal - zeal))*s*zealSalience
				 * age bonus contributes:
				   maxLinkAgeBonus * (link.getAge()) / (age + linkAgeBonusFactor) * s
				 * salience is accounted for by simple multiplication by the saliance parameter
				 

"medianNumRadRadNeighbors"
					 = median number of radicals surrounding a radical
					 = median of the set: S={for each radical i, number of radical neighbors of i}

"R-assortativity" = metric of homophily based on Newman "Mixing patterns in networks"
				  = (trace - normSq)/(1-normSq)
				  where 
				  trace = fraction of ties that are in-group
				        = (number of ties RadToRad + num of ties NonradToNonrad)/totalTies
	  		      normSq = expected fraction of in-group ties if the network is randomly-lined
                         = (num of tiesFromRadicals/totalTies)^2 + (num of ties from nonradicals/totalTies)^2.0
				  totalTies = num of ties from radicals + num of ties from non-radicals
				  = 1 if trace=1
				  note: each tie counts twice, for each node it leaves
	  	
"R-avgCellSize"   = the avg of set {size of connected components in the subgraph of radical nodes}
					  Note: the connections do not have to be direct.  Suppose the network has the topology:
					  R1-R2-R3 and M1-R4 and M2-R5  where radical nodes have labels starting with "R" and "-" indicates a tie.
					  The avg = 5/3.
					  Note that the average is computed over cell, not over radicals.
					  Thus, the average radical sits in a cell that has a larger size than this
					  (see "R-avgIndCellSize").

"R-avgIndCellSize"   = the avg of set {for each radical x, number of radicals connected to x directly or through other radicals}
 

"R-avgTieStrength"   = in the network of the radical nodes, the average strength of ties
                     if no ties exist = 0

"R-clusteringSoffer" = clustering metric based on capital-C-hat of Soffer & Vazquez: 
							"Network clustering coefficient without degree-correlation biases"
						applied to the subgraph containing the radical nodes.  
					 = number of triangles in the network / number of posibble triangles BASED on node degrees
						* unlike in Soffer & Vazquez who consider simple graphs, for every existing triangle, 
						  its contribution is weighted by the average strength of the tie in it.
						  For example, if the network consists of just one triangle with ties 0.2, 0.4, 0.6, then the metric = 0.4
						* if no triangles are possible then metric = 0
						NOTE: degrees of nodes i = #the connections from i to others radicals AND non-radicals.
						      this is based on the view that radicals are able to switch their connections from non-radicals to radicals

"R-clusteringWatts" = metric of clustering based of Watts & Strogatz 
							"Collective dynamics of ‘small-world’ networks"
						applied to the subgraph containing the radical nodes.  
					 = average over each radical node of the set
					   {number of triangles over node i / number of posibble triangles of node i, where i and its neighbors are radical nodes}
					 also termed the lower-c metric in Soffer & Vazquez
					 = 0 if no triangles are possible
"R-clusteringWattsGlobal"
					 = total number of triangles involving 3 radical nodes / number of possible triangles involving radical nodes
					 	also termed the lower-c metric in Soffer & Vazquez
                     = the capital-C metric in Soffer & Vazquez
						applied to the subgraph containing the radical nodes.  			 
					 = 0 if no triangles are possible

"R-dyadRatio" = sum of ties leaving radicals and going to a radical 
						/ sum of ties leaving radicals to and going to radical and non-radical nodes
				"sum of ties" refers to sum of positive tie strengths: sum {max(0,Tij)}
	
"R-E-I_index"		 = the Internal-External index of on Krackhardt & Stern 
                          "Informal Networks and Organizational Crises: An experimental simulation" SPQ 1988
                       where "internal" refers to the number of connected pairs of radicals, 
                       and   "external" refers to ties connecting radicals to non-radicals.

"R-excessTriads"     = RrawTriads * [numRadicals*(numRadicals-1)/2] / number of possible according to Soffer 

"R-fraction" = number of radicals in the population / size of the population

"R-homophilyPin" = metric of choice homophily derived from H metrics in Pin et al. "Opportunity and choice in social networks"
				 = (RdyadRatio - Rfraction)/(1-Rfraction)
				 see below for RdyadRatio
				 = 0.0 if Rfraction=1


"R-medianCellSize"   = the median of set {size of connected components in the subgraph of radical nodes}
					  Note: the connections do not have to be direct.  Suppose the network has the topology:
					  R1-R2-R3 and M1-R4 and M2-R5   where radical nodes have labels starting with "R" and "-" indicates a tie.
					  The median = 1
"R-numCells"         = number of connected components in the subgraph of radical nodes.  
					   Value is 3 in the example for the metric "R-medianCellSize".
"R-numIsolatedCells" = same as "R-numCells", but counting only components with no ties to non-radical nodes (completely isolated).
						Value is 1 in the example for the metric "R-medianCellSize".
"R-rawDyads" 	= number of pairs of radicals connected by a direct link.  
                    
"R-rawTriads"		 = number of triads in the network where all 3 nodes are radicals


"zealCorr"           = correlation coefficient between each node (X series) and the average zeal of its neighbors (Y series)
					 = 0 if the variance of X is 0 or variance of Y is 0
