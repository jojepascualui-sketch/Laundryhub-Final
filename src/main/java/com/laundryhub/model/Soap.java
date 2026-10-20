package com.laundryhub.model;

import com.laundryhub.exception.ValidationException;
import com.laundryhub.util.MoneyFormatter;

/**
 * A fabric soap option with its add-on price per kg (centavos).
 */
public final class Soap {

    private final long id;
    private final String name;
    private final String description;
    private final long addOnPerKg;
    private final boolean available;

    /**
     * @param id          database id
     * @param name        display name, required
     * @param description short description
     * @param addOnPerKg  extra price per kg in centavos (&ge; 0)
     * @param available   shown to customers when true
     */
    public Soap(long id, String name, String description, long addOnPerKg, boolean available) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("A soap needs a name.");
        }
        if (addOnPerKg < 0) {
            throw new ValidationException("The add-on price cannot be negative.");
        }
        this.id = id;
        this.name = name.trim();
        this.description = description == null ? "" : description;
        this.addOnPerKg = addOnPerKg;
        this.available = available;
    }

    /** @return database id */
    public long getId() {
        return id;
    }

    /** @return display name */
    public String getName() {
        return name;
    }

    /** @return description */
    public String getDescription() {
        return description;
    }

    /** @return add-on per kg in centavos */
    public long getAddOnPerKg() {
        return addOnPerKg;
    }

    /** @return true when customers can choose it */
    public boolean isAvailable() {
        return available;
    }

    /** @return "Included" for free soaps, otherwise "+ ₱5 /kg" (as on the soap cards) */
    public String getPriceLabel() {
        return addOnPerKg == 0 ? "Included" : "+ " + MoneyFormatter.formatShort(addOnPerKg) + " /kg";
    }
}
