package uk.ac.imperial.presage2.core.db;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class Neo4jDatabase implements DatabaseService, GraphDB {

	protected final Logger logger = Logger.getLogger(Neo4jDatabase.class);

	GraphDatabaseService graphDB = null;

	private static String databasePath = "var/presagedb";

	@Override
	public void start() throws Exception {
		if (graphDB == null) {
			logger.info("Starting embedded Neo4j database at " + databasePath);
			graphDB = new EmbeddedGraphDatabase(databasePath);
			
			// check/create initial graph structure
			Transaction tx = graphDB.beginTx();
			try {
				for(BaseRelationships r : BaseRelationships.values()) {
					if(graphDB.getReferenceNode().getSingleRelationship(r, Direction.OUTGOING) == null) {
						graphDB.getReferenceNode().createRelationshipTo(graphDB.createNode(), r);
					}
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}
	}

	@Override
	public boolean isStarted() {
		return graphDB != null;
	}

	@Override
	public void stop() {
		if (graphDB != null) {
			logger.info("Shutting down Neo4j database...");
			graphDB.shutdown();
		}
	}

	public Transaction beginTx() {
		return graphDB.beginTx();
	}

	public Node createNode() {
		return graphDB.createNode();
	}

	public Iterable<Node> getAllNodes() {
		return graphDB.getAllNodes();
	}

	public Node getNodeById(long arg0) {
		return graphDB.getNodeById(arg0);
	}

	public Node getReferenceNode() {
		return graphDB.getReferenceNode();
	}

	public Relationship getRelationshipById(long arg0) {
		return graphDB.getRelationshipById(arg0);
	}

	public Iterable<RelationshipType> getRelationshipTypes() {
		return graphDB.getRelationshipTypes();
	}

	public IndexManager index() {
		return graphDB.index();
	}

	public KernelEventHandler registerKernelEventHandler(KernelEventHandler arg0) {
		return graphDB.registerKernelEventHandler(arg0);
	}

	public <T> TransactionEventHandler<T> registerTransactionEventHandler(
			TransactionEventHandler<T> arg0) {
		return graphDB.registerTransactionEventHandler(arg0);
	}

	public void shutdown() {
		graphDB.shutdown();
	}

	public KernelEventHandler unregisterKernelEventHandler(
			KernelEventHandler arg0) {
		return graphDB.unregisterKernelEventHandler(arg0);
	}

	public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(
			TransactionEventHandler<T> arg0) {
		return graphDB.unregisterTransactionEventHandler(arg0);
	}

}
