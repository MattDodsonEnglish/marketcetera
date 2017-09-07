package org.marketcetera.client.rpc.server;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.marketcetera.admin.HasUser;
import org.marketcetera.admin.User;
import org.marketcetera.admin.service.UserService;
import org.marketcetera.brokers.BrokerStatus;
import org.marketcetera.brokers.BrokerStatusListener;
import org.marketcetera.brokers.BrokersStatus;
import org.marketcetera.brokers.service.BrokerService;
import org.marketcetera.core.PlatformServices;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.module.HasMutableStatus;
import org.marketcetera.persist.CollectionPageResponse;
import org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest;
import org.marketcetera.rpc.base.BaseRpc.HeartbeatResponse;
import org.marketcetera.rpc.base.BaseRpc.LoginRequest;
import org.marketcetera.rpc.base.BaseRpc.LoginResponse;
import org.marketcetera.rpc.base.BaseRpc.LogoutRequest;
import org.marketcetera.rpc.base.BaseRpc.LogoutResponse;
import org.marketcetera.rpc.base.BaseUtil;
import org.marketcetera.rpc.paging.PagingUtil;
import org.marketcetera.rpc.server.AbstractRpcService;
import org.marketcetera.symbol.SymbolResolverService;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.FIXMessageWrapper;
import org.marketcetera.trade.HasOrder;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.Option;
import org.marketcetera.trade.Order;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.OrderSummary;
import org.marketcetera.trade.TradeMessage;
import org.marketcetera.trade.TradeMessageListener;
import org.marketcetera.trade.UserID;
import org.marketcetera.trade.service.OrderSummaryService;
import org.marketcetera.trade.service.ReportService;
import org.marketcetera.trade.service.TradeService;
import org.marketcetera.trading.rpc.TradingRpc;
import org.marketcetera.trading.rpc.TradingRpc.AddBrokerStatusListenerRequest;
import org.marketcetera.trading.rpc.TradingRpc.AddReportRequest;
import org.marketcetera.trading.rpc.TradingRpc.AddReportResponse;
import org.marketcetera.trading.rpc.TradingRpc.AddTradeMessageListenerRequest;
import org.marketcetera.trading.rpc.TradingRpc.BrokerStatusListenerResponse;
import org.marketcetera.trading.rpc.TradingRpc.BrokersStatusRequest;
import org.marketcetera.trading.rpc.TradingRpc.BrokersStatusResponse;
import org.marketcetera.trading.rpc.TradingRpc.FindRootOrderIdRequest;
import org.marketcetera.trading.rpc.TradingRpc.FindRootOrderIdResponse;
import org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsAsOfRequest;
import org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsAsOfResponse;
import org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsByRootAsOfRequest;
import org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsByRootAsOfResponse;
import org.marketcetera.trading.rpc.TradingRpc.GetPositionAsOfRequest;
import org.marketcetera.trading.rpc.TradingRpc.GetPositionAsOfResponse;
import org.marketcetera.trading.rpc.TradingRpc.OpenOrdersRequest;
import org.marketcetera.trading.rpc.TradingRpc.OpenOrdersResponse;
import org.marketcetera.trading.rpc.TradingRpc.RemoveBrokerStatusListenerRequest;
import org.marketcetera.trading.rpc.TradingRpc.RemoveBrokerStatusListenerResponse;
import org.marketcetera.trading.rpc.TradingRpc.RemoveTradeMessageListenerRequest;
import org.marketcetera.trading.rpc.TradingRpc.RemoveTradeMessageListenerResponse;
import org.marketcetera.trading.rpc.TradingRpc.ResolveSymbolRequest;
import org.marketcetera.trading.rpc.TradingRpc.ResolveSymbolResponse;
import org.marketcetera.trading.rpc.TradingRpc.SendOrderRequest;
import org.marketcetera.trading.rpc.TradingRpc.SendOrderResponse;
import org.marketcetera.trading.rpc.TradingRpc.TradeMessageListenerResponse;
import org.marketcetera.trading.rpc.TradingRpcServiceGrpc;
import org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase;
import org.marketcetera.trading.rpc.TradingTypesRpc;
import org.marketcetera.trading.rpc.TradingUtil;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.ws.stateful.SessionHolder;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.util.Timestamps;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/* $License$ */

