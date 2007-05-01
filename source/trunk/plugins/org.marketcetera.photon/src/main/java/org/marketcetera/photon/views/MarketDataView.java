package org.marketcetera.photon.views;

import java.lang.reflect.Field;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;
import org.marketcetera.core.MSymbol;
import org.marketcetera.marketdata.MarketDataListener;
import org.marketcetera.photon.IFieldIdentifier;
import org.marketcetera.photon.PhotonPlugin;
import org.marketcetera.photon.core.IncomingMessageHolder;
import org.marketcetera.photon.core.MessageHolder;
import org.marketcetera.photon.marketdata.MarketDataFeedService;
import org.marketcetera.photon.marketdata.MarketDataFeedTracker;
import org.marketcetera.photon.ui.EventListContentProvider;
import org.marketcetera.photon.ui.IndexedTableViewer;
import org.marketcetera.photon.ui.MessageListTableFormat;
import org.marketcetera.photon.ui.TextContributionItem;

import quickfix.FieldMap;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.BidPx;
import quickfix.field.BidSize;
import quickfix.field.LastPx;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.NoMDEntries;
import quickfix.field.OfferPx;
import quickfix.field.OfferSize;
import quickfix.field.Symbol;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * Market data view.
 * 
 * @author gmiller
 * @author caroline.leung@softwaregoodness.com
 * @author andrei.lissovski@softwaregoodness.com
 * @author michael.lossos@softwaregoodness.com
 */
public class MarketDataView extends MessagesView implements IMSymbolListener {


	public static final String ID = "org.marketcetera.photon.views.MarketDataView"; 

	private static final int ZERO_WIDTH_COLUMN_INDEX = 0;
	private static final int SYMBOL_COLUMN_INDEX = 1;
	private static final int LASTPX_COLUMN_INDEX = 2;
	private static final int LAST_NORMAL_COLUMN_INDEX = LASTPX_COLUMN_INDEX;
	
	
	public enum MarketDataColumns implements IFieldIdentifier
	{
		ZEROWIDTH(""), SYMBOL(Symbol.class), LASTPX(LastPx.class), BIDSZ(BidSize.class),
		BID(BidPx.class), ASK(OfferPx.class), ASKSZ(OfferSize.class);
		
		private String name;
		private Integer fieldID;

		MarketDataColumns(String name){
			this.name = name;
		}

		MarketDataColumns(Class clazz) {
			name = clazz.getSimpleName();
			try {
				Field fieldField = clazz.getField("FIELD");
				fieldID = (Integer) fieldField.get(null);
			} catch (Throwable t){
				assert(false);
			}
		}

		public String toString() {
			return name;
		}

		public Integer getFieldID() {
			return fieldID;
		}
	};

	private MarketDataFeedTracker marketDataTracker;


	public MarketDataView()
	{
		super(true);
		marketDataTracker = new MarketDataFeedTracker(
				PhotonPlugin.getDefault().getBundleContext());
		marketDataTracker.open();

		marketDataListener = new MDVMarketDataListener();
		marketDataTracker.setMarketDataListener(marketDataListener);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
	    this.setInput(new BasicEventList<MessageHolder>());
	}


	
	@Override
	public void dispose() {
		marketDataTracker.setMarketDataListener(null);
		marketDataTracker.close();
		
		super.dispose();
	}
	
