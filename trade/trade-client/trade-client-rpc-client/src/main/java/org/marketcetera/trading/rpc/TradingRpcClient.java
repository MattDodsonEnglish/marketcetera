package org.marketcetera.trading.rpc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.BrokerStatusListener;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.brokers.ClusteredBrokerStatus;
import org.marketcetera.brokers.ClusteredBrokersStatus;
import org.marketcetera.core.PlatformServices;
import org.marketcetera.core.Util;
import org.marketcetera.core.Version;
import org.marketcetera.core.VersionInfo;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.persist.CollectionPageResponse;
import org.marketcetera.rpc.base.BaseRpc;
import org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest;
import org.marketcetera.rpc.base.BaseRpc.LoginResponse;
import org.marketcetera.rpc.base.BaseRpc.LogoutResponse;
import org.marketcetera.rpc.base.BaseUtil;
import org.marketcetera.rpc.client.AbstractRpcClient;
import org.marketcetera.rpc.paging.PagingUtil;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.FIXMessageWrapper;
import org.marketcetera.trade.FIXOrder;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.NewOrReplaceOrder;
import org.marketcetera.trade.Option;
import org.marketcetera.trade.Order;
import org.marketcetera.trade.OrderBase;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.OrderSummary;
import org.marketcetera.trade.RelatedOrder;
import org.marketcetera.trade.TradeMessage;
import org.marketcetera.trade.TradeMessageListener;
import org.marketcetera.trade.client.SendOrderResponse;
import org.marketcetera.trade.client.TradingClient;
import org.marketcetera.trading.rpc.TradingRpc.BrokerStatusListenerResponse;
import org.marketcetera.trading.rpc.TradingRpc.TradeMessageListenerResponse;
import org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceBlockingStub;
import org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceStub;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.ws.tags.AppId;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.util.Timestamps;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

/* $License$ */

