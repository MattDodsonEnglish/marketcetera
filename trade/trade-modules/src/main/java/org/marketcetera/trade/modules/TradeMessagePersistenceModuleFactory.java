package org.marketcetera.trade.modules;

import org.marketcetera.module.Module;
import org.marketcetera.module.ModuleCreationException;
import org.marketcetera.module.ModuleFactory;
import org.marketcetera.module.ModuleURN;

/* $License$ */

/**
 * <code>ModuleFactory</code> implementation for persisting trade messages.
 * <p>
 * The factory has the following characteristics.
 * <table>
 * <tr><th>Provider URN:</th><td><code>metc:trade:trademessagepersistence</code></td></tr>
 * <tr><th>Cardinality:</th><td>Singleton</td></tr>
 * <tr><th>Instance URN:</th><td><code>metc:trade:trademessagepersistence:single</code></td></tr>
 * <tr><th>Auto-Instantiated:</th><td>Yes</td></tr>
 * <tr><th>Auto-Started:</th><td>Yes</td></tr>
 * <tr><th>Instantiation Arguments:</th><td>none</td></tr>
 * <tr><th>Module Type:</th><td>{@link TradeMessagePersistenceModule}</td></tr>
 * </table>
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class TradeMessagePersistenceModuleFactory
        extends ModuleFactory
{
    /**
     * Create a new TradeMessagePersistenceModuleFactory instance.
     */
    public TradeMessagePersistenceModuleFactory()
    {
        super(PROVIDER_URN,
              Messages.TRADE_MESSAGE_PERSISTENCE_PROVIDER_DESCRIPTION,
              false,
              true);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.module.ModuleFactory#create(java.lang.Object[])
     */
    @Override
    public Module create(Object... inParameters)
            throws ModuleCreationException
    {
        return new TradeMessagePersistenceModule(INSTANCE_URN);
    }
    /**
     * identifier for this URN
     */
    public static final String IDENTIFIER = "trademessagepersistence";
    /**
     * provider URN value
     */
    public static final ModuleURN PROVIDER_URN = new ModuleURN("metc:trade:"+IDENTIFIER);  //$NON-NLS-1$
    /**
     * instance URN value
     */
    public static final ModuleURN INSTANCE_URN = new ModuleURN(PROVIDER_URN,
                                                               "single");  //$NON-NLS-1$
}