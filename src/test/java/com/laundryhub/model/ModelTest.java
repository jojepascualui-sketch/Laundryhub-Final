package com.laundryhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laundryhub.exception.InvalidPaymentException;
import com.laundryhub.exception.InvalidWeightException;
import com.laundryhub.exception.ValidationException;
import org.junit.jupiter.api.Test;

class ModelTest {

    private static final Soap STANDARD = new Soap(1, "Standard", "Everyday clean", 0, true);

    // ---- LaundryLoad ---------------------------------------------------------

    @Test
    void loadBelowMinimumIsChargedAsMinimum() {
        LaundryLoad load = new LaundryLoad(7, STANDARD);
        assertEquals(LaundryLoad.MIN_KG, load.getChargedKg(LaundryLoad.MIN_KG));
        assertTrue(load.isBelowMinimum(LaundryLoad.MIN_KG));
    }

    @Test
    void zeroOrHugeWeightThrows() {
        assertThrows(InvalidWeightException.class, () -> new LaundryLoad(0, STANDARD));
        assertThrows(InvalidWeightException.class, () -> new LaundryLoad(-3, STANDARD));
        assertThrows(InvalidWeightException.class, () -> new LaundryLoad(LaundryLoad.MAX_KG + 1, STANDARD));
        assertThrows(InvalidWeightException.class, () -> new LaundryLoad(8, null));
    }

    // ---- Users (polymorphism) -----------------------------------------------

    @Test
    void eachRoleHasItsOwnHome() {
        assertEquals("#/home", User.create(Role.CUSTOMER, "u1", "Ana Reyes", null, null, true, null).getHomeRoute());
        assertEquals("#/rider", User.create(Role.RIDER, "u2", "Rider Mendoza", null, null, true, null).getHomeRoute());
        assertEquals("#/admin", User.create(Role.ADMIN, "u3", "Admin", null, null, true, "Owner").getHomeRoute());
        assertEquals("#/admin", User.create(Role.STAFF, "u4", "Sam", null, null, true, null).getHomeRoute());
    }

    @Test
    void staffIsAnAdminWithoutSettingsRights() {
        User staff = User.create(Role.STAFF, "u4", "Sam", null, null, true, "Counter staff");
        assertInstanceOf(Admin.class, staff);
        assertFalse(((Admin) staff).canManageSettings());
        assertTrue(((Admin) User.create(Role.ADMIN, "u3", "Admin", null, null, true, null)).canManageSettings());
        assertEquals("Counter staff", staff.getRoleLabel());
    }

    @Test
    void userNeedsId() {
        assertThrows(ValidationException.class, () -> new Customer("", "x", null, null, true));
    }

    @Test
    void initialsAndFirstName() {
        User u = new Customer("u1", "Rider Mendoza", null, null, true);
        assertEquals("RM", u.getInitials());
        assertEquals("Rider", u.getFirstName());
        assertEquals("AD", new Admin("a", "Admin", null, null, true, null).getInitials());
    }

    @Test
    void roleFromUnknownCodeIsCustomer() {
        assertEquals(Role.CUSTOMER, Role.fromCode("HACKER"));
        assertEquals(Role.RIDER, Role.fromCode("RIDER"));
    }

    // ---- Payments ------------------------------------------------------------

    @Test
    void gcashReferenceMustBe13Digits() {
        assertEquals("1234567890123", new GCashPayment("1234 5678 90123", null).getReference());
        assertThrows(InvalidPaymentException.class, () -> new GCashPayment("123", null));
        assertThrows(InvalidPaymentException.class, () -> new GCashPayment("12345678901ab", null));
        assertThrows(InvalidPaymentException.class, () -> new GCashPayment("1234567890123", "12345"));
        assertFalse(new GCashPayment(null, null).isComplete());
    }

    @Test
    void cashDescriptionDependsOnService() {
        CashPayment cash = new CashPayment();
        assertEquals("Pay the rider upon delivery", cash.getDescription(PickupDeliveryService.INSTANCE));
        assertEquals("Pay at the counter", cash.getDescription(WalkInService.INSTANCE));
        assertEquals(PaymentStatus.UNPAID, cash.getInitialStatus());
        assertEquals(PaymentStatus.PENDING_VERIFICATION, new GCashPayment(null, null).getInitialStatus());
    }

    @Test
    void paymentFromCode() {
        assertInstanceOf(CashPayment.class, PaymentMethod.fromCode("CASH"));
        assertInstanceOf(GCashPayment.class, PaymentMethod.fromCode("GCASH"));
        assertNull(PaymentMethod.fromCode("BITCOIN"));
    }

    // ---- Services ------------------------------------------------------------

