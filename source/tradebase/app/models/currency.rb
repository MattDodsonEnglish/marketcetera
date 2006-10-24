class Currency < ActiveRecord::Base
  validates_uniqueness_of :alpha_code
  validates_uniqueness_of :numeric_code
  
  # Lookup the specified currency. If incoming currency string is empty, returns USD by default
  def Currency.get_currency(cur_string)
    if(cur_string == nil || cur_string == '') 
      cur_string = 'USD'
      logger.debug("No currency specified, defaulting to USD")
    end
    currency = Currency.find(:first, :conditions=>["alpha_code = ?", cur_string])
    return currency
  end
  


end