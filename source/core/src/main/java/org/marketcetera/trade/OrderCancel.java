package org.marketcetera.trade;

import org.marketcetera.util.misc.ClassVersion;

/* $License$ */
/**
 * An order to cancel a previously placed order. Instances
 * of this order can be created via
 * {@link Factory#createOrderCancel(ExecutionReport)}.
 *
 * @author anshul@marketcetera.com
 * @version $Id$
 * @since $Release$
 */
@ClassVersion("$Id$") //$NON-NLS-1$
public interface OrderCancel extends RelatedOrder {
}