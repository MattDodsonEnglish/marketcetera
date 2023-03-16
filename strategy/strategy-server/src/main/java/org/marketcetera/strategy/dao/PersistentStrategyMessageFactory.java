//
// this file is automatically generated
//
package org.marketcetera.strategy.dao;

/* $License$ */

/**
 * Creates new {@link PersistentStrategyMessage} objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class PersistentStrategyMessageFactory
        implements org.marketcetera.strategy.StrategyMessageFactory
{
    /**
     * Create a new <code>org.marketcetera.strategy.dao.PersistentStrategyMessage</code> instance.
     *
     * @return a <code>org.marketcetera.strategy.dao.PersistentStrategyMessage</code> value
     */
    @Override
    public org.marketcetera.strategy.dao.PersistentStrategyMessage create()
    {
        return new org.marketcetera.strategy.dao.PersistentStrategyMessage();
    }
    /**
     * Create a new <code>org.marketcetera.strategy.dao.PersistentStrategyMessage</code> instance from the given object.
     *
     * @param inStrategyMessage an <code>org.marketcetera.strategy.dao.PersistentStrategyMessage</code> value
     * @return an <code>org.marketcetera.strategy.dao.PersistentStrategyMessage</code> value
     */
    @Override
    public org.marketcetera.strategy.dao.PersistentStrategyMessage create(org.marketcetera.strategy.StrategyMessage inPersistentStrategyMessage)
    {
        return new org.marketcetera.strategy.dao.PersistentStrategyMessage(inPersistentStrategyMessage);
    }
}
