package SA.tool.analyzer;

import java.util.concurrent.atomic.AtomicInteger;

public class SymbolIdGenerator {
    private static final AtomicInteger counter = new AtomicInteger(1);
    
    public static String generateSymbolId(String symbolName, String symbolType, String context) {
        // 格式: sym_{type}_{counter}_{hash}
        String input = symbolName + symbolType + (context != null ? context : "");
        String hash = Integer.toHexString(input.hashCode() & 0xFFFF);
        
        return String.format("sym_%s_%03d_%s", 
            symbolType.toLowerCase().substring(0, Math.min(3, symbolType.length())), 
            counter.getAndIncrement(), 
            hash);
    }
    
    public static String generateSymbolKey(String symbolName, String symbolType, String dataType, String scope) {
        // 用于检查重复符号的唯一键
        return String.format("%s:%s:%s:%s", symbolName, symbolType, dataType, scope);
    }
} 