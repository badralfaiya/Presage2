/**
 * 	Copyright (C) 2011 Sam Macbeth <sm1106 [at] imperial [dot] ac [dot] uk>
 *
 * 	This file is part of Presage2.
 *
 *     Presage2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Presage2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser Public License
 *     along with Presage2.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.imperial.presage2.util.network;

import java.io.FileNotFoundException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.db.GraphDB;
import uk.ac.imperial.presage2.core.db.Transaction;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.network.MessageBlockedEvent;
import uk.ac.imperial.presage2.core.network.MessageDeliveryEvent;
import uk.ac.imperial.presage2.core.plugin.Plugin;
import uk.ac.imperial.presage2.db.graph.DataExport;
import uk.ac.imperial.presage2.db.graph.export.Edge;
import uk.ac.imperial.presage2.db.graph.export.GEXFExport;

import com.google.inject.Inject;

public class NetworkMessageMonitor implements Plugin {

	private GraphDB db = null;
	private DataExport exporter = null;
	private final Time time;

	Queue<MessageDeliveryEvent> deliveryQueue = new LinkedBlockingQueue<MessageDeliveryEvent>();

	@Inject
	public NetworkMessageMonitor(EventBus eb, Time t) {
		super();
		this.time = t;
		eb.subscribe(this);
	}

	@Inject(optional = true)
	public void setStorageService(GraphDB db) {
		this.db = db;
	}

	@Inject(optional = true)
	public void setDataExporter(DataExport exp) {
		this.exporter = exp;
	}

	@EventListener
	public void onMessageDelivery(MessageDeliveryEvent e) {
		deliveryQueue.offer(e);

	}

	@EventListener
	public void onMessageBlocked(MessageBlockedEvent e) {

	}

	@Override
	public void incrementTime() {
		if (db != null) {
			Transaction tx = db.startTransaction();
			try {
				while (deliveryQueue.peek() != null) {
					MessageDeliveryEvent e = deliveryQueue.poll();
					Map<String, Object> parameters = new Hashtable<String, Object>();
					parameters.put("time", time.intValue());
					parameters.put("type", e.getMessage().getType());
					parameters.put("performative", e.getMessage()
							.getPerformative().name());
					parameters.put("class", e.getMessage().getClass()
							.getSimpleName());
					db.getAgent(e.getMessage().getFrom().getId())
							.createRelationshipTo(
									db.getAgent(e.getRecipient().getId()),
									"SENT_MESSAGE", parameters);
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}
		time.increment();
	}

	@Override
	public void initialise() {
	}

	@Override
	public void execute() {
	}

	@Override
	public void onSimulationComplete() {
		if (exporter != null) {
			Node n = exporter.getSimulationNode();
			TraversalDescription agentMessaging = Traversal
					.description()
					.breadthFirst()
					.uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
					.relationships(exporter.getParticipantInRelationship(),
							Direction.INCOMING)
					.relationships(
							DynamicRelationshipType.withName("SENT_MESSAGE"))
					.evaluator(Evaluators.excludeStartPosition());
			Traverser t = agentMessaging.traverse(n);
			GEXFExport gexf = GEXFExport.createStaticGraph();
			for (Node node : t.nodes()) {
				gexf.addNode(new uk.ac.imperial.presage2.db.graph.export.Node(
						Long.toString(node.getId()), node.getProperty("label",
								0).toString()));
			}
			for (Relationship r : t.relationships()) {
				// exclude PARTICIPANT_IN relationships
				if (!r.getEndNode().equals(n))
					gexf.addEdge(new Edge(Long.toString(r.getId()), Long
							.toString(r.getStartNode().getId()), Long
							.toString(r.getEndNode().getId()), r
							.getProperty("label", 0).toString()));
			}
			try {
				gexf.writeTo("messaging.gexf");
			} catch (FileNotFoundException e) {

			}
		}
	}

}