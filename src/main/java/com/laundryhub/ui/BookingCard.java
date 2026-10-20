package com.laundryhub.ui;

import com.laundryhub.model.Booking;
import com.laundryhub.model.JobStatus;
import com.laundryhub.model.Location;
import com.laundryhub.model.Order;
import com.laundryhub.model.RiderJob;
import com.laundryhub.service.RiderBoard;
import com.laundryhub.util.MoneyFormatter;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Booking card of screen R1: type badge, order no. + distance, customer, address, slot + kg,
 * payment strip, and the actions (Decline / Accept booking, or Start / Continue job).
 */
public class BookingCard extends Component {

    /** What the card is for. */
    public enum Mode { AVAILABLE, MINE }

    private final Booking booking;
    private final Mode mode;
    private final Location riderPosition;
    private final Runnable onPrimary;
    private final Runnable onDecline;
    private Button primary;

    /**
     * @param booking       job + order
     * @param mode          AVAILABLE (accept/decline) or MINE (open)
     * @param riderPosition rider GPS for the distance, may be null
     * @param onPrimary     Accept booking / Start job / Continue job
     * @param onDecline     Decline (AVAILABLE only), may be null
     */
    public BookingCard(Booking booking, Mode mode, Location riderPosition, Runnable onPrimary, Runnable onDecline) {
        this.booking = booking;
        this.mode = mode;
        this.riderPosition = riderPosition;
        this.onPrimary = onPrimary;
        this.onDecline = onDecline;
    }

    /** @return the main button (to show loading) */
    public Button getPrimaryButton() {
        getElement();
        return primary;
    }

    /**
     * @param job a rider job
     * @return its type badge (Pickup blue / Return delivery orange)
     */
    public static Badge typeBadge(RiderJob job) {
        return new Badge(job.getTypeLabel(), RiderJob.TYPE_PICKUP.equals(job.getType()) ? Badge.Tone.WASHING : Badge.Tone.OUT, true);
    }

    /**
     * @param order an order
     * @return address line, e.g. "Brgy. Poblacion · Blk 5 Lot 3"
     */
    public static String addressLine(Order order) {
        String pin = order.getLocation() == null ? "" : order.getLocation().getAddress();
        String details = order.getAddressDetails() == null ? "" : order.getAddressDetails();
        if (pin.isEmpty()) {
            return details.isEmpty() ? "No address given" : details;
        }
        return details.isEmpty() ? pin : pin + " · " + details;
    }

    @Override
    protected HTMLElement render() {
        RiderJob job = booking.getJob();
        Order order = booking.getOrder();
        String distance = RiderBoard.distanceLabel(booking, riderPosition);

        HTMLElement top = Dom.div("booking__top", typeBadge(job).getElement(),
                Dom.text("span", "booking__meta", order.getOrderNo() + (distance.isEmpty() ? "" : " · " + distance)));
        HTMLElement who = Dom.div("booking__who", Dom.icon(Icons.PIN, "booking__pin"),
                Dom.div("booking__text",
                        Dom.text("span", "booking__name", order.getCustomerName().isEmpty() ? "Customer" : order.getCustomerName()),
                        Dom.text("span", "booking__line", addressLine(order)),
                        Dom.text("span", "booking__line", order.getTimeSlot() + " · " + order.getKg() + " kg")));
        HTMLElement pay = Dom.div("booking__pay", Dom.text("span", "booking__pay-note", job.getPaymentNote()),
                Dom.text("span", "booking__pay-total", MoneyFormatter.format(order.getTotal())));

        HTMLElement card = Dom.div("card booking", top, who, pay);
        if (mode == Mode.AVAILABLE) {
            Button decline = new Button("Decline", Button.Variant.OUTLINE, onDecline);
            primary = new Button("Accept booking", Button.Variant.PRIMARY, onPrimary);
            card.appendChild(Dom.div("booking__actions", decline.getElement(), primary.getElement()));
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            card.appendChild(Dom.div("booking__done", new Badge("Completed", Badge.Tone.DONE, true).getElement()));
        } else {
            primary = new Button(job.getStatus() == JobStatus.IN_PROGRESS ? "Continue job" : "Start job",
                    Button.Variant.DARK, onPrimary).block(true);
            primary.getElement().getClassList().add("btn--compact");
            card.appendChild(primary.getElement());
        }
        if (primary != null && mode == Mode.AVAILABLE) {
            primary.getElement().getClassList().add("btn--compact");
        }
        return card;
    }
}