	@Override
	protected void formatTable(Table messageTable) {
        messageTable.getVerticalBar().setEnabled(true);
        messageTable.setForeground(
        		messageTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        messageTable.setHeaderVisible(true);

		for (int i = 0; i < messageTable.getColumnCount(); i++) {
			boolean moveable = true;
			if (i == 0) {
				moveable = false;
			}
			messageTable.getColumn(i).setMoveable(moveable);
		}
    }

	@Override
	protected void packColumns(Table table) {
		super.packColumns(table);
		
		// The following is required to work around the root cause of #132 ""Dirt" rendered in Market Data view 
		// row selection".
		//
		// There is no way to remove the extra spacing in the first TableColumn of an SWT Table on Windows.
		// This extra spacing is what causes the visible gap on a selected row.
		// It has nothing to do with the TextCellEditor or TableViewer -- the problem can be 
		// reproduced using just an SWT Table with text in the TableItems. The first column always has extra space.
		//
		// The best that can be done to get rid of the visible gap in a selected row
		// is to create an unmovable zero width first column. 
		TableColumn zeroFirstColumn = table.getColumn(ZERO_WIDTH_COLUMN_INDEX);
		zeroFirstColumn.setWidth(0);
		zeroFirstColumn.setResizable(false);
		zeroFirstColumn.setMoveable(false);
		zeroFirstColumn.setText("");
		zeroFirstColumn.setImage(null);
	}

	@Override
	protected IndexedTableViewer createTableViewer(Table aMessageTable, Enum[] enums) {
		IndexedTableViewer aMessagesViewer = new IndexedTableViewer(aMessageTable);
		getSite().setSelectionProvider(aMessagesViewer);
		aMessagesViewer.setContentProvider(new EventListContentProvider<MessageHolder>());
		aMessagesViewer.setLabelProvider(new MarketDataTableFormat(aMessageTable, getSite()));
		
		// Create the cell editors
	    CellEditor[] editors = new CellEditor[MarketDataColumns.values().length];

	    // Column 1 : Completed (Checkbox)
	    editors[SYMBOL_COLUMN_INDEX] = new TextCellEditor(aMessageTable);

	    // Assign the cell editors to the viewer 
	    aMessagesViewer.setCellEditors(editors);
	    String[] columnProperties = new String[MarketDataColumns.values().length];
	    columnProperties[SYMBOL_COLUMN_INDEX] = MarketDataColumns.SYMBOL.toString();
	    aMessagesViewer.setColumnProperties(columnProperties);
	    
	    // Set the cell modifier for the viewer
	    aMessagesViewer.setCellModifier(new MarketDataCellModifier(this));

	    return aMessagesViewer;
	}

	@Override
	protected Enum[] getEnumValues() {
		return MarketDataColumns.values();
	}

	@Override
	protected void initializeToolBar(IToolBarManager theToolBarManager) {
		TextContributionItem textContributionItem = new TextContributionItem("");
		theToolBarManager.add(textContributionItem);
		theToolBarManager.add(new AddSymbolAction(textContributionItem, this));
	}
	
	private boolean listContains(String stringValue) {
		if (stringValue == null){
			return false;
		}
		EventList<MessageHolder> list = getInput();
		for (MessageHolder holder : list) {
			try {
				if (stringValue.equalsIgnoreCase(holder.getMessage().getString(Symbol.FIELD))){
					return true;
				}
			} catch (FieldNotFound e) {
				// do nothing
			}
		}
		return false;
	}

	private void updateQuote(Message quote) {
		EventList<MessageHolder> list = getInput();
		int i = 0;
		for (MessageHolder holder : list) {
			Message message = holder.getMessage();
			try {
				if (message.getString(Symbol.FIELD).equals(quote.getString(Symbol.FIELD)))
				{
					IncomingMessageHolder newHolder = new IncomingMessageHolder(quote);
					list.set(i, newHolder);
					getMessagesViewer().update(newHolder, null);
					return;
				}
			} catch (FieldNotFound e) {
			}
			i++;
		}
	}

	
	public void onQuote(final Message aQuote) {
		Display theDisplay = Display.getDefault();
		if (theDisplay.getThread() == Thread.currentThread()){
			updateQuote(aQuote);
		} else {			
			theDisplay.asyncExec(new Runnable() {
				public void run() {
					if (!getMessagesViewer().getTable().isDisposed())
						updateQuote(aQuote);
				}
			});
		}
	}


	class MarketDataCellModifier implements ICellModifier
	{
		private final MarketDataView view;

		public MarketDataCellModifier(MarketDataView view) {
			this.view = view;
		}

		public boolean canModify(Object element, String property) {
			return MarketDataColumns.SYMBOL.toString().equals(property);
		}

		public Object getValue(Object element, String property) {
			try {
				return ((MessageHolder)element).getMessage().getString(Symbol.FIELD);
			} catch (FieldNotFound e) {
				return "";
			}
		}

		public void modify(Object element, String property, Object value) {
			MarketDataFeedService service = (MarketDataFeedService) marketDataTracker.getMarketDataFeedService();
			if (service == null){
				PhotonPlugin.getMainConsoleLogger().warn("Missing quote feed");
				return;
			}
			MSymbol newSymbol = service.symbolFromString(value.toString());

			String stringValue = newSymbol.toString();
			if (listContains(stringValue)){
				return;
			}
			TableItem tableItem = (TableItem) element;
			MessageHolder messageHolder = (MessageHolder)tableItem.getData();
			Message message = messageHolder.getMessage();
			
			try {
				MSymbol symbol = service.symbolFromString(message.getString(Symbol.FIELD));
				marketDataTracker.simpleUnsubscribe(symbol);
			} catch (FieldNotFound fnf){}
			message.clear();
			if (stringValue.length()>0){
				MSymbol mSymbol = service.symbolFromString(stringValue);
				message.setField(new Symbol(stringValue));
				marketDataTracker.simpleSubscribe(mSymbol);
				getMessagesViewer().refresh();
			}
		}
	}


	private static final int BID_SIZE_INDEX = LAST_NORMAL_COLUMN_INDEX + 1;
	private static final int BID_INDEX = LAST_NORMAL_COLUMN_INDEX + 2;
	private static final int ASK_INDEX = LAST_NORMAL_COLUMN_INDEX + 3;
	private static final int ASK_SIZE_INDEX = LAST_NORMAL_COLUMN_INDEX + 4;

	private MDVMarketDataListener marketDataListener;
	
	class MarketDataTableFormat extends MessageListTableFormat {


		public MarketDataTableFormat(Table table, IWorkbenchPartSite site) {
			super(table, MarketDataColumns.values(), site);
		}

		@Override
		public String getColumnName(int index) {
			if (index == ZERO_WIDTH_COLUMN_INDEX) {
				return "";  //$NON-NLS-1$
			}
			if (index <= LAST_NORMAL_COLUMN_INDEX){
				return super.getColumnName(index);
			}
			switch(index){
			case BID_SIZE_INDEX:
				return "BidSz";
			case BID_INDEX:
				return "Bid";
			case ASK_INDEX:
				return "Ask";
			case ASK_SIZE_INDEX:
				return "AskSz";
			default:
				return "";
			}
		}

		@Override
		public String getColumnText(Object element, int index) {
			if (index == ZERO_WIDTH_COLUMN_INDEX) {
				return "";  //$NON-NLS-1$
			}
			if (index <= LAST_NORMAL_COLUMN_INDEX) {
				return super.getColumnText(element, index);
			}
			MessageHolder messageHolder = (MessageHolder) element;
			Message message = messageHolder.getMessage();
			try {
				switch (index) {
				case BID_SIZE_INDEX:
					return getGroup(message, MDEntryType.BID).getString(
							MDEntrySize.FIELD);
				case BID_INDEX:
					return getGroup(message, MDEntryType.BID).getString(
							MDEntryPx.FIELD);
				case ASK_INDEX:
					return getGroup(message, MDEntryType.OFFER).getString(
							MDEntryPx.FIELD);
				case ASK_SIZE_INDEX:
					return getGroup(message, MDEntryType.OFFER).getString(
							MDEntrySize.FIELD);
				default:
					return "";
				}
			} catch (FieldNotFound e) {
				return "";
			}
		}

		private FieldMap getGroup(Message message, char type) {
			int noEntries;
			try {
				noEntries = message.getInt(NoMDEntries.FIELD);
				for (int i = 1; i < noEntries+1; i++){
					MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
					message.getGroup(i, group);
					if (type == group.getChar(MDEntryType.FIELD)){
						return group;
					}
				}
			} catch (FieldNotFound e) {
			}
			return new Message();
		}

		@Override
		public Object fieldValueFromMap(FieldMap map, Integer fieldID) {
			Object value = super.fieldValueFromMap(map, fieldID);
			if (value == null && (map instanceof Message) 
				&& (fieldID == quickfix.field.BidSize.FIELD
					|| fieldID == quickfix.field.BidPx.FIELD
					|| fieldID == quickfix.field.OfferPx.FIELD 
					|| fieldID == quickfix.field.OfferSize.FIELD)) 
			{
				try {
					Message castedMap = (Message) map;
					switch (fieldID) {

					case quickfix.field.BidSize.FIELD:
						return getGroup(castedMap, MDEntryType.BID).getDouble(
								MDEntrySize.FIELD);
					case quickfix.field.BidPx.FIELD:
						return getGroup(castedMap, MDEntryType.BID).getDouble(
								MDEntryPx.FIELD);
					case quickfix.field.OfferPx.FIELD:
						return getGroup(castedMap, MDEntryType.OFFER)
								.getDouble(MDEntryPx.FIELD);
					case quickfix.field.OfferSize.FIELD:
						return getGroup(castedMap, MDEntryType.OFFER)
								.getDouble(MDEntrySize.FIELD);
					default:
						return 0d;
					}
				} catch (FieldNotFound e) {
					return 0d;
				}
			}
			return value;
		}
		
	}

	
	public void onAssertSymbol(MSymbol symbol) {
		addSymbol(symbol);
	}

	/**
	 * @param symbol
	 */
	public void addSymbol(MSymbol symbol) {

		if (hasSymbol(symbol)) {
			PhotonPlugin.getMainConsoleLogger().warn("Duplicate symbol added to view: " +symbol);
		} else {
			EventList<MessageHolder> list = getInput();

			Message message = new Message();
			message.setField(new Symbol(symbol.toString()));
			list.add(new MessageHolder(message));

			marketDataTracker.simpleSubscribe(symbol);
			getMessagesViewer().refresh();
		}
	}
	
	private boolean hasSymbol(final MSymbol symbol) {
		EventList<MessageHolder> list = getInput();
			
		FilterList<MessageHolder> matches = new FilterList<MessageHolder>(list, 
				new Matcher<MessageHolder>() {
					public boolean matches(MessageHolder listItem) {
						try {
							String listSymbol = listItem.getMessage().getString(Symbol.FIELD).trim().toLowerCase();
							return listSymbol.equals(symbol.getFullSymbol().trim().toLowerCase());
						} catch (FieldNotFound e) {
							return false;
						}
					}
				});
		boolean rv = !matches.isEmpty();
		return rv;
	}

	public void removeItem(MessageHolder holder){
		MarketDataFeedService service = (MarketDataFeedService) marketDataTracker.getService();
		if (service == null){
			PhotonPlugin.getMainConsoleLogger().warn("Missing quote feed");
			return;
		}
		try {
			MSymbol mSymbol = service.symbolFromString(holder.getMessage().getString(Symbol.FIELD));
			marketDataTracker.simpleUnsubscribe(mSymbol);
			removeSymbol(mSymbol);
		} catch (FieldNotFound e) {
			// do nothing
		}
	}
	
	public void removeSymbol(MSymbol symbol) {

		marketDataTracker.simpleUnsubscribe(symbol);

		EventList<MessageHolder> list = getInput();
		for (MessageHolder holder : list) {
			try {
				String messageSymbolString = holder.getMessage().getString(Symbol.FIELD);
				if (messageSymbolString.equals(symbol.toString())){
					list.remove(holder);
					break;
				}
			} catch (FieldNotFound e) {
			}
		}
		getMessagesViewer().refresh();
		
	}
	

	public class MDVMarketDataListener extends MarketDataListener {

		public void onLevel2Quote(Message aQuote) {}

		public void onQuote(Message aQuote) {
			MarketDataView.this.onQuote(aQuote);
		}

		public void onTrade(Message aTrade) {}

	}

}
