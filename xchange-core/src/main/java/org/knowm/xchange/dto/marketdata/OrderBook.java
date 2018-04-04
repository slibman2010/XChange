package org.knowm.xchange.dto.marketdata;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.*;


import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;

/**
 * DTO representing the exchange order book
 */
public final class OrderBook implements Serializable {

  private String currencyPair;
  /**
   * the timestamp of the orderbook according to the exchange's server, null if not provided
   */
  private Date timeStamp;
  //Format date till miliseconds

  /**
   * the asks
   */
  private final List<LimitOrder> asks;

  /**
   * the bids
   */
  private final List<LimitOrder> bids;
/**
 ** We need the exchange ID per Book to separate books from different exchanges
  **/
  private int exchangeID;


  /**
   * Constructor
   *
   * @param timeStamp - the timestamp of the orderbook according to the exchange's server, //if not provided replaced by machine time
   * @param asks The ASK orders
   * @param bids The BID orders
   */
  public OrderBook(Date timeStamp, List<LimitOrder> asks, List<LimitOrder> bids) {
    currencyPair ="";
    if(timeStamp!= null)
		  this.timeStamp = timeStamp;
	  else
		  this.timeStamp = new Date();
    this.asks = asks;
    this.bids = bids;
    exchangeID = 0;

  }

  public OrderBook(){
    currencyPair ="";
    timeStamp = new Date();
    asks = new LinkedList<LimitOrder>();
    bids = new LinkedList<LimitOrder>();
  }
  public String getCurrencyPair(){
    return currencyPair;
  }
  public void setCurrencyPair(String currencyPair){
    this.currencyPair =currencyPair;
  }
  public Date getTimeStamp() {

    return timeStamp;
  }

  public List<LimitOrder> getAsks() {

    return asks;
  }

  public List<LimitOrder> getBids() {

    return bids;
  }

  public List<LimitOrder> getOrders(OrderType type) {

    return type == OrderType.ASK ? asks : bids;
  }

  /**
   * Given a new LimitOrder, it will replace a matching limit order in the orderbook if one is found, or add the new LimitOrder if one is not.
   * timeStamp will be updated if the new timestamp is non-null and in the future.
   *
   * @param limitOrder the new LimitOrder
   */
  public void update(LimitOrder limitOrder) {

    update(getOrders(limitOrder.getType()), limitOrder);
    updateDate(limitOrder.getTimestamp());
  }

  // Replace the amount for limitOrder's price in the provided list.
  private void update(List<LimitOrder> tests, LimitOrder limitOrder) {

    int idx = Collections.binarySearch(tests, limitOrder);
    if (idx >= 0) {
    	tests.remove(idx);
    	tests.add(idx, limitOrder);
    } else {
    	tests.add(-idx - 1, limitOrder);
    }
  }

  /**
   * Given an OrderBookUpdate, it will replace a matching limit order in the orderbook if one is found, or add a new if one is not. timeStamp will be
   * updated if the new timestamp is non-null and in the future.
   *
   * @param orderBookUpdate the new OrderBookUpdate
   */
  public void update(OrderBookUpdate orderBookUpdate) {

    LimitOrder limitOrder = orderBookUpdate.getLimitOrder();
    List<LimitOrder> limitOrders = getOrders(limitOrder.getType());
    int idx = Collections.binarySearch(limitOrders, limitOrder);
    if (idx >= 0) {
      limitOrders.remove(idx);
    } else {
      idx = -idx - 1;
    }

    if (orderBookUpdate.getTotalVolume().compareTo(BigDecimal.ZERO) != 0) {
      LimitOrder updatedOrder = withAmount(limitOrder, orderBookUpdate.getTotalVolume());
      limitOrders.add(idx, updatedOrder);
    }

    updateDate(limitOrder.getTimestamp());
  }

  // Returns a copy of limitOrder with tradeableAmount replaced.
  private static LimitOrder withAmount(LimitOrder limitOrder, BigDecimal tradeableAmount) {

    OrderType type = limitOrder.getType();
    CurrencyPair currencyPair = limitOrder.getCurrencyPair();
    String id = limitOrder.getId();
    Date date = limitOrder.getTimestamp();
    BigDecimal limit = limitOrder.getLimitPrice();
    return new LimitOrder(type, tradeableAmount, currencyPair, id, date, limit);
  }

  // Replace timeStamp if the provided date is non-null and in the future
  // TODO should this raise an exception if the order timestamp is in the past?
  private void updateDate(Date updateDate) {

    if (updateDate != null && (timeStamp == null || updateDate.after(timeStamp))) {
      this.timeStamp = updateDate;
    }else {
    	updateDate =new Date();
    }
  }

  @Override
  public int hashCode() {

    int hash = 17;
    hash = 31 * hash + (this.timeStamp != null ? this.timeStamp.hashCode() : 0);
    for (LimitOrder order : this.bids) {
      hash = 31 * hash + order.hashCode();
    }
    for (LimitOrder order : this.asks) {
      hash = 31 * hash + order.hashCode();
    }
    return hash;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final OrderBook other = (OrderBook) obj;
    if (this.timeStamp == null ? other.timeStamp != null : !this.timeStamp.equals(other.timeStamp)) {
      return false;
    }
    if (this.bids.size() != other.bids.size()) {
      return false;
    }
    for (int index = 0; index < this.bids.size(); index++) {
      if (!this.bids.get(index).equals(other.bids.get(index))) {
        return false;
      }
    }
    if (this.asks.size() != other.asks.size()) {
      return false;
    }
    for (int index = 0; index < this.asks.size(); index++) {
      if (!this.asks.get(index).equals(other.asks.get(index))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Identical to {@link #equals(Object) equals} method except that this ignores different timestamps. In other words, this version of equals returns
   * true if the order internal to the OrderBooks are equal but their timestamps are unequal. It returns false if false if any order between the two
   * are different.
   *
   * @param ob
   * @return
   */
  public boolean ordersEqual(OrderBook ob) {
    Date timestamp = new Date();
    if (this != null && ob != null) {
      OrderBook thisOb = new OrderBook(timestamp, this.getAsks(), this.getBids());
      OrderBook thatOb = new OrderBook(timestamp, ob.getAsks(), ob.getBids());
      return thisOb.equals(thatOb);
    } else {
      return this.equals(ob);
    }
  }

  @Override
  public String toString() {

    return "OrderBook [timestamp: " + timeStamp + ", asks=" + asks.toString() + ", bids=" + bids.toString() + "]";
  }
  public String toStringShort() {
    int count =0;
	StringBuilder builder = new StringBuilder();
    builder.append("[ "+timeStamp.getTime() + ","+String.valueOf(exchangeID)+","+this.currencyPair);
    builder.append(", BID, ");
	  for (LimitOrder  order: bids) {
		  builder.append(order.toStringShort());
      ++count;
      if(count > 10)
        break;
	  }
    count =0;
    builder.append(" ASK,");
	  for (LimitOrder  order: asks) {
		  builder.append(order.toStringShort());
       ++count;
      if(count > 10)
        break;
	  }
	   builder.append("]").append(System.getProperty("line.separator"));
	   return builder.toString();
    }
    public int getExchangeID() {
	return exchangeID;
}

public void setExchangeID(int exchangeID) {
	this.exchangeID = exchangeID;
}

	public void set(OrderBook book){
      this.currencyPair = book.getCurrencyPair();
      this.timeStamp = book.getTimeStamp();
      this.exchangeID = book.getExchangeID();
      this.bids.clear();
      this.bids.addAll(book.getBids());
      this.asks.clear();
      this.asks.addAll(book.getAsks());
    }
}
