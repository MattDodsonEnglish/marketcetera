class CurrencyPair < ActiveRecord::Base
  belongs_to :first_currency,
             :class_name => 'Currency',
             :foreign_key => 'first_currency_id'
  belongs_to :second_currency,
             :class_name => 'Currency',
             :foreign_key => 'second_currency_id'

  has_many :trades, :as => :tradeable
  has_many :positions, :as => :tradeable
  has_many :marks, :as => :tradeable

  def validate
    errors.add(:first_currency, "unknown") if first_currency.nil?
    errors.add(:second_currency, "unknown") if second_currency.nil?
  end

  def to_s
    "#{first_currency_code}/#{second_currency_code}"
  end

  def first_currency_code
    first_currency.nil? ? "" : first_currency.alpha_code
  end

  def second_currency_code
    second_currency.nil? ? "" : second_currency.alpha_code
  end

  # throws an exception if one of the underlying currencies is not present
  def CurrencyPair.get_currency_pair(symbol, create_missing=true)
    # EUR/USD
    # EURUSD
    # eur/usd

    # case-insensitive
    symbol = (symbol.nil?) ? nil : symbol.upcase
    matched = /^([A-Z]{3})\/([A-Z]{3})$/.match(symbol)
    if (matched.nil?)
      matched = /^([A-Z]{3})([A-Z]{3})$/.match(symbol)
      if (matched.nil?)
        raise UnknownCurrencyPairException.new("Illegal currency pair symbol: #{symbol}")
      end
    end
    first_currency_code = matched[1]
    second_currency_code = matched[2] 
    first_currency = Currency.get_currency(first_currency_code)
    second_currency = Currency.get_currency(second_currency_code)
    currency_pair = CurrencyPair.find(:first, :conditions => { :first_currency_id => first_currency, :second_currency_id => second_currency } )
    currency_pair = CurrencyPair.create(:first_currency => first_currency, :second_currency => second_currency) if (currency_pair.nil? && create_missing)

    raise UnknownCurrencyPairException.new("Unknown currency in pair: #{symbol}") if (!currency_pair.nil? && !currency_pair.valid?)

    currency_pair
  end
end