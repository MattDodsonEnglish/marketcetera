//
// this file is automatically generated
//
package org.marketcetera.strategy;

/* $License$ */

/**
 * Represents a message emitted by a strategy.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public interface StrategyMessage
        extends org.marketcetera.strategy.HasStrategyInstance
{
    /**
     * Get the messageTimestamp value.
     *
     * @return a <code>java.util.Date</code> value
     */
    java.util.Date getMessageTimestamp();
    /**
     * Set the messageTimestamp value.
     *
     * @param inMessageTimestamp a <code>java.util.Date</code> value
     */
    void setMessageTimestamp(java.util.Date inMessageTimestamp);
    /**
     * Get the severity value.
     *
     * @return an <code>org.marketcetera.core.notifications.INotification.Severity</code> value
     */
    org.marketcetera.core.notifications.INotification.Severity getSeverity();
    /**
     * Set the severity value.
     *
     * @param inSeverity an <code>org.marketcetera.core.notifications.INotification.Severity</code> value
     */
    void setSeverity(org.marketcetera.core.notifications.INotification.Severity inSeverity);
    /**
     * Get the message value.
     *
     * @return a <code>String</code> value
     */
    String getMessage();
    /**
     * Set the message value.
     *
     * @param inMessage a <code>String</code> value
     */
    void setMessage(String inMessage);
}
