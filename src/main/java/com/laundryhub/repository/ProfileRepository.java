package com.laundryhub.repository;

import com.laundryhub.model.User;
import com.laundryhub.util.Callback;

/**
 * Accounts (profiles + riders tables).
 * OOP: Abstraction — pages depend on this interface, not on Supabase.
 */
public interface ProfileRepository {

    /**
     * Loads a user with the right subclass for their role (riders include plate and duty).
     *
     * @param userId   profile id
     * @param callback receives the user, or an error when the profile is missing
     */
    void findUser(String userId, Callback<User> callback);

    /**
     * Updates the signed-in user's own name and mobile.
     *
     * @param userId   own profile id
     * @param fullName full name
     * @param mobile   mobile 09XXXXXXXXX, may be null
     * @param callback called when saved
     */
    void updateOwnProfile(String userId, String fullName, String mobile, Callback<Void> callback);
}
