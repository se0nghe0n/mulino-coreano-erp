package com.mulino.domain.inventory;
import java.math.BigDecimal;
import java.util.*;
/** Exact physical coordinates, half-open [start,end); never infer a mixed subset. */
public final class QualityRanges {
 private QualityRanges(){}
 public record Range(BigDecimal start,BigDecimal end){public Range{if(start.signum()<0||end.compareTo(start)<=0||start.scale()>12||end.scale()>12||start.precision()>38||end.precision()>38)throw new IllegalArgumentException("Invalid physical range");}}
 public static List<Range> union(Collection<Range> input){var sorted=input.stream().sorted(Comparator.comparing(Range::start)).toList();var out=new ArrayList<Range>();for(var r:sorted){if(out.isEmpty()||out.getLast().end().compareTo(r.start())<0)out.add(r);else{var p=out.removeLast();out.add(new Range(p.start(),p.end().max(r.end())));}}return List.copyOf(out);}
 public static List<Range> intersect(Collection<Range>a,Collection<Range>b){var out=new ArrayList<Range>();for(var x:union(a))for(var y:union(b)){var s=x.start().max(y.start());var e=x.end().min(y.end());if(e.compareTo(s)>0)out.add(new Range(s,e));}return union(out);}
 public static List<Range> subtract(Collection<Range>a,Collection<Range>b){var out=new ArrayList<>(union(a));for(var block:union(b)){var next=new ArrayList<Range>();for(var r:out){if(block.end().compareTo(r.start())<=0||block.start().compareTo(r.end())>=0){next.add(r);continue;}if(block.start().compareTo(r.start())>0)next.add(new Range(r.start(),block.start()));if(block.end().compareTo(r.end())<0)next.add(new Range(block.end(),r.end()));}out=next;}return List.copyOf(out);}
 public static BigDecimal quantity(Collection<Range>r){return union(r).stream().map(x->x.end().subtract(x.start())).reduce(BigDecimal.ZERO,BigDecimal::add);}
}
