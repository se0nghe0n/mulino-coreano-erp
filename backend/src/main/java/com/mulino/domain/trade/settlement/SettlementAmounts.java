package com.mulino.domain.trade.settlement;
import com.mulino.application.core.DomainError;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
/** Exact decimal values and immutable conversion policy. Never accepts floating point. */
public final class SettlementAmounts {
 private SettlementAmounts(){}
 public static BigDecimal decimal(Object value){if(value instanceof Float||value instanceof Double)throw DomainError.invalid("Decimal string required");try{var d=new BigDecimal(String.valueOf(value));if(d.scale()>12||d.precision()>38||d.precision()-d.scale()>26)throw DomainError.invalid("Decimal(38,12) overflow");return d;}catch(NumberFormatException e){throw DomainError.invalid("Decimal string required");}}
 public static BigDecimal nonnegative(Object value){var d=decimal(value);if(d.signum()<0)throw DomainError.invalid("Negative amount");return d;}
 public static BigDecimal positive(Object value){var d=nonnegative(value);if(d.signum()==0)throw DomainError.invalid("Positive quantity required");return d;}
 public static String currency(Object value){String c=Objects.toString(value,"");if(!c.matches("[A-Z]{3}"))throw DomainError.invalid("Currency required");return c;}
 public static BigDecimal product(BigDecimal q,BigDecimal price){return decimal(q.multiply(price).stripTrailingZeros().toPlainString());}
 public record Fx(String pair,BigDecimal rate,LocalDate date,String source,String policyVersion,String rounding,BigDecimal convertedAmount){ }
 public static Fx fx(String from,BigDecimal amount,Map<String,Object> s){String pair=Objects.toString(s.get("pair"),"");if(!pair.matches("[A-Z]{3}/[A-Z]{3}")||!pair.startsWith(from+"/"))throw DomainError.invalid("FX pair mismatch");var rate=positive(s.get("rate"));String source=required(s,"source"),policy=required(s,"policyVersion"),rounding=required(s,"rounding");if(!rounding.matches("(HALF_UP|HALF_EVEN|DOWN|UP)_SCALE_([0-9]|1[0-2])"))throw DomainError.invalid("Explicit supported rounding policy required");int pos=rounding.indexOf("_SCALE_");int scale=Integer.parseInt(rounding.substring(pos+7));BigDecimal converted=amount.multiply(rate).setScale(scale,RoundingMode.valueOf(rounding.substring(0,pos)));try{return new Fx(pair,rate,LocalDate.parse(required(s,"date")),source,policy,rounding,decimal(converted));}catch(java.time.format.DateTimeParseException e){throw DomainError.invalid("FX date required");}}
 public static String required(Map<String,Object> m,String k){String s=Objects.toString(m.get(k),"");if(s.isBlank())throw DomainError.invalid("Missing "+k);return s;}
}
