package com.almende.cape.test;

import java.util.Scanner;

import com.almende.cape.CapeClient;
import com.almende.cape.entity.Message;
import com.almende.cape.handler.MessageHandler;
import com.almende.cape.handler.StateChangeHandler;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestCapeClient {
	public static void main(String[] args) {
		try {
			CapeClient cape = new CapeClient();
			
			String username = "alex";
			String password = "alex";
			cape.login(username, password);
			
			ArrayNode contacts = cape.getContacts(null);
			System.out.println("contacts:" + contacts);

			//cape.sendNotification("gloria", "hello gloria!");
			
			cape.onMessage(new MessageHandler() {
				
				@Override
				public void onMessage(Message message) {
					System.out.println("Notification: " + message.getMessage());
				}
			});

			cape.onStateChange("location", new StateChangeHandler() {
				@Override
				public void onChange(Object state) {
					System.out.println("State changed: " + state);
				}
			});
			
			System.out.println("Press ENTER to quit");
			Scanner scanner = new Scanner(System.in);
	        scanner.nextLine();
	        
	        cape.logout();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
