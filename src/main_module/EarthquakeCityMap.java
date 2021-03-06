package main_module;
//test
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Donation.Donate2;
import People.MissingInterface;
import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.utils.MapUtils;
import parsing.ParseFeed;
import processing.core.PApplet;

/** EarthquakeCityMap
 * An application with an interactive map displaying earthquake data.
 * @author Team Defaulting
 * Date: March 3, 2016
 * */
public class EarthquakeCityMap extends PApplet {

	// We will use member variables, instead of local variables, to store the data
	// that the setUp and draw methods will need to access (as well as other methods)
	// You will use many of these variables, but the only one you should need to add
	// code to modify is countryQuakes, where you will store the number of earthquakes
	// per country.

	private boolean isQuakeClicked = false;
	private boolean isInLegend = false;
	// You can ignore this.  It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/** This is where to find the local tiles, for working without an Internet connection */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	/** Greater than or equal to this threshold is a moderate earthquake */
	public static final float THRESHOLD_MODERATE = 5;
	/** Greater than or equal to this threshold is a light earthquake */
	public static final float THRESHOLD_LIGHT = 4;

	/** Greater than or equal to this threshold is an intermediate depth */
	public static final float THRESHOLD_INTERMEDIATE = 70;
	/** Greater than or equal to this threshold is a deep depth */
	public static final float THRESHOLD_DEEP = 300;

	//feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city names and info and country names and info
	private String cityFile = "city-data.json";
	private String countryFile = "countries.geo.json";

	// The map
	private UnfoldingMap map;

	// Markers for each city
	private List<Marker> cityMarkers;
	// Markers for each earthquake
	private List<Marker> quakeMarkers;
	private List<Marker> routeList;
	// A List of country markers
	private List<Marker> countryMarkers;
	private List<Marker> airportMarkers;
	private List<Marker> airportList;
	HashMap<Integer, Location> airports;
	// NEW IN MODULE 5
	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	int mX,mY;
	float threatDist;
	public void setup() {
		// (1) Initializing canvas and map tiles
		size(1500, 700, OPENGL);
		if (offline) {
		    map = new UnfoldingMap(this, 200, 10, 1500, 800, new MBTilesMapProvider(mbTilesString));
		    earthquakesURL = "2.5_week.atom";  // The same feed, but saved August 7, 2015
		}
		else {
			map = new UnfoldingMap(this, 200, 10, 1500, 800, new Google.GoogleMapProvider());
			// IF YOU WANT TO TEST WITH A LOCAL FILE, uncomment the next line
		    //earthquakesURL = "2.5_week.atom";
		}
		MapUtils.createDefaultEventDispatcher(this, map);

	
		airports = new HashMap<Integer, Location>();
		routeList = new ArrayList<Marker>();
		// (2) Reading in earthquake data and geometric properties
	    //     STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		//     STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for(Feature city : cities) {
		  cityMarkers.add(new CityMarker(city));
		}

		//     STEP 3: read in earthquake RSS feed
	    List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
	    quakeMarkers = new ArrayList<Marker>();

	    for(PointFeature feature : earthquakes) {
		  //check if LandQuake
		  if(isLand(feature)) {
		    quakeMarkers.add(new LandQuakeMarker(feature));
		  }
		  // OceanQuakes
		  else {
		    quakeMarkers.add(new OceanQuakeMarker(feature));
		  }
	    }

	    List<PointFeature> features = ParseFeed.parseAirports(this, "airports.dat");

		airportMarkers = new ArrayList<Marker>();

		// create markers from features
		for (PointFeature feature : features) {
			airportMarkers.add(new AirportMarker(feature));
			airports.put(Integer.parseInt(feature.getId()), feature.getLocation());
		}
		
