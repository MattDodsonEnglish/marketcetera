package org.marketcetera.trade.service;

import org.marketcetera.brokers.Broker;
import org.marketcetera.trade.Order;
import org.marketcetera.trade.TradeMessage;

import quickfix.Message;

/* $License$ */

/**
 * Provides trade services.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public interface TradeService
{
    /**
     * Select a broker for the given order.
     *
     * @param inOrder an <code>Order</code> value
     * @return a <code>Broker</code> value
     * @throws NoBrokerSelected if a broker could not be determined
     */
    Broker selectBroker(Order inOrder);
    /**
     * Convert the given order into a FIX message targeted to the given broker.
     *
     * @param inOrder an <code>Order</code> value
     * @param inBroker a <code>Broker</code> value
     * @return a <code>Message</code> value
     * @throws BrokerUnavailable if the broker is unavailable or unknown
     * @throws OrderIntercepted if the order should not be sent on in the data flow
     */
    Message convertOrder(Order inOrder,
                         Broker inBroker);
    /**
     * Convert the given message from the given broker to a <code>TradeMessage</code>.
     *
     * @param inMessage a <code>Message</code> value
     * @param inBroker a <code>Broker</code> value
     * @return a <code>TradeMessage</code> value
     * @throws OrderIntercepted if the message should not be sent on in the data flow
     * @throws MessageCreationException if the message could not be converted
     */
    TradeMessage convertResponse(Message inMessage,
                                 Broker inBroker);
}