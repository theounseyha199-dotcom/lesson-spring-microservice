package kh.edu.istad.platform.customer.domain.dto;
import java.util.List;

// A single MongoDB facet computes every count from the same filtered input stream.
public record CustomerStatistics(long total, List<StatusCount> byStatus, List<SegmentCount> bySegment,
                                 List<StatusSegmentCount> byStatusAndSegment) {
    public record StatusCount(String status, long total) {}
    public record SegmentCount(String segmentId, long total) {}
    public record StatusSegmentCount(String status, String segmentId, long total) {}
}