/**
 * Provides trade client RPC server services.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class TradeClientRpcService<SessionClazz>
        extends AbstractRpcService<SessionClazz,TradingRpcServiceGrpc.TradingRpcServiceImplBase>
{
    /**
     * Validate and start the object.
     */
    @PostConstruct
    public void start()
            throws Exception
    {
        service = new Service();
        super.start();
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.server.AbstractRpcService#getServiceDescription()
     */
    @Override
    protected String getServiceDescription()
    {
        return description;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.rpc.server.AbstractRpcService#getService()
     */
    @Override
    protected TradingRpcServiceImplBase getService()
    {
        return service;
    }
    /**
     * Trade RPC Service implementation.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id: MarketDataRpcService.java 17251 2016-09-08 23:18:29Z colin $
     * @since $Release$
     */
    private class Service
            extends TradingRpcServiceGrpc.TradingRpcServiceImplBase
    {
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#login(org.marketcetera.rpc.base.BaseRpc.LoginRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void login(LoginRequest inRequest,
                          StreamObserver<LoginResponse> inResponseObserver)
        {
            TradeClientRpcService.this.doLogin(inRequest,
                                               inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#logout(org.marketcetera.rpc.base.BaseRpc.LogoutRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void logout(LogoutRequest inRequest,
                           StreamObserver<LogoutResponse> inResponseObserver)
        {
            TradeClientRpcService.this.doLogout(inRequest,
                                                inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#heartbeat(org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void heartbeat(HeartbeatRequest inRequest,
                              StreamObserver<HeartbeatResponse> inResponseObserver)
        {
            TradeClientRpcService.this.doHeartbeat(inRequest,
                                                   inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#getOpenOrders(org.marketcetera.trading.rpc.TradingRpc.OpenOrdersRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void getOpenOrders(OpenOrdersRequest inRequest,
                                  StreamObserver<OpenOrdersResponse> inResponseObserver)
        {
            try {
                validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.OpenOrdersResponse.Builder responseBuilder = TradingRpc.OpenOrdersResponse.newBuilder();
                int pageNumber = 0;
                int pageSize = Integer.MAX_VALUE;
                if(inRequest.hasPageRequest()) {
                    pageNumber = PagingUtil.getPageNumber(inRequest.getPageRequest());
                    pageSize = PagingUtil.getPageSize(inRequest.getPageRequest());
                }
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received open order request {}",
                                       inRequest);
                CollectionPageResponse<? extends OrderSummary> orderSummaryPage = orderSummaryService.findOpenOrders(pageNumber,
                                                                                                                     pageSize);
                for(OrderSummary orderSummary : orderSummaryPage.getElements()) {
                    responseBuilder.addOrders(TradingUtil.getRpcOrderSummary(orderSummary));
                }
                responseBuilder.setPageResponse(PagingUtil.getPageResponse(orderSummaryPage));
                TradingRpc.OpenOrdersResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Responding: {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#sendOrders(org.marketcetera.trading.rpc.TradingRpc.SendOrderRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void sendOrders(SendOrderRequest inRequest,
                               StreamObserver<SendOrderResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.SendOrderResponse.Builder responseBuilder = TradingRpc.SendOrderResponse.newBuilder();
                TradingRpc.OrderResponse.Builder orderResponseBuilder = TradingRpc.OrderResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received send order request {} from {}",
                                       inRequest,
                                       sessionHolder);
                for(TradingTypesRpc.Order rpcOrder : inRequest.getOrderList()) {
                    try {
                        Order matpOrder = TradingUtil.getOrder(rpcOrder);
                        TradingUtil.setOrderId(matpOrder,
                                               orderResponseBuilder);
                        User user = userService.findByName(sessionHolder.getUser());
                        Object result = tradeService.submitOrderToOutgoingDataFlow(new RpcOrderWrapper(user,
                                                                                                       matpOrder));
                        SLF4JLoggerProxy.debug(TradeClientRpcService.this,
                                               "Order submission returned {}",
                                               result);
                    } catch (Exception e) {
                        SLF4JLoggerProxy.warn(TradeClientRpcService.this,
                                              e,
                                              "Unable to submit order {}",
                                              rpcOrder);
                        inResponseObserver.onError(e);
                    }
                    responseBuilder.addOrderResponse(orderResponseBuilder.build());
                    orderResponseBuilder.clear();
                }
                TradingRpc.SendOrderResponse response = responseBuilder.build();
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#resolveSymbol(org.marketcetera.trading.rpc.TradingRpc.ResolveSymbolRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void resolveSymbol(ResolveSymbolRequest inRequest,
                                  StreamObserver<ResolveSymbolResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.ResolveSymbolResponse.Builder responseBuilder = TradingRpc.ResolveSymbolResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received resolve symbol request {} from {}",
                                       inRequest,
                                       sessionHolder);
                Instrument instrument = symbolResolverService.resolveSymbol(inRequest.getSymbol());
                TradingUtil.setInstrument(instrument,
                                          responseBuilder);
                TradingRpc.ResolveSymbolResponse response = responseBuilder.build();
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#addTradeMessageListener(org.marketcetera.trading.rpc.TradingRpc.AddTradeMessageListenerRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void addTradeMessageListener(AddTradeMessageListenerRequest inRequest,
                                            StreamObserver<TradeMessageListenerResponse> inResponseObserver)
        {
            try {
                validateAndReturnSession(inRequest.getSessionId());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received add trade message listener request {}",
                                       inRequest);
                String listenerId = inRequest.getListenerId();
                AbstractListenerProxy<?> tradeMessageListenerProxy = listenerProxiesById.getIfPresent(listenerId);
                if(tradeMessageListenerProxy == null) {
                    tradeMessageListenerProxy = new TradeMessageListenerProxy(listenerId,
                                                                              inResponseObserver);
                    listenerProxiesById.put(tradeMessageListenerProxy.getId(),
                                            tradeMessageListenerProxy);
                    tradeService.addTradeMessageListener((TradeMessageListener)tradeMessageListenerProxy);
                }
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#removeTradeMessageListener(org.marketcetera.trading.rpc.TradingRpc.RemoveTradeMessageListenerRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void removeTradeMessageListener(RemoveTradeMessageListenerRequest inRequest,
                                               StreamObserver<RemoveTradeMessageListenerResponse> inResponseObserver)
        {
            try {
                validateAndReturnSession(inRequest.getSessionId());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received remove trade message listener request {}",
                                       inRequest);
                String listenerId = inRequest.getListenerId();
                AbstractListenerProxy<?> tradeMessageListenerProxy = listenerProxiesById.getIfPresent(listenerId);
                listenerProxiesById.invalidate(listenerId);
                if(tradeMessageListenerProxy != null) {
                    tradeService.removeTradeMessageListener((TradeMessageListener)tradeMessageListenerProxy);
                    tradeMessageListenerProxy.close();
                }
                TradingRpc.RemoveTradeMessageListenerResponse.Builder responseBuilder = TradingRpc.RemoveTradeMessageListenerResponse.newBuilder();
                TradingRpc.RemoveTradeMessageListenerResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#addBrokerStatusListener(org.marketcetera.trading.rpc.TradingRpc.AddBrokerStatusListenerRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void addBrokerStatusListener(AddBrokerStatusListenerRequest inRequest,
                                            StreamObserver<BrokerStatusListenerResponse> inResponseObserver)
        {
            try {
                validateAndReturnSession(inRequest.getSessionId());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received add broker status listener request {}",
                                       inRequest);
                String listenerId = inRequest.getListenerId();
                AbstractListenerProxy<?> brokerStatusListenerProxy = listenerProxiesById.getIfPresent(listenerId);
                if(brokerStatusListenerProxy == null) {
                    brokerStatusListenerProxy = new BrokerStatusListenerProxy(listenerId,
                                                                              inResponseObserver);
                    listenerProxiesById.put(brokerStatusListenerProxy.getId(),
                                            brokerStatusListenerProxy);
                    brokerService.addBrokerStatusListener((BrokerStatusListener)brokerStatusListenerProxy);
                }
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#removeBrokerStatusListener(org.marketcetera.trading.rpc.TradingRpc.RemoveBrokerStatusListenerRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void removeBrokerStatusListener(RemoveBrokerStatusListenerRequest inRequest,
                                               StreamObserver<RemoveBrokerStatusListenerResponse> inResponseObserver)
        {
            try {
                validateAndReturnSession(inRequest.getSessionId());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received remove broker status listener request {}",
                                       inRequest);
                String listenerId = inRequest.getListenerId();
                AbstractListenerProxy<?> brokerStatusListenerProxy = listenerProxiesById.getIfPresent(listenerId);
                listenerProxiesById.invalidate(listenerId);
                if(brokerStatusListenerProxy != null) {
                    brokerService.removeBrokerStatusListener((BrokerStatusListener)brokerStatusListenerProxy);
                    brokerStatusListenerProxy.close();
                }
                TradingRpc.RemoveBrokerStatusListenerResponse.Builder responseBuilder = TradingRpc.RemoveBrokerStatusListenerResponse.newBuilder();
                TradingRpc.RemoveBrokerStatusListenerResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#getBrokersStatus(org.marketcetera.trading.rpc.TradingRpc.BrokersStatusRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void getBrokersStatus(BrokersStatusRequest inRequest,
                                     StreamObserver<BrokersStatusResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received get brokers status request {} from {}",
                                       inRequest,
                                       sessionHolder);
                TradingRpc.BrokersStatusResponse.Builder responseBuilder = TradingRpc.BrokersStatusResponse.newBuilder();
                BrokersStatus brokersStatus = brokerService.getBrokersStatus();
                TradingUtil.setBrokersStatus(brokersStatus,
                                             responseBuilder);
                TradingRpc.BrokersStatusResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#findRootOrderId(org.marketcetera.trading.rpc.TradingRpc.FindRootOrderIdRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void findRootOrderId(FindRootOrderIdRequest inRequest,
                                    StreamObserver<FindRootOrderIdResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.FindRootOrderIdResponse.Builder responseBuilder = TradingRpc.FindRootOrderIdResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received find root order id request {} from {}",
                                       inRequest,
                                       sessionHolder);
                OrderID orderId = new OrderID(inRequest.getOrderId());
                OrderID rootOrderId = reportService.getRootOrderIdFor(orderId);
                if(rootOrderId != null) {
                    responseBuilder.setRootOrderId(rootOrderId.getValue());
                }
                TradingRpc.FindRootOrderIdResponse response = responseBuilder.build();
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#getPositionAsOf(org.marketcetera.trading.rpc.TradingRpc.GetPositionAsOfRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void getPositionAsOf(GetPositionAsOfRequest inRequest,
                                    StreamObserver<GetPositionAsOfResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.GetPositionAsOfResponse.Builder responseBuilder = TradingRpc.GetPositionAsOfResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received get position as of request {} from {}",
                                       inRequest,
                                       sessionHolder);
                Instrument instrument = null;
                if(inRequest.hasInstrument()) {
                    instrument = TradingUtil.getInstrument(inRequest.getInstrument());
                }
                Date timestamp = null;
                if(inRequest.hasTimestamp()) {
                    timestamp = new Date(Timestamps.toMillis(inRequest.getTimestamp()));
                }
                User user = userService.findByName(sessionHolder.getUser());
                BigDecimal result = reportService.getPositionAsOf(user,
                                                                  timestamp,
                                                                  instrument);
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "{} position for {}: {} as of {}",
                                       user,
                                       instrument,
                                       result,
                                       timestamp);
                responseBuilder.setPosition(BaseUtil.getQtyValueFrom(result));
                TradingRpc.GetPositionAsOfResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#getAllPositionsAsOf(org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsAsOfRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void getAllPositionsAsOf(GetAllPositionsAsOfRequest inRequest,
                                        StreamObserver<GetAllPositionsAsOfResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.GetAllPositionsAsOfResponse.Builder responseBuilder = TradingRpc.GetAllPositionsAsOfResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received get all positions as of request {} from {}",
                                       inRequest,
                                       sessionHolder);
                Date timestamp = null;
                if(inRequest.hasTimestamp()) {
                    timestamp = new Date(Timestamps.toMillis(inRequest.getTimestamp()));
                }
                User user = userService.findByName(sessionHolder.getUser());
                Map<PositionKey<? extends Instrument>,BigDecimal> result = reportService.getAllPositionsAsOf(user,
                                                                                                             timestamp);
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "{} all positions as of {}: {}",
                                       user,
                                       timestamp,
                                       result);
                TradingTypesRpc.Position.Builder positionBuilder = TradingTypesRpc.Position.newBuilder();
                TradingTypesRpc.PositionKey.Builder positionKeyBuilder = TradingTypesRpc.PositionKey.newBuilder();
                for(Map.Entry<PositionKey<? extends Instrument>,BigDecimal> entry : result.entrySet()) {
                    PositionKey<? extends Instrument> key = entry.getKey();
                    BigDecimal value = entry.getValue();
                    if(key.getAccount() != null) {
                        positionKeyBuilder.setAccount(key.getAccount());
                    }
                    if(key.getInstrument() != null) {
                        positionKeyBuilder.setInstrument(TradingUtil.getRpcInstrument(key.getInstrument()));
                    }
                    if(key.getTraderId() != null) {
                        String traderName = String.valueOf(key.getTraderId());
                        try {
                            long traderId = Long.parseLong(key.getTraderId());
                            traderName = String.valueOf(traderId);
                            User trader = userService.findByUserId(new UserID(traderId));
                            if(trader != null) {
                                traderName = trader.getName();
                            }
                        } catch (NumberFormatException e) {
                            PlatformServices.handleException(TradeClientRpcService.this,
                                                             "Cannot convert trader id " + key.getTraderId() + " to a numerical ID",
                                                             e);
                        }
                        positionKeyBuilder.setTraderId(traderName);
                    }
                    positionBuilder.setPositionKey(positionKeyBuilder.build());
                    positionBuilder.setPosition(BaseUtil.getQtyValueFrom(value));
                    responseBuilder.addPosition(positionBuilder.build());
                    positionKeyBuilder.clear();
                    positionBuilder.clear();
                }
                TradingRpc.GetAllPositionsAsOfResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#getAllPositionsByRootAsOf(org.marketcetera.trading.rpc.TradingRpc.GetAllPositionsByRootAsOfRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void getAllPositionsByRootAsOf(GetAllPositionsByRootAsOfRequest inRequest,
                                              StreamObserver<GetAllPositionsByRootAsOfResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.GetAllPositionsByRootAsOfResponse.Builder responseBuilder = TradingRpc.GetAllPositionsByRootAsOfResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received get all positions by root as of request {} from {}",
                                       inRequest,
                                       sessionHolder);
                Date timestamp = null;
                if(inRequest.hasTimestamp()) {
                    timestamp = new Date(Timestamps.toMillis(inRequest.getTimestamp()));
                }
                User user = userService.findByName(sessionHolder.getUser());
                Map<PositionKey<Option>,BigDecimal> result = reportService.getOptionPositionsAsOf(user,
                                                                                                  timestamp,
                                                                                                  inRequest.getRootList().toArray(new String[0]));
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "{} all positions as of {}: {}",
                                       user,
                                       timestamp,
                                       result);
                TradingTypesRpc.Position.Builder positionBuilder = TradingTypesRpc.Position.newBuilder();
                TradingTypesRpc.PositionKey.Builder positionKeyBuilder = TradingTypesRpc.PositionKey.newBuilder();
                for(Map.Entry<PositionKey<Option>,BigDecimal> entry : result.entrySet()) {
                    PositionKey<Option> key = entry.getKey();
                    BigDecimal value = entry.getValue();
                    if(key.getAccount() != null) {
                        positionKeyBuilder.setAccount(key.getAccount());
                    }
                    if(key.getInstrument() != null) {
                        positionKeyBuilder.setInstrument(TradingUtil.getRpcInstrument(key.getInstrument()));
                    }
                    if(key.getTraderId() != null) {
                        String traderName = String.valueOf(key.getTraderId());
                        try {
                            long traderId = Long.parseLong(key.getTraderId());
                            traderName = String.valueOf(traderId);
                            User trader = userService.findByUserId(new UserID(traderId));
                            if(trader != null) {
                                traderName = trader.getName();
                            }
                        } catch (NumberFormatException e) {
                            PlatformServices.handleException(TradeClientRpcService.this,
                                                             "Cannot convert trader id " + key.getTraderId() + " to a numerical ID",
                                                             e);
                        }
                        positionKeyBuilder.setTraderId(traderName);
                    }
                    positionBuilder.setPositionKey(positionKeyBuilder.build());
                    positionBuilder.setPosition(BaseUtil.getQtyValueFrom(value));
                    responseBuilder.addPosition(positionBuilder.build());
                    positionKeyBuilder.clear();
                    positionBuilder.clear();
                }
                TradingRpc.GetAllPositionsByRootAsOfResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trading.rpc.TradingRpcServiceGrpc.TradingRpcServiceImplBase#addReport(org.marketcetera.trading.rpc.TradingRpc.AddReportRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void addReport(AddReportRequest inRequest,
                              StreamObserver<AddReportResponse> inResponseObserver)
        {
            try {
                SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inRequest.getSessionId());
                TradingRpc.AddReportResponse.Builder responseBuilder = TradingRpc.AddReportResponse.newBuilder();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Received add report request {} from {}",
                                       inRequest,
                                       sessionHolder);
                FIXMessageWrapper report = null;
                if(inRequest.hasMessage()) {
                    report = new FIXMessageWrapper(TradingUtil.getFixMessage(inRequest.getMessage()));
                }
                BrokerID brokerId = TradingUtil.getBrokerId(inRequest).orElse(null);
                User user = userService.findByName(sessionHolder.getUser());
                if(user == null) {
                    throw new IllegalArgumentException("Unknown user: " + user);
                }
                reportService.addReport(report,
                                        brokerId,
                                        user.getUserID());
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "{} added for {}/{}",
                                       report,
                                       user,
                                       brokerId);
                TradingRpc.AddReportResponse response = responseBuilder.build();
                SLF4JLoggerProxy.trace(TradeClientRpcService.this,
                                       "Returning {}",
                                       response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                if(e instanceof StatusRuntimeException) {
                    throw (StatusRuntimeException)e;
                }
                throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withCause(e).withDescription(ExceptionUtils.getRootCauseMessage(e)));
            }
        }
    }
    /**
     * Provides common behaviors for listener proxies.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class AbstractListenerProxy<ResponseClazz>
    {
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
         * Closes the connection with the RPC client call.
         */
        protected void close()
        {
            observer.onCompleted();
        }
        /**
         * Get the observer value.
         *
         * @return a <code>StreamObserver&lt;ResponseClazz&gt;</code> value
         */
        protected StreamObserver<ResponseClazz> getObserver()
        {
            return observer;
        }
        /**
         * Create a new AbstractListenerProxy instance.
         *
         * @param inId a <code>String</code> value
         * @param inObserver a <code>StreamObserver&lt;ResponseClazz&gt;</code> value
         */
        protected AbstractListenerProxy(String inId,
                                        StreamObserver<ResponseClazz> inObserver)
        {
            id = inId;
            observer = inObserver;
        }
        /**
         * listener id uniquely identifies this listener
         */
        private final String id;
        /**
         * provides the connection to the RPC client call
         */
        private final StreamObserver<ResponseClazz> observer;
    }
    /**
     * Provides a connection between broker status requests and the server interface.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class BrokerStatusListenerProxy
            extends AbstractListenerProxy<BrokerStatusListenerResponse>
            implements BrokerStatusListener
    {
        /* (non-Javadoc)
         * @see org.marketcetera.brokers.BrokerStatusListener#receiveBrokerStatus(org.marketcetera.brokers.BrokerStatus)
         */
        @Override
        public void receiveBrokerStatus(BrokerStatus inStatus)
        {
            TradingUtil.setBrokerStatus(inStatus,
                                        responseBuilder);
            BrokerStatusListenerResponse response = responseBuilder.build();
            SLF4JLoggerProxy.trace(TradeClientRpcService.class,
                                   "{} received broker status {}, sending {}",
                                   getId(),
                                   inStatus,
                                   response);
            getObserver().onNext(response);
            responseBuilder.clear();
        }
        /**
         * Create a new BrokerStatusListenerProxy instance.
         *
         * @param inId a <code>String</code> value
         * @param inObserver a <code>StreamObserver&lt;BrokerStatusListenerResponse&gt;</code> value
         */
        private BrokerStatusListenerProxy(String inId,
                                          StreamObserver<BrokerStatusListenerResponse> inObserver)
        {
            super(inId,
                  inObserver);
        }
        /**
         * builder used to construct messages
         */
        private final TradingRpc.BrokerStatusListenerResponse.Builder responseBuilder = TradingRpc.BrokerStatusListenerResponse.newBuilder();
    }
    /**
     * Wraps a {@link TradeMessageListener} with the RPC call from the client.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class TradeMessageListenerProxy
            extends AbstractListenerProxy<TradeMessageListenerResponse>
            implements TradeMessageListener
    {
        /* (non-Javadoc)
         * @see org.marketcetera.trade.TradeMessageListener#receiveTradeMessage(org.marketcetera.trade.TradeMessage)
         */
        @Override
        public void receiveTradeMessage(TradeMessage inTradeMessage)
        {
            TradingUtil.setTradeMessage(inTradeMessage,
                                        responseBuilder);
            TradeMessageListenerResponse response = responseBuilder.build();
            SLF4JLoggerProxy.trace(TradeClientRpcService.class,
                                   "{} received trade message {}, sending {}",
                                   getId(),
                                   inTradeMessage,
                                   response);
            getObserver().onNext(response);
            responseBuilder.clear();
        }
        /**
         * Create a new TradeMessageListenerProxy instance.
         *
         * @param inId a <code>String</code> value
         * @param inObserver a <code>StreamObserver&lt;TradeMessageListenerResponse&gt;</code> value
         */
        private TradeMessageListenerProxy(String inId,
                                          StreamObserver<TradeMessageListenerResponse> inObserver)
        {
            super(inId,
                  inObserver);
        }
        /**
         * builder used to construct messages
         */
        private final TradingRpc.TradeMessageListenerResponse.Builder responseBuilder = TradingRpc.TradeMessageListenerResponse.newBuilder();
    }
    /**
     * Wraps submitted orders.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private static class RpcOrderWrapper
            implements HasOrder,HasUser,HasMutableStatus
    {
        /* (non-Javadoc)
         * @see org.marketcetera.trade.HasOrder#getOrder()
         */
        @Override
        public Order getOrder()
        {
            return order;
        }
        /* (non-Javadoc)
         * @see org.marketcetera.admin.HasUser#getUser()
         */
        @Override
        public User getUser()
        {
            return user;
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trade.HasStatus#getFailed()
         */
        @Override
        public boolean getFailed()
        {
            return failed;
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trade.HasStatus#setFailed(boolean)
         */
        @Override
        public void setFailed(boolean inFailed)
        {
            failed = inFailed;
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trade.HasStatus#getMessage()
         */
        @Override
        public String getErrorMessage()
        {
            return message;
        }
        /* (non-Javadoc)
         * @see org.marketcetera.trade.HasStatus#setMessage(java.lang.String)
         */
        @Override
        public void setErrorMessage(String inMessage)
        {
            message = inMessage;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("RpcOrderWrapper [order=").append(order).append(", user=").append(user).append(", failed=")
                    .append(failed).append(", message=").append(message).append(", start=").append(start).append("]");
            return builder.toString();
        }
        /**
         * Create a new RpcHasOrder instance.
         *
         * @param inUser a <code>User</code> value
         * @param inOrder an <code>Order</code> value
         */
        private RpcOrderWrapper(User inUser,
                                Order inOrder)
        {
            user = inUser;
            order = inOrder;
            failed = false;
        }
        /**
         * message value
         */
        private volatile String message;
        /**
         * failed value
         */
        private volatile boolean failed;
        /**
         * user value
         */
        private final User user;
        /**
         * order value
         */
        private final Order order;
        /**
         * start time stamp
         */
        private final long start = System.nanoTime();
    }
    /**
     * provides report services
     */
    @Autowired
    private ReportService reportService;
    /**
     * provides symbol resolution services
     */
    @Autowired
    private SymbolResolverService symbolResolverService;
    /**
     * provides access to broker services
     */
    @Autowired
    private BrokerService brokerService;
    /**
     * privates access to user services
     */
    @Autowired
    private UserService userService;
    /**
     * provides access to trade services
     */
    @Autowired
    private TradeService tradeService;
    /**
     * provides access to order summary services
     */
    @Autowired
    private OrderSummaryService orderSummaryService;
    /**
     * provides the RPC service
     */
    private Service service;
    /**
     * description of this service
     */
    private final static String description = "Marketcetera Trading Service";
    /**
     * holds trade message listeners by id
     */
    private final Cache<String,AbstractListenerProxy<?>> listenerProxiesById = CacheBuilder.newBuilder().build();
}
