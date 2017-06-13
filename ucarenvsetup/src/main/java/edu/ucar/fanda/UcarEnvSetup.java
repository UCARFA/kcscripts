package edu.ucar.fanda;

import edu.ucar.fanda.envsetupapi.CreateCustomDataAttributes;
import edu.ucar.fanda.envsetupapi.UpdateAwardTransTypes;
import edu.ucar.fanda.envsetupapi.UpdateUnitAdminTypes;

public class UcarEnvSetup {
	public static void main(String[] args) {
		try {
			if (args.length == 0 || args.length > 1) {
				System.out.println("Must have one and only one argument: server name.\n");
			} else {
				String serverName = args[0];
				UpdateUnitAdminTypes updateUnitAdminTypes = new UpdateUnitAdminTypes(serverName);
				updateUnitAdminTypes.runUpdate();
				CreateCustomDataAttributes createCustomDataAttributes = new CreateCustomDataAttributes(serverName);
				createCustomDataAttributes.runUpdate();
				UpdateAwardTransTypes updateAwardTransTypes = new UpdateAwardTransTypes(serverName);
				updateAwardTransTypes.runUpdate();
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