/**
 * Provides an RPC {@link TradingClient} implementation.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class TradingRpcClient
        extends AbstractRpcClient<TradingRpcServiceBlockingStub,TradingRpcServiceStub,TradingRpcClientParameters>
        implements TradingClient
{
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#addTradeMessageListener(org.marketcetera.trade.client.TradeMessageListener)
     */
    @Override
    public void addTradeMessageListener(TradeMessageListener inTradeMessageListener)
    {
        // check to see if this listener is already registered
        if(listenerProxies.asMap().containsKey(inTradeMessageListener)) {
            return;
        }
        // make sure that this listener wasn't just whisked out from under us
        final AbstractListenerProxy<?,?,?> listener = listenerProxies.getUnchecked(inTradeMessageListener);
        if(listener == null) {
            return;
        }
        executeCall(new Callable<Void>() {
            @Override
            public Void call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} adding report listener",
                                       getSessionId());
                TradingRpc.AddTradeMessageListenerRequest.Builder requestBuilder = TradingRpc.AddTradeMessageListenerRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setListenerId(listener.getId());
                TradingRpc.AddTradeMessageListenerRequest addTradeMessageListenerRequest = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       addTradeMessageListenerRequest);
                getAsyncStub().addTradeMessageListener(addTradeMessageListenerRequest,
                                                       (TradeMessageListenerProxy)listener);
                return null;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#removeTradeMessageListener(org.marketcetera.trade.client.TradeMessageListener)
     */
    @Override
    public void removeTradeMessageListener(TradeMessageListener inTradeMessageListener)
    {
        final AbstractListenerProxy<?,?,?> proxy = listenerProxies.getIfPresent(inTradeMessageListener);
        listenerProxies.invalidate(inTradeMessageListener);
        if(proxy == null) {
            return;
        }
        listenerProxiesById.invalidate(proxy.getId());
        executeCall(new Callable<Void>() {
            @Override
            public Void call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} removing report listener",
                                       getSessionId());
                TradingRpc.RemoveTradeMessageListenerRequest.Builder requestBuilder = TradingRpc.RemoveTradeMessageListenerRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setListenerId(proxy.getId());
                TradingRpc.RemoveTradeMessageListenerRequest removeTradeMessageListenerRequest = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       removeTradeMessageListenerRequest);
                TradingRpc.RemoveTradeMessageListenerResponse response = getBlockingStub().removeTradeMessageListener(removeTradeMessageListenerRequest);
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                return null;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#addBrokerStatusListener(org.marketcetera.brokers.BrokerStatusListener)
     */
    @Override
    public void addBrokerStatusListener(BrokerStatusListener inBrokerStatusListener)
    {
        // check to see if this listener is already registered
        if(listenerProxies.asMap().containsKey(inBrokerStatusListener)) {
            return;
        }
        // make sure that this listener wasn't just whisked out from under us
        final AbstractListenerProxy<?,?,?> listener = listenerProxies.getUnchecked(inBrokerStatusListener);
        if(listener == null) {
            return;
        }
        executeCall(new Callable<Void>() {
            @Override
            public Void call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} adding broker status listener",
                                       getSessionId());
                TradingRpc.AddBrokerStatusListenerRequest.Builder requestBuilder = TradingRpc.AddBrokerStatusListenerRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setListenerId(listener.getId());
                TradingRpc.AddBrokerStatusListenerRequest addBrokerStatusListenerRequest = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       addBrokerStatusListenerRequest);
                getAsyncStub().addBrokerStatusListener(addBrokerStatusListenerRequest,
                                                       (BrokerStatusListenerProxy)listener);
                return null;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#removeBrokerStatusListener(org.marketcetera.brokers.BrokerStatusListener)
     */
    @Override
    public void removeBrokerStatusListener(BrokerStatusListener inBrokerStatusListener)
    {
        final AbstractListenerProxy<?,?,?> proxy = listenerProxies.getIfPresent(inBrokerStatusListener);
        listenerProxies.invalidate(inBrokerStatusListener);
        if(proxy == null) {
            return;
        }
        listenerProxiesById.invalidate(proxy.getId());
        executeCall(new Callable<Void>() {
            @Override
            public Void call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} removing broker status listener",
                                       getSessionId());
                TradingRpc.RemoveBrokerStatusListenerRequest.Builder requestBuilder = TradingRpc.RemoveBrokerStatusListenerRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setListenerId(proxy.getId());
                TradingRpc.RemoveBrokerStatusListenerRequest removeBrokerStatusListenerRequest = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       removeBrokerStatusListenerRequest);
                TradingRpc.RemoveBrokerStatusListenerResponse response = getBlockingStub().removeBrokerStatusListener(removeBrokerStatusListenerRequest);
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                return null;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#findRootOrderIdFor(org.marketcetera.trade.OrderID)
     */
    @Override
    public OrderID findRootOrderIdFor(OrderID inOrderId)
    {
        OrderID result = rootOrderIdCache.getIfPresent(inOrderId);
        if(result != null) {
            return result;
        }
        result = executeCall(new Callable<OrderID>() {
            @Override
            public OrderID call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} finding root order ID for: {}",
                                       getSessionId(),
                                       inOrderId);
                TradingRpc.FindRootOrderIdRequest.Builder requestBuilder = TradingRpc.FindRootOrderIdRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setOrderId(inOrderId.getValue());
                TradingRpc.FindRootOrderIdResponse response = getBlockingStub().findRootOrderId(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                OrderID result = null;
                if(response.getRootOrderId() != null) {
                    result = new OrderID(response.getRootOrderId());
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       result);
                return result;
            }}
        );
        if(result != null) {
            rootOrderIdCache.put(inOrderId,
                                 result);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#getPositionAsOf(java.util.Date, org.marketcetera.trade.Instrument)
     */
    @Override
    public BigDecimal getPositionAsOf(Date inDate,
                                      Instrument inInstrument)
    {
        return executeCall(new Callable<BigDecimal>() {
            @Override
            public BigDecimal call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} getting position of {} as of {}",
                                       getSessionId(),
                                       inInstrument,
                                       inDate);
                TradingRpc.GetPositionAsOfRequest.Builder requestBuilder = TradingRpc.GetPositionAsOfRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setInstrument(TradingUtil.getRpcInstrument(inInstrument));
                requestBuilder.setTimestamp(Timestamps.fromMillis(inDate.getTime()));
                TradingRpc.GetPositionAsOfResponse response = getBlockingStub().getPositionAsOf(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                BigDecimal result = BigDecimal.ZERO;
                if(response.hasPosition()) {
                    result = BaseUtil.getScaledQuantity(response.getPosition());
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       result);
                return result;
            }}
        );
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#getAllPositionsAsOf(java.util.Date)
     */
    @Override
    public Map<PositionKey<? extends Instrument>,BigDecimal> getAllPositionsAsOf(Date inDate)
    {
        return executeCall(new Callable<Map<PositionKey<? extends Instrument>,BigDecimal>>() {
            @Override
            public Map<PositionKey<? extends Instrument>,BigDecimal> call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} getting all positions as of {}",
                                       getSessionId(),
                                       inDate);
                TradingRpc.GetAllPositionsAsOfRequest.Builder requestBuilder = TradingRpc.GetAllPositionsAsOfRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setTimestamp(Timestamps.fromMillis(inDate.getTime()));
                TradingRpc.GetAllPositionsAsOfResponse response = getBlockingStub().getAllPositionsAsOf(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                Map<PositionKey<? extends Instrument>,BigDecimal> result = Maps.newHashMap();
                for(TradingTypesRpc.Position rpcPosition : response.getPositionList()) {
                    PositionKey<? extends Instrument> positionKey = null;
                    BigDecimal position = BigDecimal.ZERO;
                    if(rpcPosition.hasPositionKey()) {
                        positionKey = TradingUtil.getPositionKey(rpcPosition.getPositionKey());
                    }
                    if(rpcPosition.hasPosition()) {
                        position = BaseUtil.getScaledQuantity(rpcPosition.getPosition());
                    }
                    if(positionKey != null) {
                        result.put(positionKey,
                                   position);
                    }
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       result);
                return result;
            }}
        );
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#getOptionPositionsAsOf(java.util.Date, java.lang.String[])
     */
    @Override
    public Map<PositionKey<Option>,BigDecimal> getOptionPositionsAsOf(Date inDate,
                                                                      String... inRootSymbols)
    {
        return executeCall(new Callable<Map<PositionKey<Option>,BigDecimal>>() {
            @Override
            @SuppressWarnings("unchecked")
            public Map<PositionKey<Option>,BigDecimal> call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} getting all option positions as of {}",
                                       getSessionId(),
                                       inDate);
                TradingRpc.GetAllPositionsByRootAsOfRequest.Builder requestBuilder = TradingRpc.GetAllPositionsByRootAsOfRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setTimestamp(Timestamps.fromMillis(inDate.getTime()));
                TradingRpc.GetAllPositionsByRootAsOfResponse response = getBlockingStub().getAllPositionsByRootAsOf(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                Map<PositionKey<Option>,BigDecimal> result = Maps.newHashMap();
                for(TradingTypesRpc.Position rpcPosition : response.getPositionList()) {
                    PositionKey<Option> positionKey = null;
                    BigDecimal position = BigDecimal.ZERO;
                    if(rpcPosition.hasPositionKey()) {
                        positionKey = (PositionKey<Option>)TradingUtil.getPositionKey(rpcPosition.getPositionKey());
                    }
                    if(rpcPosition.hasPosition()) {
                        position = BaseUtil.getScaledQuantity(rpcPosition.getPosition());
                    }
                    if(positionKey != null) {
                        result.put(positionKey,
                                   position);
                    }
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       result);
                return result;
            }}
        );
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#getBrokersStatus()
     */
    @Override
    public BrokersStatus getBrokersStatus()
    {
        return executeCall(new Callable<BrokersStatus>() {
            @Override
            public BrokersStatus call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} getting brokers status",
                                       getSessionId());
                TradingRpc.BrokersStatusRequest.Builder requestBuilder = TradingRpc.BrokersStatusRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                TradingRpc.BrokersStatusResponse response = getBlockingStub().getBrokersStatus(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                List<ClusteredBrokerStatus> brokers = Lists.newArrayList();
                for(TradingTypesRpc.BrokerStatus rpcBrokerStatus : response.getBrokerStatusList()) {
                    Optional<BrokerStatus> brokerStatus = TradingUtil.getBrokerStatus(rpcBrokerStatus);
                    if(brokerStatus.isPresent()) {
                        brokers.add((ClusteredBrokerStatus)brokerStatus.get());
                    }
                }
                BrokersStatus brokersStatus = new ClusteredBrokersStatus(brokers);
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       brokersStatus);
                return brokersStatus;
            }}
        );
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#resolveSymbol(java.lang.String)
     */
    @Override
    public Instrument resolveSymbol(String inSymbol)
    {
        Instrument result = symbolCache.getIfPresent(inSymbol);
        if(result != null) {
            return result;
        }
        result = executeCall(new Callable<Instrument>() {
            @Override
            public Instrument call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} resolving symbol: {}",
                                       getSessionId(),
                                       inSymbol);
                TradingRpc.ResolveSymbolRequest.Builder requestBuilder = TradingRpc.ResolveSymbolRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setSymbol(inSymbol);
                TradingRpc.ResolveSymbolResponse response = getBlockingStub().resolveSymbol(requestBuilder.build());
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                Instrument result = null;
                if(response.hasInstrument()) {
                    result = TradingUtil.getInstrument(response.getInstrument());
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       result);
                return result;
            }}
        );
        if(result != null) {
            symbolCache.put(inSymbol,
                            result);
        }
        return result;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.tradingclient.TradingClient#getOpenOrders(int, int)
     */
    @Override
    public CollectionPageResponse<? extends OrderSummary> getOpenOrders(final int inPageNumber,
                                                                        final int inPageSize)
    {
        return executeCall(new Callable<CollectionPageResponse<? extends OrderSummary>>() {
            @Override
            public CollectionPageResponse<? extends OrderSummary> call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} requesting open orders",
                                       getSessionId());
                TradingRpc.OpenOrdersRequest.Builder requestBuilder = TradingRpc.OpenOrdersRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setPageRequest(PagingUtil.buildPageRequest(inPageNumber,
                                                                          inPageSize));
                TradingRpc.OpenOrdersResponse response = getBlockingStub().getOpenOrders(requestBuilder.build());
                CollectionPageResponse<OrderSummary> results = new CollectionPageResponse<>();
                for(TradingTypesRpc.OrderSummary rpcOrderSummary : response.getOrdersList()) {
                    Optional<OrderSummary> value = TradingUtil.getOrderSummary(rpcOrderSummary);
                    if(value.isPresent()) {
                        results.getElements().add(value.get());
                    }
                }
                PagingUtil.setPageResponse(response.getPageResponse(),
                                           results);
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       results);
                return results;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#getOpenOrders()
     */
    @Override
    public Collection<? extends OrderSummary> getOpenOrders()
    {
        return getOpenOrders(0,
                             Integer.MAX_VALUE).getElements();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#sendOrder(org.marketcetera.trade.Order)
     */
    @Override
    public SendOrderResponse sendOrder(Order inOrder)
    {
        return sendOrders(Lists.newArrayList(inOrder)).get(0);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#sendOrders(java.util.List)
     */
    @Override
    public List<SendOrderResponse> sendOrders(List<Order> inOrders)
    {
        return executeCall(new Callable<List<SendOrderResponse>>(){
            @Override
            public List<SendOrderResponse> call()
                    throws Exception
            {
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {} order(s)",
                                       getSessionId(),
                                       inOrders.size());
                TradingRpc.SendOrderRequest.Builder requestBuilder = TradingRpc.SendOrderRequest.newBuilder();
                TradingTypesRpc.Order.Builder orderBuilder = TradingTypesRpc.Order.newBuilder();
                TradingTypesRpc.OrderBase.Builder orderBaseBuilder = null;
                TradingTypesRpc.FIXOrder.Builder fixOrderBuilder = null;
                BaseRpc.Map.Builder mapBuilder = null;
                BaseRpc.KeyValuePair.Builder keyValuePairBuilder = null;
                requestBuilder.setSessionId(getSessionId().getValue());
                for(Order order : inOrders) {
                    try {
                        if(order instanceof FIXOrder) {
                            FIXOrder fixOrder = (FIXOrder)order;
                            if(fixOrderBuilder == null) {
                                fixOrderBuilder = TradingTypesRpc.FIXOrder.newBuilder();
                            } else {
                                fixOrderBuilder.clear();
                            }
                            if(mapBuilder == null) {
                                mapBuilder = BaseRpc.Map.newBuilder();
                            } else {
                                mapBuilder.clear();
                            }
                            for(Map.Entry<Integer,String> entry : fixOrder.getFields().entrySet()) {
                                if(keyValuePairBuilder == null) {
                                    keyValuePairBuilder = BaseRpc.KeyValuePair.newBuilder();
                                } else {
                                    keyValuePairBuilder.clear();
                                }
                                keyValuePairBuilder.setKey(String.valueOf(entry.getKey()));
                                keyValuePairBuilder.setKey(entry.getValue());
                                mapBuilder.addKeyValuePairs(keyValuePairBuilder.build());
                            }
                            TradingUtil.setBrokerId(fixOrder,
                                                    fixOrderBuilder);
                            // TODO
//                            fixOrderBuilder.setMessage(mapBuilder.build());
                            orderBuilder.setFixOrder(fixOrderBuilder.build());
                        } else if(order instanceof OrderBase) {
                            if(orderBaseBuilder == null) {
                                orderBaseBuilder = TradingTypesRpc.OrderBase.newBuilder();
                            } else {
                                orderBaseBuilder.clear();
                            }
                            // either an OrderSingle, OrderReplace, or OrderCancel
                            // the types overlap some, first, set all the common fields on OrderBase
                            OrderBase orderBase = (OrderBase)order;
                            TradingUtil.setAccount(orderBase,
                                                   orderBaseBuilder);
                            TradingUtil.setBrokerId(orderBase,
                                                    orderBaseBuilder);
                            TradingUtil.setRpcCustomFields(orderBase,
                                                        orderBaseBuilder);
                            TradingUtil.setInstrument(orderBase,
                                                      orderBaseBuilder);
                            TradingUtil.setOrderId(orderBase,
                                                   orderBaseBuilder);
                            TradingUtil.setQuantity(orderBase,
                                                    orderBaseBuilder);
                            TradingUtil.setSide(orderBase,
                                                orderBaseBuilder);
                            TradingUtil.setText(orderBase,
                                                orderBaseBuilder);
                            // now, check for various special order types
                            if(orderBase instanceof NewOrReplaceOrder) {
                                NewOrReplaceOrder newOrReplaceOrder = (NewOrReplaceOrder)orderBase;
                                TradingUtil.setDisplayQuantity(newOrReplaceOrder,
                                                               orderBaseBuilder);
                                TradingUtil.setExecutionDestination(newOrReplaceOrder,
                                                                    orderBaseBuilder);
                                TradingUtil.setOrderCapacity(newOrReplaceOrder,
                                                             orderBaseBuilder);
                                TradingUtil.setOrderType(newOrReplaceOrder,
                                                         orderBaseBuilder);
                                TradingUtil.setPositionEffect(newOrReplaceOrder,
                                                              orderBaseBuilder);
                                TradingUtil.setPrice(newOrReplaceOrder,
                                                     orderBaseBuilder);
                                TradingUtil.setTimeInForce(newOrReplaceOrder,
                                                           orderBaseBuilder);
                            }
                            TradingTypesRpc.OrderBase rpcOrderBase = orderBaseBuilder.build();
                            if(order instanceof RelatedOrder) {
                                RelatedOrder relatedOrder = (RelatedOrder)order;
                                TradingUtil.setOriginalOrderId(relatedOrder,
                                                               orderBaseBuilder);
                            } else {
                                orderBuilder.setOrderBase(rpcOrderBase);
                            }
                        } else {
                            throw new UnsupportedOperationException("Unsupported order type: " + order.getClass().getSimpleName());
                        }
                        requestBuilder.addOrder(orderBuilder.build());
                        orderBuilder.clear();
                        if(orderBaseBuilder != null) {
                            orderBaseBuilder.clear();
                        }
                    } catch (Exception e) {
                        PlatformServices.handleException(TradingRpcClient.this,
                                                         "Unable to send " + order,
                                                         e);
                    }
                }
                TradingRpc.SendOrderRequest sendOrderRequest = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       sendOrderRequest);
                TradingRpc.SendOrderResponse response = getBlockingStub().sendOrders(sendOrderRequest);
                List<SendOrderResponse> results = new ArrayList<>();
                for(TradingRpc.OrderResponse rpcResponse : response.getOrderResponseList()) {
                    SendOrderResponse orderResponse = new SendOrderResponse();
                    orderResponse.setOrderId(rpcResponse.getOrderid()==null?null:new OrderID(rpcResponse.getOrderid()));
                    results.add(orderResponse);
                }
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} returning {}",
                                       getSessionId(),
                                       results);
                return results;
            }
        });
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.client.TradingClient#addReport(org.marketcetera.trade.FIXMessageWrapper, org.marketcetera.trade.BrokerID)
     */
    @Override
    public void addReport(FIXMessageWrapper inReport,
                          BrokerID inBrokerID)
    {
        executeCall(new Callable<Void>() {
            @Override
            public Void call()
                    throws Exception
            {
                TradingRpc.AddReportRequest.Builder requestBuilder = TradingRpc.AddReportRequest.newBuilder();
                requestBuilder.setSessionId(getSessionId().getValue());
                requestBuilder.setBrokerId(inBrokerID.getValue());
                requestBuilder.setMessage(TradingUtil.getFixMessage(inReport.getMessage()));
                TradingRpc.AddReportRequest request = requestBuilder.build();
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} sending {}",
                                       getSessionId(),
                                       request);
                TradingRpc.AddReportResponse response = getBlockingStub().addReport(request);
                SLF4JLoggerProxy.trace(TradingRpcClient.this,
                                       "{} received {}",
                                       getSessionId(),
                                       response);
                return null;
            }}
        );
    }
    /**
     * Create a new TradingRpcClient instance.
     *
     * @param inParameters a <code>TradingRpcClientParameters</code> value
     */
    TradingRpcClient(TradingRpcClientParameters inParameters)
    {
        super(inParameters);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#getBlockingStub(io.grpc.Channel)
     */
    @Override
    protected TradingRpcServiceBlockingStub getBlockingStub(Channel inChannel)
    {
        return TradingRpcServiceGrpc.newBlockingStub(inChannel);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#getAsyncStub(io.grpc.Channel)
     */
    @Override
    protected TradingRpcServiceStub getAsyncStub(Channel inChannel)
    {
        return TradingRpcServiceGrpc.newStub(inChannel);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#executeLogin(org.marketcetera.rpc.base.BaseRpc.LoginRequest)
     */
    @Override
    protected LoginResponse executeLogin(BaseRpc.LoginRequest inRequest)
    {
        return getBlockingStub().login(inRequest);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#executeLogout(org.marketcetera.rpc.base.BaseRpc.LogoutRequest)
     */
    @Override
    protected LogoutResponse executeLogout(BaseRpc.LogoutRequest inRequest)
    {
        return getBlockingStub().logout(inRequest);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#executeHeartbeat(org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest, io.grpc.stub.StreamObserver)
     */
    @Override
    protected void executeHeartbeat(HeartbeatRequest inRequest,
                                    StreamObserver<BaseRpc.HeartbeatResponse> inObserver)
    {
        getAsyncStub().heartbeat(inRequest,
                                 inObserver);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#getAppId()
     */
    @Override
    protected AppId getAppId()
    {
        return APP_ID;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.client.AbstractRpcClient#getVersionInfo()
     */
    @Override
    protected VersionInfo getVersionInfo()
    {
        return APP_ID_VERSION;
    }
    /**
     * Provides common behavior for message listener proxies.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static abstract class AbstractListenerProxy<ListenerResponseClazz,MessageClazz,MessageListenerClazz>
            implements StreamObserver<ListenerResponseClazz>
    {
        private static AbstractListenerProxy<?,?,?> getListenerFor(Object inListener)
        {
            if(inListener instanceof TradeMessageListener) {
                return new TradeMessageListenerProxy((TradeMessageListener)inListener);
            } else if(inListener instanceof BrokerStatusListener) {
                return new BrokerStatusListenerProxy((BrokerStatusListener)inListener);
            } else {
                throw new UnsupportedOperationException();
            }
        }
        /* (non-Javadoc)
         * @see io.grpc.stub.StreamObserver#onNext(java.lang.Object)
         */
        @Override
        public void onNext(ListenerResponseClazz inResponse)
        {
            SLF4JLoggerProxy.trace(TradingRpcClient.class,
                                   "{} received {}",
                                   getId(),
                                   inResponse);
            MessageClazz message;
            try {
                message = translateMessage(inResponse);
            } catch (Exception e) {
                PlatformServices.handleException(TradingRpcClient.class,
                                                 "Error translating message",
                                                 e);
                return;
            }
            if(message == null) {
                return;
            }
            try {
                sendMessage(messageListener,
                            message);
            } catch (Exception e) {
                PlatformServices.handleException(TradingRpcClient.class,
                                                 "Error sending message",
                                                 e);
            }
        }
        /* (non-Javadoc)
         * @see io.grpc.stub.StreamObserver#onError(java.lang.Throwable)
         */
        @Override
        public void onError(Throwable inT)
        {
            SLF4JLoggerProxy.trace(TradingRpcClient.class,
                                   "{} received {}",
                                   getId(),
                                   inT);
        }
        /* (non-Javadoc)
         * @see io.grpc.stub.StreamObserver#onCompleted()
         */
        @Override
        public void onCompleted()
        {
            SLF4JLoggerProxy.trace(TradingRpcClient.class,
                                   "{} completed",
                                   getId());
        }
        /**
         * Translate the message contained in the given response.
         *
         * @param inResponse a <code>ListenerResponseClazz</code> value
         * @return a <code>MessageClazz</code> value
         */
        protected abstract MessageClazz translateMessage(ListenerResponseClazz inResponse);
        /**
         * Send the given message to the given message listener.
         *
         * @param inMessageListener a <code>MessageListenerClazz</code> value
         * @param inMessage a <code>MessageClazz</code> value
         */
        protected abstract void sendMessage(MessageListenerClazz inMessageListener,
                                            MessageClazz inMessage);
        /**
         * Get the id value.
         *
         * @return a <code>String</code> value
         */
        protected String getId()
        {
            return id;
        }
        /**
         * Create a new AbstractListenerProxy instance.
         *
         * @param inMessageListener a <code>MessageListenerClazz</code> value
         */
        protected AbstractListenerProxy(MessageListenerClazz inMessageListener)
        {
            messageListener = inMessageListener;
        }
        /**
         * message listener to receive the messages
         */
        private final MessageListenerClazz messageListener;
        /**
         * unique id value
         */
        private final String id = UUID.randomUUID().toString();
    }
    /**
     * Provides an interface between broker message stream listeners and their handlers.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class BrokerStatusListenerProxy
            extends AbstractListenerProxy<BrokerStatusListenerResponse,BrokerStatus,BrokerStatusListener>
    {
        /**
         * Create a new BrokerStatusListenerProxy instance.
         *
         * @param inMessageListener
         */
        protected BrokerStatusListenerProxy(BrokerStatusListener inMessageListener)
        {
            super(inMessageListener);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcClient.AbstractListenerProxy#translateMessage(java.lang.Object)
         */
        @Override
        protected BrokerStatus translateMessage(BrokerStatusListenerResponse inResponse)
        {
            return TradingUtil.getBrokerStatus(inResponse).orElse(null);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcClient.AbstractListenerProxy#sendMessage(java.lang.Object, java.lang.Object)
         */
        @Override
        protected void sendMessage(BrokerStatusListener inMessageListener,
                                   BrokerStatus inMessage)
        {
            inMessageListener.receiveBrokerStatus(inMessage);
        }
    }
    /**
     * Provides an interface between trade message stream listeners and their handlers.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class TradeMessageListenerProxy
            extends AbstractListenerProxy<TradeMessageListenerResponse,TradeMessage,TradeMessageListener>
    {
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcClient.AbstractListenerProxy#translateMessage(java.lang.Object)
         */
        @Override
        protected TradeMessage translateMessage(TradeMessageListenerResponse inResponse)
        {
            return TradingUtil.getTradeMessage(inResponse);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcClient.AbstractListenerProxy#sendMessage(java.lang.Object, java.lang.Object)
         */
        @Override
        protected void sendMessage(TradeMessageListener inMessageListener,
                                   TradeMessage inMessage)
        {
            inMessageListener.receiveTradeMessage(inMessage);
        }
        /**
         * Create a new TradeMessageListenerProxy instance.
         *
         * @param inTradeMessageListener a <code>TradeMessageListener</code> value
         */
        protected TradeMessageListenerProxy(TradeMessageListener inTradeMessageListener)
        {
            super(inTradeMessageListener);
        }
    }
    /**
     * The client's application ID: the application name.
     */
    private static final String APP_ID_NAME = TradingRpcClient.class.getSimpleName();
    /**
     * The client's application ID: the version.
     */
    private static final VersionInfo APP_ID_VERSION = new VersionInfo(Version.pomversion);
    /**
     * The client's application ID: the ID.
     */
    private static final AppId APP_ID = Util.getAppId(APP_ID_NAME,APP_ID_VERSION.getVersionInfo());
    /**
     * symbol to instrument cache
     */
    private final Cache<String,Instrument> symbolCache = CacheBuilder.newBuilder().expireAfterAccess(10,TimeUnit.SECONDS).build();
    /**
     * root order ID cache
     */
    private final Cache<OrderID,OrderID> rootOrderIdCache = CacheBuilder.newBuilder().expireAfterAccess(10,TimeUnit.MINUTES).build();
    /**
     * holds report listeners by their id
     */
    private final Cache<String,AbstractListenerProxy<?,?,?>> listenerProxiesById = CacheBuilder.newBuilder().build();
    /**
     * holds listener proxies keyed by the listener
     */
    private final LoadingCache<Object,AbstractListenerProxy<?,?,?>> listenerProxies = CacheBuilder.newBuilder().build(new CacheLoader<Object,AbstractListenerProxy<?,?,?>>() {
        @Override
        public AbstractListenerProxy<?,?,?> load(Object inKey)
                throws Exception
        {
            AbstractListenerProxy<?,?,?> proxy = AbstractListenerProxy.getListenerFor(inKey);
            listenerProxiesById.put(proxy.getId(),
                                    proxy);
            return proxy;
        }}
    );
}
