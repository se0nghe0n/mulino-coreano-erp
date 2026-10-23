package org.mulino.verification.modelbinding;
import java.math.BigDecimal;
import java.util.*;
import static org.mulino.verification.modelbinding.PhysicalRecipeRows.require;
/** Exact half-open physical intervals, canonicalized without counting repeated evidence twice. */
public final class PhysicalIntervals {
    public record Range(String root,BigDecimal start,BigDecimal quantity,String unit) {
        public Range {require(root!=null&&!root.isBlank()&&unit!=null&&!unit.isBlank()&&start.signum()>=0&&quantity.signum()>0,"PHYSICAL_RANGE_INVALID");}
        public BigDecimal end(){return start.add(quantity);}
        public String identity(){return root+"|"+start.stripTrailingZeros().toPlainString()+"|"+end().stripTrailingZeros().toPlainString()+"|"+unit;}
    }
    public static List<Range> union(List<Range> ranges) {
        var units=new HashMap<String,String>();var groups=new TreeMap<String,List<Range>>();
        for(var range:ranges){String old=units.putIfAbsent(range.root(),range.unit());require(old==null||old.equals(range.unit()),"PHYSICAL_IDENTITY_UNIT_CONFLICT");groups.computeIfAbsent(range.root(),k->new ArrayList<>()).add(range);}
        var out=new ArrayList<Range>();for(var group:groups.values()){group.sort(Comparator.comparing(Range::start).thenComparing(Range::end));Range head=null;for(var r:group){if(head==null)head=r;else if(r.start().compareTo(head.end())<0)head=new Range(head.root(),head.start(),head.end().max(r.end()).subtract(head.start()),head.unit());else{out.add(head);head=r;}}if(head!=null)out.add(head);}return List.copyOf(out);
    }
    private PhysicalIntervals() {}
}
