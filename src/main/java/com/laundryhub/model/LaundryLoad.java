package com.laundryhub.model;

import com.laundryhub.exception.InvalidWeightException;

/**
 * The weight of one order and the chosen soap.
 * Below {@link #MIN_KG} the load is still accepted but charged as {@link #MIN_KG}.
 */
public final class LaundryLoad {

    /** Minimum charged weight in kg (the shop setting can change it; this is the default). */
    public static final int MIN_KG = 8;
    /** Upper limit to catch typing mistakes. */
    public static final int MAX_KG = 200;

    private final int kg;
    private final Soap soap;

    /**
     * @param kg   weight in whole kilograms (1..MAX_KG)
     * @param soap chosen soap, required
     * @throws InvalidWeightException when the weight is not usable
     */
    public LaundryLoad(int kg, Soap soap) {
        if (kg <= 0) {
            throw new InvalidWeightException("Enter the laundry weight in kg.");
        }
        if (kg > MAX_KG) {
            throw new InvalidWeightException("That is too heavy for one order (max " + MAX_KG + " kg).");
        }
        if (soap == null) {
            throw new InvalidWeightException("Choose a fabric soap.");
        }
        this.kg = kg;
        this.soap = soap;
    }

    /** @return weight entered by the customer */
    public int getKg() {
        return kg;
    }

    /** @return chosen soap */
    public Soap getSoap() {
        return soap;
    }

    /**
     * @param minKg the shop's minimum load
     * @return weight that is charged: never below the minimum
     */
    public int getChargedKg(int minKg) {
        return Math.max(kg, minKg);
    }

    /**
     * @param minKg the shop's minimum load
     * @return true when the customer pays for more than they bring
     */
    public boolean isBelowMinimum(int minKg) {
        return kg < minKg;
    }
}
