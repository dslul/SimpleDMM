package org.dslul.simpledmm;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.sigrok.core.classes.ConfigKey;
import org.sigrok.core.classes.Context;
import org.sigrok.core.classes.Driver;
import org.sigrok.core.classes.HardwareDevice;
import org.sigrok.core.classes.OutputFormat;
import org.sigrok.core.classes.Session;
import org.sigrok.core.classes.Variant;
import org.sigrok.core.interfaces.DatafeedCallback;

/**
 * @author daniele
 *
 */
public class DMMManager {
	
	Context context;
	Session session;
	
	public DMMManager() {
		context = Context.create();
		session = context.create_session();
	}
	
	private class RunAcquisition implements Runnable {
		@Override
		public void run() {
			try {
				session.start();
				System.out.println("Running...");
				session.run();
				System.out.println("Stopped.");
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	public void connect(HardwareDevice device, DatafeedCallback callback) {
		device.open();
		session.add_device(device);
		session.add_datafeed_callback(callback);
	}
	
	public Vector<HardwareDevice> getConnectedDevices() {
		Vector<HardwareDevice> deviceList = new Vector<>();
		try {
			// scan for all connected equipment
			Vector<HardwareDevice> tmp = new Vector<>();
			for (Map.Entry<String, Driver> driversEntry : context.drivers().entrySet()) {
				// scan for USB connections
				tmp = driversEntry.getValue().scan();
				if (tmp.isEmpty() == false) {
					for (HardwareDevice device : tmp) {
						deviceList.add(device);
						System.out.println("Found USB device: " + driversEntry.getValue().long_name());
					}
				}
			}
			// scan for serial connections
			String[] serialDevices = { "uni-t-ut61e-ser" }; // TODO: add to settings
			for (String driverName : serialDevices) {
				Driver driver = context.drivers().get(driverName);
				Map<String, String> serialPorts = context.serials(driver);
				if (serialPorts.isEmpty() == false) {
					for (Map.Entry<String, String> serialPort : serialPorts.entrySet()) {
						Map<ConfigKey, Variant> connMap = new HashMap<>();
						connMap.put(ConfigKey.CONN, ConfigKey.CONN.parse_string(serialPort.getKey()));
						tmp = driver.scan(connMap);
						if (tmp.isEmpty() == false) {
							for (HardwareDevice device : tmp) {
								deviceList.add(device);
								System.out.println("Found RS-232 device: " + driver.long_name());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return deviceList;
	}

	public OutputFormat getOutputFormat() {
		return context.output_formats().get("analog");
	}

	public void start() {
		Runnable r = new RunAcquisition();
		Thread t = new Thread(r);
		t.start();
	}

	public void stop() {
		session.stop();
	}

	public boolean isStopped() {
		return !session.is_running();
	}
}