    @Test
    void serviceTypesFromDesign() {
        ShopSettings s = ShopSettings.defaults();
        assertEquals("No extra fee", WalkInService.INSTANCE.getFeeLabel(s));
        assertEquals("+ ₱60 delivery fee", PickupDeliveryService.INSTANCE.getFeeLabel(s));
        assertEquals("Today, 2–4 PM", PickupDeliveryService.INSTANCE.getTimeSlots().get(0));
        assertEquals("Tomorrow, afternoon", WalkInService.INSTANCE.getTimeSlots().get(3));
        assertTrue(PickupDeliveryService.INSTANCE.requiresLocation());
        assertEquals(WalkInService.INSTANCE, ServiceType.fromCode("WALK_IN"));
        assertNull(ServiceType.fromCode("x"));
    }

    @Test
    void soapPriceLabels() {
        assertEquals("Included", STANDARD.getPriceLabel());
        assertEquals("+ ₱5 /kg", new Soap(2, "Hypoallergenic", "", 500, true).getPriceLabel());
        assertThrows(ValidationException.class, () -> new Soap(9, " ", "", 0, true));
        assertThrows(ValidationException.class, () -> new Soap(9, "x", "", -1, true));
    }

    // ---- Rider jobs ------------------------------------------------------------

    @Test
    void pickupJobStepsAndButtons() {
        RiderJob job = RiderJob.create("PICKUP", 1, 1, JobStatus.ACCEPTED, 0, "r", true);
        assertInstanceOf(PickupJob.class, job);
        assertEquals(4, job.getTotalSteps());
        assertEquals("Start trip", job.getNextActionLabel());
        assertEquals(RiderJob.StepState.CURRENT, job.getStepState(0));
        assertEquals("Cash on return · order total", job.getPaymentNote());
    }

    @Test
    void cashDeliveryHasCashStep() {
        RiderJob cash = RiderJob.create("RETURN_DELIVERY", 2, 1, JobStatus.IN_PROGRESS, 3, "r", true);
        RiderJob gcash = RiderJob.create("RETURN_DELIVERY", 3, 1, JobStatus.IN_PROGRESS, 3, "r", false);
        assertEquals(5, cash.getTotalSteps());
        assertEquals(4, gcash.getTotalSteps());
        assertEquals("Cash received", cash.getNextActionLabel());
        assertEquals("Mark as delivered", gcash.getNextActionLabel());
        assertEquals("Collect cash", cash.getPaymentNote());
        assertEquals("Paid via GCash", gcash.getPaymentNote());
        assertEquals(RiderJob.StepState.DONE, cash.getStepState(2));
        assertEquals(RiderJob.StepState.UPCOMING, cash.getStepState(4));
    }

    @Test
    void finishedJobHasNoButton() {
        RiderJob done = RiderJob.create("PICKUP", 1, 1, JobStatus.COMPLETED, 4, "r", false);
        assertTrue(done.isDone());
        assertEquals("", done.getNextActionLabel());
        assertThrows(ValidationException.class, () -> RiderJob.create("TELEPORT", 1, 1, JobStatus.ACCEPTED, 0, "r", false));
    }

    // ---- Location, settings, session ----------------------------------------

    @Test
    void locationValidatesAndMeasures() {
        assertThrows(ValidationException.class, () -> new Location(91, 0, null));
        Location shop = new Location(14.5896, 120.9811, "Shop");
        Location near = new Location(14.5935, 120.9850, "Near");
        assertTrue(shop.distanceKmTo(near) < 1);
        assertTrue(new ShopSettings(3000, 8, 6000, 5, shop, "", null, null, true, true).isWithinDeliveryArea(near));
        assertFalse(new ShopSettings(3000, 8, 6000, 5, shop, "", null, null, true, true)
                .isWithinDeliveryArea(new Location(15.5, 121.5, "Far")));
    }

    @Test
    void shopSettingsValidate() {
        assertThrows(ValidationException.class, () -> new ShopSettings(0, 8, 6000, 5, null, "", null, null, true, true));
        assertThrows(ValidationException.class, () -> new ShopSettings(3000, 0, 6000, 5, null, "", null, null, true, true));
        assertThrows(ValidationException.class, () -> new ShopSettings(3000, 8, -1, 5, null, "", null, null, true, true));
    }

    @Test
    void sessionRefreshWindow() {
        AuthSession s = new AuthSession("a", "r", 1000, "u", null);
        assertFalse(s.needsRefresh(900));
        assertTrue(s.needsRefresh(950));
        assertThrows(ValidationException.class, () -> new AuthSession("", "r", 1, "u", null));
    }

    @Test
    void statusLabels() {
        assertEquals("Out for delivery", OrderStatus.fromCode("OUT_FOR_DELIVERY").getLabel());
        assertEquals("Rejected — please resend", PaymentStatus.fromCode("REJECTED").getCustomerLabel());
        assertEquals(OrderStatus.PENDING, OrderStatus.fromCode(null));
    }
}
