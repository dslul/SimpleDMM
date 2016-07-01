package org.dslul.simpledmm;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.Vector;

import org.sigrok.core.classes.Device;
import org.sigrok.core.classes.HardwareDevice;
import org.sigrok.core.classes.Output;
import org.sigrok.core.classes.Packet;
import org.sigrok.core.interfaces.DatafeedCallback;


import org.gillius.jfxutils.JFXUtil;
import org.gillius.jfxutils.chart.ChartPanManager;
import org.gillius.jfxutils.chart.FixedFormatTickFormatter;
import org.gillius.jfxutils.chart.JFXChartUtil;
import org.gillius.jfxutils.chart.StableTicksAxis;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;

public class MainController {
	
	private enum Status { FIRST_START, RUNNING, STOPPED}
	
	private DMMManager dManager;
	private Vector<HardwareDevice> devices = new Vector<>();
	private Status status = Status.FIRST_START;
	
	private final ObservableList<Measurement> data = FXCollections.observableArrayList();
	XYChart.Series<Number,Number> series = new XYChart.Series<>();
	
	public MainController() {
		dManager = new DMMManager();
	}
	
	@FXML
	public void initialize() {
		colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
		colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
		table.setItems(data);
		System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
		format.setTimeZone(TimeZone.getDefault());
		((StableTicksAxis)chart.getXAxis()).setAxisTickFormatter(
				new FixedFormatTickFormatter(format));
		//chart configs
	    series.setName("Data Series 1");
	    chart.getStyleClass().add("thick-chart");
	    //chart.setCreateSymbols(false);
	    chart.getData().add(series);
	    chart.setAnimated(false);
	    chart.setHorizontalGridLinesVisible(true);
	    ((StableTicksAxis)chart.getXAxis()).setForceZeroInRange(false);
	    ChartPanManager panner = new ChartPanManager(chart);
	    //panning
		panner.setMouseFilter((mouseEvent) -> {
				if ( mouseEvent.getButton() == MouseButton.SECONDARY ||
						 (mouseEvent.getButton() == MouseButton.PRIMARY &&
						  mouseEvent.isShortcutDown())){
					//let it through
				} else {
					mouseEvent.consume();
				}
		});
		panner.start();

		//Zooming works only via primary mouse button without ctrl held down
		JFXChartUtil.setupZooming( chart, (mouseEvent) -> {
				if (mouseEvent.getButton() != MouseButton.PRIMARY ||
				     mouseEvent.isShortcutDown() )
				mouseEvent.consume();
		});
		//double click to reset zoom
		JFXChartUtil.addDoublePrimaryClickAutoRangeHandler(chart);
	    //scan for devices on startup
	    this.eventScan();
	}
	
	@FXML
	private BorderPane mainPane;
	
	@FXML
	private Button btnAcquisition;
	@FXML
	private Button btnScan;

    @FXML
    private ChoiceBox<String> comboDevices;

    @FXML
    private LineChart<Number, Number> chart;

    @FXML
    private TableView<Measurement> table;
    @FXML
    private TableColumn<Measurement, Double> colValue;
    @FXML
    private TableColumn<Measurement, String> colUnit;
    @FXML
    private TableColumn<Measurement, String> colTime;
	
    
	@FXML
	private void eventAcquisition() {
		switch (status) {
		case FIRST_START:
			int deviceId = comboDevices.getSelectionModel().getSelectedIndex();
			HardwareDevice device = devices.get(deviceId);
			dManager.connect(device, new CLIDatafeedCallback(device));
			dManager.start();
			chart.getXAxis().setAutoRanging(true);
			chart.getYAxis().setAutoRanging(true);
			btnAcquisition.setText("Stop acquisition");
			status = Status.RUNNING;
			break;
		case RUNNING:
			dManager.stop();
			if(dManager.isStopped()) {
				btnAcquisition.setText("Start acquisition");
				status = Status.STOPPED;
			}
			break;
		case STOPPED:
			dManager.start();
			if(dManager.isStopped() == false) {
				btnAcquisition.setText("Stop acquisition");
				status = Status.RUNNING;
			}
			break;
		}
	}
	
	@FXML
	private void eventScan() {
		devices = dManager.getConnectedDevices();
		
		ObservableList<String> deviceNames = FXCollections.observableArrayList();
		for (HardwareDevice device : devices) {
			deviceNames.add(device.model());
		}
		comboDevices.getItems().clear();
		comboDevices.getItems().addAll(deviceNames);
		comboDevices.getSelectionModel().selectFirst();
		if(comboDevices.getSelectionModel().getSelectedItem() != null) {
			btnAcquisition.setDisable(false);
		} else {
			btnAcquisition.setDisable(true);
		}
	}
	
	
	@FXML
	private void eventSaveToFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.setInitialFileName("measurements_" + LocalDateTime.now().toString() + ".csv");
		File file = fileChooser.showSaveDialog((Stage)mainPane.getScene().getWindow());
        if (file != null) {
        	Writer fileWriter;
			try {
				fileWriter = new FileWriter(file);
				for(Measurement meas : data) {
					fileWriter.write(meas.toCsvString());
					fileWriter.write(System.getProperty("line.separator"));
				}
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	
	
	
	
	// called for every measurement
	class CLIDatafeedCallback implements DatafeedCallback {
		Output output;

		public CLIDatafeedCallback(Device device) {
			this.output = dManager.getOutputFormat().create_output(device);
		}
		
		public void run(Device device, Packet packet) {
			String text = output.receive(packet);
			if (text.length() > 0) {
				Measurement meas = new Measurement(text);
				if(meas.isValid()) {
					data.add(meas);
					Platform.runLater(() ->
			        	series.getData().add(new XYChart.Data<Number,Number>(meas.getTimeMillis(), meas.getValue()))
			        );
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}
}
