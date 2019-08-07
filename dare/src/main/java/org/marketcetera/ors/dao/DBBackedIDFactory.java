package org.marketcetera.ors.dao;

import java.sql.SQLException;

import org.marketcetera.core.ClassVersion;
import org.marketcetera.core.ExternalIDFactory;
import org.marketcetera.core.NoMoreIDsException;

/**
 * @author toli
 * @version $Id$
 */

@ClassVersion("$Id$")
public abstract class DBBackedIDFactory
        extends ExternalIDFactory
{
    protected DBBackedIDFactory(String prefix) {
        super(prefix);
    }

    /** Lock the table to prevent concurrent access with {@link java.sql.ResultSet#CONCUR_UPDATABLE} */
    public void grabIDs() throws NoMoreIDsException {

        factoryValidityCheck();

        try {
            performIDRequest();
        } catch (SQLException e) {
            throw new NoMoreIDsException(e);
	    } catch (Throwable t){
	    	throw new NoMoreIDsException(t);
		}
    }

    public void init() throws ClassNotFoundException, NoMoreIDsException {
        // no-op
    }

    /** Intended to be overwritten by subclasses
     * Extra validity check before performing the request.
     * Checks the factory state, if inconsistent, throws an exception
     * @throws NoMoreIDsException if no more ids are available
     */
    protected void factoryValidityCheck() throws NoMoreIDsException
    {
        // do nothing
    }

    /** Peforms the necessary cleanup after the request is done, whether or not it succeeds or fails. */
    protected void postRequestCleanup()
    {
        // do nothing
    }

    /** Helper function intended to be overwritten by subclasses.
     * This is where the real request for IDs happens
     * @throws Exception if the request cannot be completed
     */
    protected abstract void performIDRequest() throws Exception;

}
