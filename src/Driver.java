import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

class Driver {
	
	// Declare class data
	static JFrame mapFrame;
	static JPanel topPanel;
	static JButton playButton;
	static JCheckBox stopsCheckBox;
	static JComboBox<Integer> comboBox;
	static JMapViewer mapViewer;
	static BufferedImage image;
	static IconMarker icon;
	static Timer timer;
	static Color lineColor = Color.RED;
	static int currTime = 0;
	static int animationSpeed = 15;
	static Integer[] animationTimes = {15, 30, 45, 60};
	
	// trip
	static ArrayList<TripPoint> trip;
	
	 public static void main(String[] args) throws FileNotFoundException, IOException {
		
		try {
			TripPoint.readFile("triplog.csv");
			
			TripPoint.h1StopDetection();
		} catch (IOException e) {}
		
		trip = TripPoint.getTrip();
		
		// set up frame
		mapFrame = new JFrame("Project 5 - Giang Hoang");
		mapFrame.setPreferredSize(new Dimension(1150, 750));
		playButton = new JButton("Play");
		stopsCheckBox = new JCheckBox("Include Stops");
		stopsCheckBox.setSelected(true);
		
		// dropbox to select animation time
		JTextField timeText = new JTextField("Animation Speed:");
		timeText.setEditable(false);
		timeText.setBorder(null);
		
		comboBox = new JComboBox<Integer>(animationTimes);
		
		// add to panel
		topPanel = new JPanel();
		topPanel.add(playButton);
		topPanel.add(stopsCheckBox);
		topPanel.add(timeText);
		topPanel.add(comboBox);
		
		// set up map viewer
		mapViewer = new JMapViewer();
		mapViewer.setTileSource(new OsmTileSource.TransportMap());
		
		// set up map icon
		image = null;
		try {
			image = ImageIO.read(new File("arrow.png"));
		} catch (IOException e) {}
		
		icon = new IconMarker(new Coordinate(trip.get(0).getLat(), trip.get(0).getLon()), image);
    	mapViewer.addMapMarker(icon);
    	
    	// add listeners
    	playButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				play();		
			}	
    	});
    	
    	stopsCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {	
				trip = stopsCheckBox.isSelected() ? TripPoint.getTrip() : TripPoint.getMovingTrip();
			}
    	});
    	comboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				animationSpeed = (int) comboBox.getSelectedItem();
				timer.setDelay(animationSpeed*1000 / trip.size());
			}
        });
    	
    	// set map center and zoom level
    	double minLat = TripPoint.getTrip().get(0).getLat();
    	double maxLat = TripPoint.getTrip().get(0).getLat();
    	double minLon = TripPoint.getTrip().get(0).getLon();
    	double maxLon = TripPoint.getTrip().get(0).getLon();
    	
    	for(TripPoint t : TripPoint.getTrip()) {
    		if(t.getLat() < minLat) {
    			minLat = t.getLat();
    		}else if(t.getLat() > maxLat) {
    			maxLat = t.getLat();
    		}if(t.getLon() < minLon) {
    			minLon = t.getLon();
    		}else if(t.getLon() > maxLon) {
    			maxLon = t.getLon();
    		}
    	}
   
    	mapViewer.setDisplayPosition(new Coordinate((minLat + maxLat)/2, (minLon + maxLon)/2), 6);
    	
    	// finish frame
    	mapFrame.add(topPanel, BorderLayout.NORTH);
    	mapFrame.add(mapViewer, BorderLayout.CENTER);
    	mapFrame.pack();
    	mapFrame.setLocationRelativeTo(null);
    	mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	mapFrame.setVisible(true);
    	
    	setTime(0);	
	}
	
	public static void play() {
		
		if(timer!=null && timer.isRunning()) {
			timer.stop();
		}
		
		setTime(0);
		
		int diff = animationSpeed*1000 / trip.size();
    	ActionListener tick = new ActionListener() {
    			
    		public void actionPerformed(ActionEvent evnt) {		
    			if(currTime<=trip.size()*5-10) {
    				setTime(currTime + 5);
    			}else {
    				timer.stop();
    			}	
    		}
    	};
    	timer = new Timer(diff, tick);
    	timer.start();
	}
	
	public static void setTime(int t) {
		
		currTime = t;
		
		mapViewer.removeAllMapPolygons();
		if(t > 0) {
    		for(int i = 1; i < t/5; i++) {
    			TripPoint old = trip.get(i-1);
    			TripPoint current = trip.get(i);
    			
    			drawLine(old, current);
    		}
    	}
		updateArrow();
	}
	
	private static void drawLine(TripPoint t1, TripPoint t2) {
		Coordinate a = new Coordinate(t1.getLat(), t1.getLon());
		Coordinate b = new Coordinate(t2.getLat(), t2.getLon());
    	
    	//draw lines and move icon
    	List<Coordinate> route = new ArrayList<Coordinate>(Arrays.asList(a, b, a));
    	MapPolygonImpl polygon = new MapPolygonImpl(route);
    	polygon.setColor(lineColor);
    	mapViewer.addMapPolygon(polygon);	
    	mapViewer.setMapPolygonsVisible(true);		
    	mapViewer.repaint();	
	}

	private static BufferedImage rotateImage(BufferedImage sourceImage, double angle) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.rotate(angle / 180 * Math.PI, width / 2 , height / 2);
        graphics.drawRenderedImage(sourceImage, transform);
        graphics.dispose();
        return image;
    }

	public static void rotateArrow(double angle) {
		icon.setImage(rotateImage(image, angle));
	}
	
	public static void updateArrow() {
		TripPoint x;
		TripPoint y;
		TripPoint current = trip.get(currTime/5);
		
		// first point
		if(currTime/5==0) {
			x = trip.get(0);
			y = trip.get(1);
		}else {
			x = trip.get(currTime/5-1);
			y = trip.get(currTime/5);
		}
		double angle = Math.toDegrees(Math.atan((y.getLat()-x.getLat())/(y.getLon()-x.getLon())));
		if(y.getLon() > x.getLon()) {
			angle += 180;
		}
		rotateArrow(270 - angle);
		icon.setLat(current.getLat());
		icon.setLon(current.getLon());
	}
		
}