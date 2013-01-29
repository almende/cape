package com.almende.cape;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.tooling.GlobalGraphOperations;

public class DB  {
	private final static String DB_PATH="neoDB";
	private static GraphDatabaseService graphDb = null;
	private static WrappingNeoServerBootstrapper srv;
	private static AutoIndexer<Node> nodeAutoIndexer = null;
	// The server is now running
	// until we stop it:
	
	
	public static GraphDatabaseService get(){
		if (graphDb == null){
			graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( DB_PATH ) 
					.setConfig( GraphDatabaseSettings.node_keys_indexable, "id" )
					.setConfig( GraphDatabaseSettings.node_auto_indexing, "true" )
					.newGraphDatabase();
			 nodeAutoIndexer = graphDb.index()
			        .getNodeAutoIndexer();
			nodeAutoIndexer.startAutoIndexingProperty("id");
			nodeAutoIndexer.setEnabled(true);
			
			srv = new WrappingNeoServerBootstrapper((GraphDatabaseAPI)graphDb );
			srv.start();
			registerShutdownHook(graphDb);
		}
		return graphDb;
	}
	public static ReadableIndex<Node> getIndex(){
		return nodeAutoIndexer.getAutoIndex();
	}
	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running example before it's completed)
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	        	srv.stop();
	        	graphDb.shutdown();
	        }
	    } );
	}
	public static void emptyDB(){
		Transaction tx = get().beginTx();
		try {
			for (Node node : GlobalGraphOperations.at(graphDb).getAllNodes()){
				for (Relationship rel : node.getRelationships()){
					rel.delete();
				}
				if (node.getId()!=0){ //keep ref/rootnode
					node.delete();	
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}
}
