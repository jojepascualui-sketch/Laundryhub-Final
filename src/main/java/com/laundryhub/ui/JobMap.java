package com.laundryhub.ui;

import com.laundryhub.model.Location;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Map of an active rider job (R2/R3): rider, customer pin and shop markers with a line from the
 * rider to the current destination. Falls back to the drawn map when Google Maps is unavailable.
 */
public class JobMap extends Component {

    private static final int PADDING = 56;
    private static final int ZOOM = 15;

    private final Location shop;
    private final Location customer;
    private Location rider;
    private Location destination;

    private HTMLElement canvas;
    private GoogleMaps.GMap map;
    private GoogleMaps.Marker riderMarker;
    private GoogleMaps.Overlay line;
    private boolean fitted;

    /**
     * @param shop        shop location, may be null
     * @param customer    customer pin, may be null
     * @param rider       rider GPS, may be null
     * @param destination where the rider is heading, may be null
     */
    public JobMap(Location shop, Location customer, Location rider, Location destination) {
        this.shop = shop;
        this.customer = customer;
        this.rider = rider;
        this.destination = destination;
    }

    /**
     * @param position new rider position
     */
    public void setRider(Location position) {
        rider = position;
        update();
    }

    /**
     * @param target new destination (changes with the job step)
     */
    public void setDestination(Location target) {
        destination = target;
        update();
    }

    @Override
    protected HTMLElement render() {
        canvas = Dom.div("job-map__canvas map-box--loading");
        Window.setTimeout(() -> GoogleMaps.whenReady(this::init, this::fallback), 0);
        return Dom.div("job-map", canvas);
    }

    private void init() {
        canvas.getClassList().remove("map-box--loading");
        Location center = destination != null ? destination : customer != null ? customer : shop;
        if (center == null) {
            fallback();
            return;
        }
        map = GoogleMaps.createMap(canvas, center.getLat(), center.getLng(), ZOOM);
        GoogleMaps.watchAuthFailure(() -> {
            map = null;
            riderMarker = null;
            line = null;
            fallback();
        });
        if (shop != null) {
            GoogleMaps.createMarker(map, shop.getLat(), shop.getLng(), GoogleMaps.MarkerStyle.SHOP, false, "Laundry HUB");
        }
        if (customer != null) {
            GoogleMaps.createMarker(map, customer.getLat(), customer.getLng(), GoogleMaps.MarkerStyle.PIN, false, "Customer");
        }
        update();
    }

    private void update() {
        if (map == null) {
            return;
        }
        if (rider != null) {
            if (riderMarker == null) {
                riderMarker = GoogleMaps.createMarker(map, rider.getLat(), rider.getLng(),
                        GoogleMaps.MarkerStyle.RIDER, false, "You");
            } else {
                riderMarker.setPosition(GoogleMaps.latLng(rider.getLat(), rider.getLng()));
            }
        }
        if (line != null) {
            line.setMap(null);
            line = null;
        }
        if (rider != null && destination != null) {
            double[] lats = {rider.getLat(), destination.getLat()};
            double[] lngs = {rider.getLng(), destination.getLng()};
            line = GoogleMaps.createDashedLine(map, lats, lngs);
            if (!fitted) {
                GoogleMaps.fitPoints(map, lats, lngs, PADDING);
                fitted = true;
            }
        } else if (destination != null) {
            map.panTo(GoogleMaps.latLng(destination.getLat(), destination.getLng()));
        }
    }

    private void fallback() {
        Dom.clear(canvas);
        canvas.getClassList().remove("map-box--loading");
        canvas.getClassList().add("map-box__art");
        canvas.setInnerHTML(MapArt.SVG);
    }
}
