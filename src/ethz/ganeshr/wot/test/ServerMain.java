package ethz.ganeshr.wot.test;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thingweb.servient.ServientBuilder;
import de.thingweb.servient.ThingInterface;
import de.thingweb.servient.ThingServer;
import de.thingweb.servient.impl.ServedThing;
import de.thingweb.thing.Content;
import de.thingweb.thing.MediaType;
import de.thingweb.thing.Property;
import de.thingweb.thing.Thing;
import de.thingweb.util.encoding.ContentHelper;

public class ServerMain {
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ServerMain serverMain = new ServerMain();
		serverMain.start();
		
	}
	
	private final ThingServer server;
	private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
	BACnetChannel bacnetChannel = null;
	
	public ServerMain() throws Exception{
		System.out.println("Hello");
		ServientBuilder.getHttpBinding().setPort(80);
		ServientBuilder.initialize();
		server = ServientBuilder.newThingServer();
		//final ThingDescription basicLedDesc = DescriptionParser.fromFile("e:/data/temp/basic_led.jsonld");
		//ThingInterface basicLed = server.addThing(basicLedDesc);
		//attachBasicHandlers(basicLed);

		bacnetChannel = new BACnetChannel();
		bacnetChannel.open();
		List<Thing> things = bacnetChannel.discover(5000);
		//bacnet.close();
		
		for(Thing thing : things){
			if(thing == null)
				log.error("null thing!");
			else{
			ThingInterface thingIfc = server.addThing(thing);
			attachHandler((ServedThing)thingIfc);
			}
		}
	}
	
	public void start() throws Exception{
		ServientBuilder.start();		
	}
	Date dummy;
	public void attachHandler(ServedThing thing){
		thing.onPropertyRead((input) -> {
			Property property = (Property)input;
			log.info("Got a read");
			String result = "";
			String propertyName = property.getName();
			if(propertyName.equalsIgnoreCase("_action/status")){
				Date d = new Date();
				long i = d.getTime() - dummy.getTime();
				if(i > 10000)
					result = "{\"state\":\"done\"}";
				else
					result = String.format("{\"state\":\"%d\"}", i);;
			}
			else{
				result = bacnetChannel.read(thing.getName() + "/" + propertyName);
			}
			thing.setProperty(property, result);	
			
		});
		
		thing.onActionInvoke("_action", (input) -> {
			Property subResource = Property.getBuilder("").setPropertyType("").build();
			thing.addProperty(subResource);
			dummy = new Date();
			server.rebindSec(thing.getName(), false);
			return new Content("{\"state\":\"in_progress\"}".getBytes(), MediaType.APPLICATION_JSON);
		} );
	}	

}