		List<ShapeFeature> routes = ParseFeed.parseRoutes(this, "routes.dat");
		routeList = new ArrayList<Marker>();
		for(ShapeFeature route : routes) {
			
			// get source and destination airportIds
			int source = Integer.parseInt((String)route.getProperty("source"));
			int dest = Integer.parseInt((String)route.getProperty("destination"));
			
			// get locations for airports on route
			if(airports.containsKey(source) && airports.containsKey(dest)) {
				route.addLocation(airports.get(source));
				route.addLocation(airports.get(dest));
			}
			
			SimpleLinesMarker sl = new SimpleLinesMarker(route.getLocations(), route.getProperties());
		
			routeList.add(sl);
		}
		
		map.addMarkers(routeList);
		for(Marker marker:routeList)
			marker.setHidden(true);
		
	    // could be used for debugging
	    //printQuakes();

	    // (3) Add markers to map
	    //     NOTE: Country markers are not added to the map.  They are used
	    //           for their geometric properties
	    map.addMarkers(quakeMarkers);
	    map.addMarkers(cityMarkers);
	    map.addMarkers(airportMarkers);
	    airportList = new ArrayList<Marker>();
	    airports = new HashMap<Integer, Location>();
	    for (Marker marker : airportMarkers)
			marker.setHidden(true);


	}  // End setup

   /**
    * renders the map
    */
	public void draw() {
		background(0);
		map.draw();
		addKey();

	}


	private void sortAndPrint(int numToPrint){
		Object[] arrayMarker = quakeMarkers.toArray();
		Arrays.sort(arrayMarker);
		if(numToPrint > arrayMarker.length) numToPrint = arrayMarker.length;
		int revNum = arrayMarker.length-numToPrint;
		for(int i=arrayMarker.length-1;i>=revNum;i--) System.out.println(arrayMarker[i].toString()+" ");
	}
	// and then call that method from setUp

	/** Event handler that gets called automatically when the
	 * mouse moves.
	 */
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;

		}
		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
		selectMarkerIfHover(airportMarkers);
		//loop();
	}

	// If there is a marker selected
	private void selectMarkerIfHover(List<Marker> markers)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}

		for (Marker m : markers)
		{
			CommonMarker marker = (CommonMarker)m;
			if (marker.isInside(map,  mouseX, mouseY)) {
				lastSelected = marker;
				marker.setSelected(true);
				return;
			}
		}
	}

	/** The event handler for mouse clicks
	 * It will display an earthquake and its threat circle of cities
	 * Or if a city is clicked, it will display all the earthquakes
	 * where the city is in the threat circle
	 */
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null && !isQuakeClicked) {
			unhideMarkers();
			for (Marker marker : airportMarkers){
				marker.setHidden(true);
			}
			for(Marker route: routeList)
				route.setHidden(true);
			lastClicked = null;
		}
		else if(isQuakeClicked){
			boolean isButtonClicked = checkButtonClick();
			if(!isButtonClicked){
				unhideMarkers();
				for (Marker marker : airportMarkers){
					marker.setHidden(true);
				}
				for(Marker route: routeList)
					route.setHidden(true);
				isQuakeClicked = false;
			}
		}

		else if (lastClicked == null || isInLegend)
		{
			checkEarthquakesForClick();
			if (lastClicked == null) {
				checkCitiesForClick();
			}
			if(lastClicked == null){
				checkLegendForClick();
			}

		}
	}

	private boolean checkButtonClick() {
		int xbase = 25;
		int ybase = 50;

		if(mouseX>=xbase+18 && mouseX<=xbase+138){
			if(mouseY>=ybase+260 && mouseY<=ybase+285){
				callDatabase();
				return true;
			}
		}
		if(mouseX>=xbase+35 && mouseX<=xbase+115){
			if(mouseY>=ybase+300 && mouseY<=ybase+320){
				callDonation();
				return true;
			}
		}
		return false;
	}

/**
 * calls an object of Donate2 to class which implements the donation service  
 */
	private void callDonation() {
		new Donate2();

	}

/**
 * calls the database which stores the missing person's information and also implements its searching.
 */
	private void callDatabase() {
		//System.out.println("DB pending");
		new MissingInterface(lastClicked.getStringProperty("name")).setVisible(true);
	}


