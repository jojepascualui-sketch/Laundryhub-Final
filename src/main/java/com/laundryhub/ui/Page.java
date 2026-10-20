package com.laundryhub.ui;

import com.laundryhub.model.Role;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A full screen reached through a hash route.
 *
 * <p>OOP: Inheritance — a Page is a Component with a title and an access rule.
 */
public abstract class Page extends Component {

    /** @return browser tab title (without the app name) */
    public abstract String getTitle();

    /**
     * Who may open this page. The router sends everyone else to sign in or to their own home.
     *
     * @return allowed roles; empty set = anyone, even signed out
     */
    public Set<Role> getAllowedRoles() {
        return Collections.emptySet();
    }

    /**
     * Helper for subclasses.
     *
     * @param first first role
     * @param rest  more roles
     * @return a set of roles
     */
    protected static Set<Role> roles(Role first, Role... rest) {
        return EnumSet.of(first, rest);
    }
}
