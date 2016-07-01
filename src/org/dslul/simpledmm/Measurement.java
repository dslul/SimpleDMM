package org.dslul.simpledmm;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Measurement {

	private double value = -1;
	private String unit = "";
	private boolean isAuto = false;
	private String type = "";
	private String time = "";
	
	private long timeMillis;
	//private LocalDateTime dateTime;
	private boolean isValid = true;
	
	public Measurement(String text) {
		String[] tmp = text.split(System.getProperty("line.separator"))[0].split(" ");
		String valueStr = tmp[1];
		try {
			this.value = NumberFormat.getNumberInstance(Locale.getDefault()).parse(valueStr).floatValue();
		} catch (ParseException e) {
			isValid = false;
			System.out.println(valueStr);
			e.printStackTrace();
		}
		
		this.unit = tmp[2];
		
		if(tmp.length > 3 && (tmp[3].equals("AC") || tmp[3].equals("DC"))) {
			this.type = tmp[3];
		}
		
		if(text.contains("AUTO")) {
			this.isAuto = true;
		}
		
		//dateTime = LocalDateTime.now();
		timeMillis = System.currentTimeMillis();
		this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss -- d/M/YY"));
	}
	
	public double getValue() {
		return value;
	}

	public String getUnit() {
		return unit;
	}
	
	public boolean isAuto() {
		return isAuto;
	}

	public String getType() {
		return type;
	}
	
	public String getTime() {
		return time;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public long getTimeMillis() {
		return timeMillis;
	}
	
	@Override
	public String toString() {
		return "Measurement [value=" + value + ", unit=" + unit + ", isAuto=" + isAuto + ", time=" + time + "]";
	}

	public String toCsvString() {
		return value + "," + unit + "," + isAuto + "," + time;
	}


}