/**
 * helper function to display the airports and their routes.
 * @param markerSelected denotes the currently selected marker.
 */
	private void showAirports(EarthquakeMarker markerSelected){
		Location location = markerSelected.getLocation();
		double threatDistance = ((EarthquakeMarker) markerSelected).threatCircle();
		airportList.clear();
		for (Marker marker : airportMarkers) {
			if (marker.getDistanceTo(location) > threatDistance) {
				marker.setHidden(true);
				marker.setSelected(false);

			} else {
				airportList.add(marker);
				marker.setHidden(false);
			}
			
		}

		
		for(Marker m:airportList){
			PointFeature feature = ((AirportMarker) m).getFeature();
			for(Marker route:routeList){
				List<Location> locations = ((SimpleLinesMarker)route).getLocations();
				//System.out.println(m.getLocation());
				if(locations.indexOf(m.getLocation())!=-1)
					{
					route.setHidden(false);
					//System.out.println(route.getStringProperty("name")+" "+locations.indexOf(m.getLocation()));
					for(Marker m1:airportMarkers)
						if(locations.indexOf(m1.getLocation())!=-1)
							m1.setHidden(false);
					}
			}
			
		}

	}

	/**
	 *  Helper method that will check if a city marker was clicked on
	 *   and respond appropriately
	 */
	private void checkCitiesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected
		for (Marker marker : cityMarkers) {
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (CommonMarker)marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : cityMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : quakeMarkers) {
					EarthquakeMarker quakeMarker = (EarthquakeMarker)mhide;
					if (quakeMarker.getDistanceTo(marker.getLocation())
							> quakeMarker.threatCircle()) {
						quakeMarker.setHidden(true);
					}
				}
				return;
			}
		}
	}

	/** Helper method that will check if an earthquake marker was clicked on and respond appropriately*/
	private void checkEarthquakesForClick()
	{
		if (lastClicked != null) return;
		// Loop over the earthquake markers to see if one of them is selected

		for (Marker m : quakeMarkers) {
			EarthquakeMarker marker = (EarthquakeMarker)m;
			if (!marker.isHidden() && marker.isInside(map, mouseX, mouseY)) {
				lastClicked = marker;
				// Hide all the other earthquakes and hide
				for (Marker mhide : quakeMarkers) {
					if (mhide != lastClicked) {
						mhide.setHidden(true);
					}
				}
				for (Marker mhide : cityMarkers) {
					if (mhide.getDistanceTo(marker.getLocation())
							> marker.threatCircle()) {
						mhide.setHidden(true);
					}
				}
				mX=mouseX;mY=mouseY;
				showAirports(marker);
				threatDist=(float)((EarthquakeMarker)marker).threatCircle();
				isQuakeClicked = true;
				return;
			} 
			
		}
	}
	/**
	 * Helper function to check if legend is clicked.
	 */

	private void checkLegendForClick(){
		if(lastClicked!=null) return;

		int xbase = 25; int ybase = 50;
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase +50;

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=tri_ybase-5 && mouseY<=tri_ybase+7)
				{
				hideNonCity();
				isInLegend = true;
				lastClicked = null;
				return;
				}

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=ybase+70 && mouseY<=ybase+82)
				{
				hideNonLand();
				isInLegend = true;
				lastClicked = null;
				return;
				}

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=ybase+140 && mouseY<=ybase+152)
				{
				showShallow();
				isInLegend = true;
				lastClicked = null;
				return;
				}

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=ybase+160 && mouseY<=ybase+172)
				{
				showInter();
				isInLegend = true;
				lastClicked = null;
				return;
				}

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=ybase+180 && mouseY<=ybase+192)
				{
				showDeep();
				isInLegend = true;
				lastClicked = null;
				return;
				}

		if(mouseX>=tri_xbase-10 && mouseX<=tri_xbase+70)
			if(mouseY>=ybase+200 && mouseY<=ybase+212)
				{
				showPastHour();
				isInLegend = true;
				lastClicked = null;
				return;
				}
		isInLegend = false;
		lastClicked = (CommonMarker) cityMarkers.get(0);

	}

	public void hideNonCity(){
		for(Marker marker : quakeMarkers){
			marker.setHidden(true);
		}
	}

	public void hideNonLand(){
		//System.out.println("HAHA");
		for(Marker marker: cityMarkers){
			marker.setHidden(true);
		}
		for(Marker marker: quakeMarkers){
			if(!((EarthquakeMarker) marker).isOnLand()) marker.setHidden(true);
		}

	}

	public void showShallow(){
		for(Marker marker: cityMarkers)
			marker.setHidden(true);
		for(Marker marker: quakeMarkers){
			float depth = ((EarthquakeMarker) marker).getDepth();
			if(!(depth<THRESHOLD_INTERMEDIATE)) marker.setHidden(true);
		}
	}

	public void showInter(){
		for(Marker marker: cityMarkers)
			marker.setHidden(true);
		for(Marker marker: quakeMarkers){
			float depth = ((EarthquakeMarker) marker).getDepth();
			if((depth<THRESHOLD_INTERMEDIATE) || depth>THRESHOLD_DEEP) marker.setHidden(true);
		}
	}

	public void showDeep(){
		for(Marker marker: cityMarkers)
			marker.setHidden(true);
		for(Marker marker: quakeMarkers){
			float depth = ((EarthquakeMarker) marker).getDepth();
			if(!(depth>THRESHOLD_DEEP)) marker.setHidden(true);
		}
	}

	public void showPastHour(){
		for(Marker marker: cityMarkers)
			marker.setHidden(true);
		for(Marker marker: quakeMarkers){
			String age = marker.getStringProperty("age");
			if (!("Past Hour".equals(age) || "Past Day".equals(age)))
				marker.setHidden(true);
		}
	}
	// loop over and unhide all markers
	private void unhideMarkers() {
		hideButton();
		for(Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for(Marker marker : cityMarkers) {
			marker.setHidden(false);
		}
	}

	// helper method to draw key in GUI
	private void addKey() {
	
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 150, 250);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase+25, ybase+25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE,
				tri_ybase+CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase+50, ybase+70);
		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Magnitude", xbase+25, ybase+110);

		fill(255, 255, 255);
		ellipse(xbase+35,
				ybase+70,
				10,
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase+50, ybase+140);
		text("Intermediate", xbase+50, ybase+160);
		text("Deep", xbase+50, ybase+180);

		text("Past hour", xbase+50, ybase+200);

		text("Airports", xbase + 50, ybase + 217);

		fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);
		if(isQuakeClicked){
			stroke(240, 232, 14);

			fill(255,255,255);
			rect(xbase+18, ybase+260, 120, 25);

			fill(255, 255, 255);
			rect(xbase+35, ybase+300, 80, 20);

			fill(0, 0, 0);
			text("Victim Database", xbase+27, ybase+270);

			fill(0, 0, 0);
			text("Donation", xbase+45, ybase+309);
			
			float currentScale=UnfoldingMap.getScaleFromZoom(map.getZoom());
			int ratio=(int)(Math.log(currentScale)/Math.log(16));
			fill(255,0,0,63);
			ellipse(mX,mY,(int)((threatDist*ratio)/16),(int)(threatDist*ratio)/16);
		}
		else stroke(0);
		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);

		fill(200, 240);
		rect(xbase + 30, ybase + 215, 10, 10);

	}

	private void addButton(){

	}

	private void hideButton(){

	}
	// Checks whether this quake occurred on land.  If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true.  Notice that the helper method isInCountry will
	// set this "country" property already.  Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	
    // helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the earthquake feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use isInsideByLoc
		if(country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for(Marker marker : ((MultiMarker)country).getMarkers()) {

				// checking if inside
				if(((AbstractShapeMarker)marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if(((AbstractShapeMarker)country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

}
