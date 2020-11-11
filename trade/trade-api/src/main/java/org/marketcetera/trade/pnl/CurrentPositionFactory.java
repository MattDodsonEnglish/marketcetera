//
// this file is automatically generated
//
package org.marketcetera.trade.pnl;

/* $License$ */

/**
 * Creates new {@link CurrentPosition} objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public interface CurrentPositionFactory
        extends org.marketcetera.core.Factory<CurrentPosition>
{
    /**
     * Create a new <code>CurrentPosition</code> instance.
     *
     * @return a <code>CurrentPosition</code> value
     */
    @Override
    CurrentPosition create();
    /**
     * Create a new <code>CurrentPosition</code> instance from the given object.
     *
     * @param inObject a <code>CurrentPosition</code> value
     * @return a <code>CurrentPosition</code> value
     */
    @Override
    CurrentPosition create(CurrentPosition inCurrentPosition);
}
