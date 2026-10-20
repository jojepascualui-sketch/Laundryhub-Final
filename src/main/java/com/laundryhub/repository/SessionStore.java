package com.laundryhub.repository;

/**
 * Keeps the login session between page loads.
 *
 * <p>OOP: Abstraction — the browser version uses localStorage/sessionStorage; tests use memory.
 */
public interface SessionStore {

    /** @return the saved session JSON, or null */
    String load();

    /**
     * @param sessionJson session as JSON
     * @param remember    true to keep it after the browser closes ("Remember me")
     */
    void save(String sessionJson, boolean remember);

    /** Forgets the session. */
    void clear();
}
