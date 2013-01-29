package com.almende.cape.agent;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import com.almende.cape.DB;
import com.almende.eve.agent.AgentFactory;
import com.almende.eve.agent.annotation.Name;
import com.almende.eve.transport.xmpp.XmppService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class ContactAgent extends com.almende.eve.agent.Agent {
	
	static final ObjectMapper om = new ObjectMapper();

	private static enum RelTypes implements RelationshipType
	{
	    GROUPMEMBER,
	    SUBGROUP
	}
	static boolean test = generateTestData();
	//Generate test data in graph like manner:
	private static boolean generateTestData(){
		GraphDatabaseService db = DB.get();

		System.out.println("GenerateTestData() called!");
		Node rootNode = DB.getIndex().get("id","agents").getSingle();
		if (rootNode == null){
			Transaction tx = DB.get().beginTx();
			try {
				rootNode = db.createNode();
				rootNode.setProperty("id", "agents");

				ArrayList<Node> groups = new ArrayList<Node>(5);
				
				Node friendsNode = db.createNode();
				friendsNode.setProperty("name","friends");
				rootNode.createRelationshipTo(friendsNode,RelTypes.SUBGROUP);
				groups.add(friendsNode);
				
				Node schoolDaysNode = db.createNode();
				schoolDaysNode.setProperty("name","schooldays");
				friendsNode.createRelationshipTo(schoolDaysNode,RelTypes.SUBGROUP);
				groups.add(schoolDaysNode);

				Node colleaguesNode = db.createNode();
				colleaguesNode.setProperty("name","colleagues");
				rootNode.createRelationshipTo(colleaguesNode,RelTypes.SUBGROUP);
				groups.add(colleaguesNode);

				Node projectNode = db.createNode();
				projectNode.setProperty("name","project X");
				colleaguesNode.createRelationshipTo(projectNode,RelTypes.SUBGROUP);
				groups.add(projectNode);

				Node familyNode = db.createNode();
				familyNode.setProperty("name","family");
				rootNode.createRelationshipTo(familyNode,RelTypes.SUBGROUP);
				groups.add(familyNode);
				
				ArrayList<String> names = new ArrayList<String>(20);
				names.add("Jan");
				names.add("Erika");
				names.add("Loes");
				names.add("Piet");
				names.add("Klaas");
				names.add("Joop");
				names.add("Jaap");
				names.add("Daniel");
				names.add("Annelies");
				names.add("Esther");
				names.add("Noa");
				names.add("Lisette");
				names.add("Dorien");
				names.add("Bert");
				names.add("Erik");
				names.add("Albert");
				names.add("Hielke");
				names.add("Ilka");
				names.add("Tamara");
				names.add("Sietse");
				names.add("Gerben");
				names.add("George");
				names.add("Ida");
				names.add("Leonie");
				
				for (String name: names){
					Node node = db.createNode();
					node.setProperty("name", name);
					Node group = groups.get((int)Math.floor(Math.random()*groups.size()));
					group.createRelationshipTo(node, RelTypes.GROUPMEMBER);
					
					if (Math.random()>=0.5){
						Node secondGroup = groups.get((int)Math.floor(Math.random()*groups.size()));
						if (!secondGroup.equals(group)) secondGroup.createRelationshipTo(node, RelTypes.GROUPMEMBER);
					}
				}
				tx.success();
			} finally {
				tx.finish();
			}
		}
		System.out.println("Found rootnode:"+rootNode.getId()+":"+rootNode.toString());
		return true;
	}
	
	public void xmppConnect(@Name("username") String username,
	        @Name("password") String password) throws Exception {
	    AgentFactory factory = getAgentFactory();

	    XmppService service = (XmppService) factory.getTransportService("xmpp");
	    if (service != null) {
	        service.connect(getId(), username, password);
	    }
	    else {
	        throw new Exception("No XMPP service registered");
	    }
	}

	public void xmppDisconnect() throws Exception {
	    AgentFactory factory = getAgentFactory();
	    XmppService service = (XmppService) factory.getTransportService("xmpp");
	    if (service != null) {
	        service.disconnect(getId());
	    }
	    else {
	        throw new Exception("No XMPP service registered");
	    }
	}
	
	public String getContacts(@Name("filter") String filter){
		StringWriter resultWriter= new StringWriter();
		JsonFactory f = new JsonFactory();
		f.setCodec(om);
		JsonGenerator g;
		try {
			g = f.createJsonGenerator(resultWriter);
			
			
			final Node target = DB.getIndex().get("id","agents").getSingle();
			TraversalDescription td = Traversal.description()
					.relationships(RelTypes.GROUPMEMBER, Direction.OUTGOING)
					.relationships(RelTypes.SUBGROUP, Direction.OUTGOING)
					.uniqueness( Uniqueness.NODE_PATH )
			        .evaluator( new Evaluator()
			{
			    @Override
			    public Evaluation evaluate( Path path )
			    {
			        if ( path.endNode().hasProperty("name") && path.lastRelationship().isType(RelTypes.GROUPMEMBER))
			        {
			            return Evaluation.INCLUDE_AND_PRUNE;
			        }
			        return Evaluation.EXCLUDE_AND_CONTINUE;
			    }
			} );
			 
			Traverser results = td.traverse( target );
			
			Multimap<Long,Path> groupByNode = ArrayListMultimap.create();
			
			Iterator<Path> paths = results.iterator();
			while(paths.hasNext()){
				Path path = paths.next();
				groupByNode.put(path.endNode().getId(), path);
			}
			g.writeStartArray();
			for (Long nodeId : groupByNode.keySet()){
				List<Path> pathList = (List<Path>)groupByNode.get(nodeId);
				g.writeStartObject();
				Node node = pathList.get(0).endNode();
				g.writeStringField("name", (String)node.getProperty("name"));
				g.writeArrayFieldStart("groups");
				for (Path path : pathList){
					g.writeStartObject();
						g.writeStringField("name", (String)path.lastRelationship().getStartNode().getProperty("name"));
						g.writeArrayFieldStart("path");
							for (Node pathStep : path.nodes()){
								if ( !pathStep.equals(node) &&
										pathStep.hasProperty("name")
								) g.writeString((String) pathStep.getProperty("name"));
							}
						g.writeEndArray();
					g.writeEndObject();
				}
				g.writeEndArray();
				g.writeEndObject();
			}
			g.writeEndArray();
			g.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultWriter.flush();
		return resultWriter.toString();
	}
	public String createContact(@Name("contact") String json){
		return "{}";
	}
	public String updateContact(@Name("contact") String json){
		return "{}";
	}
	public void deleteContact(@Name("contact") String json){
		return;
	}
	public String getGroups(@Name("filter") String filter){
		StringWriter resultWriter= new StringWriter();
		JsonFactory f = new JsonFactory();
		f.setCodec(om);
		JsonGenerator g;
		try {
			g = f.createJsonGenerator(resultWriter);
			
			final Node target = DB.getIndex().get("id","agents").getSingle();
			TraversalDescription td = Traversal.description()
					.relationships(RelTypes.SUBGROUP, Direction.OUTGOING)
					.uniqueness( Uniqueness.NODE_PATH )
			;
			 
			Traverser results = td.traverse( target );
			g.writeStartArray();
			Iterator<Path> paths = results.iterator();
			while(paths.hasNext()){
				Path path = paths.next();
				if (!path.endNode().hasProperty("name")) continue;
				g.writeStartObject();
					g.writeStringField("name", (String)path.endNode().getProperty("name"));
					g.writeArrayFieldStart("path");
						for (Node pathStep : path.nodes()){
							if (pathStep.hasProperty("name")) g.writeString((String) pathStep.getProperty("name"));
						}
					g.writeEndArray();
				g.writeEndObject();
			}
			g.writeEndArray();
			g.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		resultWriter.flush();
		return resultWriter.toString();
	}
	public String createGroup(@Name("group") String json){
		return "{}";
	}
	public String updateGroup(@Name("group") String json){
		return "{}";
	}
	public void deleteGroup(@Name("group") String json){
		return;
	}
	@Override
	public String getDescription() {
		return "Hi there, I'm a demo agent for Cape, providing contact information!";
	}

	@Override
	public String getVersion() {
		return "0.2";
	}

}
