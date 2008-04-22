package org.marketcetera.photon.marketdata;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.marketcetera.core.MSymbol;
import org.marketcetera.marketdata.IMarketDataFeed;
import org.marketcetera.quickfix.FIXMessageFactory;
import org.marketcetera.quickfix.FIXVersion;

import quickfix.Group;
import quickfix.Message;
import quickfix.field.MarketDepth;
import quickfix.field.MsgType;
import quickfix.field.NoMDEntryTypes;
import quickfix.field.NoRelatedSym;
import quickfix.field.NoUnderlyings;
import quickfix.field.SecurityType;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.UnderlyingSymbol;

public class MarketDataUtils {

	static final String UTC_TIME_ZONE = "UTC";
	
	static FIXMessageFactory messageFactory = FIXVersion.FIX44
			.getMessageFactory();

	public static Message newSubscribeLevel2(MSymbol symbol) {
		Message message = newSubscribeHelper(symbol, null);
		message.setField(new MarketDepth(0)); // full book

		return message;
	}

	public static Message newSubscribeBBO(MSymbol symbol) {
		Message message = newSubscribeHelper(symbol, null);
		message.setField(new MarketDepth(1)); // top-of-book
		return message;
	}

	public static Message newSubscribeBBO(MSymbol symbol, String securityType) {
		Message message = newSubscribeHelper(symbol, securityType);
		message.setField(new MarketDepth(1)); // top-of-book
		return message;
	}

	private static Message newSubscribeHelper(MSymbol symbol, String securityType) {
		Message message = messageFactory
				.createMessage(MsgType.MARKET_DATA_REQUEST);
		message.setField(new SubscriptionRequestType(
				SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
		message.setField(new NoMDEntryTypes(0));
		Group relatedSymGroup = messageFactory.createGroup(
				MsgType.MARKET_DATA_REQUEST, NoRelatedSym.FIELD);
		relatedSymGroup.setField(new Symbol(symbol.toString()));
		if (securityType != null && !"".equals(securityType)){
			relatedSymGroup.setField(new SecurityType(securityType));
		}
		message.addGroup(relatedSymGroup);
		return message;
	}
	
	public static Message newSubscribeOptionUnderlying(MSymbol underlying){
		Message message = messageFactory.createMessage(MsgType.MARKET_DATA_REQUEST);
		message.setField(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
		message.setField(new NoMDEntryTypes(0));
		message.setField(new NoRelatedSym(0));
		message.setField(new MarketDepth(1)); // top-of-book
		Group relatedSymGroup = messageFactory.createGroup(
				MsgType.MARKET_DATA_REQUEST, NoRelatedSym.FIELD);
		Group underlyingGroup = messageFactory.createGroup(
				MsgType.MARKET_DATA_REQUEST, NoUnderlyings.FIELD);
		relatedSymGroup.setString(Symbol.FIELD, "[N/A]");
		relatedSymGroup.setField(new SecurityType(SecurityType.OPTION));

		underlyingGroup.setString(UnderlyingSymbol.FIELD, underlying.toString());

		relatedSymGroup.addGroup(underlyingGroup);
		message.addGroup(relatedSymGroup);

		return message;
	}

}
