package SA.tool.visualizer;

import SA.tool.model.CallGraphNode;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static guru.nidi.graphviz.model.Factory.*;

public class CallGraphVisualizer {
    
    private final Map<String, CallGraphNode> callGraph;
    private final Set<String> classFilters;
    private final Map<String, Color> classColors;
    private final Color[] colorPalette = {
        Color.LIGHTBLUE, Color.LIGHTCORAL, Color.LIMEGREEN, Color.LIGHTYELLOW,
        Color.LIGHTPINK, Color.LIGHTCYAN, Color.CORAL, Color.WHEAT,
        Color.PALEGREEN, Color.PLUM
    };
    
    public CallGraphVisualizer(Map<String, CallGraphNode> callGraph) {
        this.callGraph = callGraph;
        this.classFilters = new HashSet<>();
        this.classColors = new HashMap<>();
        assignClassColors();
    }
    
    public CallGraphVisualizer addClassFilter(String classPattern) {
        this.classFilters.add(classPattern.toLowerCase());
        return this;
    }
    
    public void visualize(String outputPath, LayoutEngine layout, boolean removeIsolated) throws IOException {
        // 根据文件扩展名自动选择格式
        String format = detectFormat(outputPath);
        visualize(outputPath, layout, removeIsolated, format);
    }
    
    public void visualize(String outputPath, LayoutEngine layout, boolean removeIsolated, String format) throws IOException {
        System.out.println("开始生成调用图可视化 (格式: " + format + ")...");
        
        // 过滤节点
        Map<String, CallGraphNode> filteredGraph = filterGraph();
        System.out.println("过滤后节点数: " + filteredGraph.size());
        
        // 移除孤立节点
        if (removeIsolated) {
            filteredGraph = removeIsolatedNodes(filteredGraph);
            System.out.println("移除孤立节点后: " + filteredGraph.size());
        }
        
        if (filteredGraph.isEmpty()) {
            System.out.println("警告: 过滤后没有节点可显示");
            return;
        }
        
        // 创建图
        MutableGraph graph = createGraph(filteredGraph, layout);
        
        // 根据格式选择输出类型
        Format outputFormat = getOutputFormat(format);
        
        // 生成图形
        Graphviz graphviz = Graphviz.fromGraph(graph);
        
        // 对于矢量格式，不限制宽高，让它自适应
        if (isVectorFormat(format)) {
            graphviz.render(outputFormat).toFile(new File(outputPath));
        } else {
            // 位图格式设置高分辨率
            graphviz.width(1600)
                   .height(1200)
                   .render(outputFormat)
                   .toFile(new File(outputPath));
        }
                
        System.out.println("调用图已生成: " + outputPath + " (格式: " + format + ")");
        printGraphStats(filteredGraph);
    }
    
    private String detectFormat(String outputPath) {
        String ext = outputPath.substring(outputPath.lastIndexOf('.') + 1).toUpperCase();
        switch (ext) {
            case "PNG": return "PNG";
            case "SVG": return "SVG";
            case "DOT": return "DOT";
            default: return "SVG"; // 默认使用矢量格式
        }
    }
    
    private Format getOutputFormat(String format) {
        return switch (format.toUpperCase()) {
            case "PNG" -> Format.PNG;
            case "SVG" -> Format.SVG;
            case "DOT" -> Format.DOT;
            default -> {
                System.err.println("未知格式: " + format + ", 使用SVG");
                yield Format.SVG;
            }
        };
    }
    
    private boolean isVectorFormat(String format) {
        String upperFormat = format.toUpperCase();
        return "SVG".equals(upperFormat) || "PDF".equals(upperFormat) || "DOT".equals(upperFormat);
    }
    
    private Map<String, CallGraphNode> filterGraph() {
        if (classFilters.isEmpty()) {
            return callGraph;
        }
        
        return callGraph.entrySet().stream()
                .filter(entry -> {
                    String className = entry.getValue().getClassName();
                    if (className == null) return false;
                    
                    String lowerClassName = className.toLowerCase();
                    return classFilters.stream()
                            .anyMatch(filter -> lowerClassName.contains(filter));
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }
    
    private Map<String, CallGraphNode> removeIsolatedNodes(Map<String, CallGraphNode> graph) {
        return graph.entrySet().stream()
                .filter(entry -> {
                    CallGraphNode node = entry.getValue();
                    boolean hasConnections = 
                        (node.getCallers() != null && !node.getCallers().isEmpty()) ||
                        (node.getCallees() != null && !node.getCallees().isEmpty());
                    return hasConnections;
                })
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
    }
    
    private MutableGraph createGraph(Map<String, CallGraphNode> filteredGraph, LayoutEngine layout) {
        MutableGraph graph = mutGraph("callgraph")
                .setDirected(true)
                .graphAttrs().add(
                    Rank.dir(Rank.RankDir.TOP_TO_BOTTOM),
                    GraphAttr.dpi(150)
                );
        
        // 设置布局引擎
        switch (layout) {
            case DOT:
                graph.graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.ORTHO));
                break;
            case NEATO:
                graph.graphAttrs().add(GraphAttr.splines(GraphAttr.SplineMode.POLYLINE));
                break;
        }
        
        // 创建节点
        Map<String, MutableNode> nodeMap = new HashMap<>();
        for (Map.Entry<String, CallGraphNode> entry : filteredGraph.entrySet()) {
            String methodSig = entry.getKey();
            CallGraphNode node = entry.getValue();
            
            String shortName = getShortMethodName(node);
            String className = getSimpleClassName(node.getClassName());
            Color nodeColor = classColors.getOrDefault(className, Color.LIGHTGRAY);
            
            MutableNode graphNode = mutNode(methodSig)
                    .add(Label.of(shortName))
                    .add(Style.FILLED)
                    .add(nodeColor)
                    .add(Shape.BOX)
                    .add(Attributes.attr("fontsize", "10"))
                    .add(Attributes.attr("fontname", "Arial"));
                    
            nodeMap.put(methodSig, graphNode);
            graph.add(graphNode);
        }
        
        // 创建边
        for (Map.Entry<String, CallGraphNode> entry : filteredGraph.entrySet()) {
            String fromMethod = entry.getKey();
            CallGraphNode node = entry.getValue();
            MutableNode fromNode = nodeMap.get(fromMethod);
            
            if (node.getCallees() != null) {
                for (String toMethod : node.getCallees()) {
                    MutableNode toNode = nodeMap.get(toMethod);
                    if (toNode != null) {
                        fromNode.addLink(
                            to(toNode)
                                .with(Style.SOLID)
                                .with(Color.DEEPSKYBLUE)
                                .with(Attributes.attr("arrowsize", "0.7"))
                        );
                    }
                }
            }
        }
        
        return graph;
    }
    
    private void assignClassColors() {
        Set<String> classNames = callGraph.values().stream()
                .map(node -> getSimpleClassName(node.getClassName()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
        List<String> sortedClasses = new ArrayList<>(classNames);
        Collections.sort(sortedClasses);
        
        for (int i = 0; i < sortedClasses.size(); i++) {
            String className = sortedClasses.get(i);
            Color color = colorPalette[i % colorPalette.length];
            classColors.put(className, color);
        }
    }
    
    private String getShortMethodName(CallGraphNode node) {
        String className = getSimpleClassName(node.getClassName());
        String methodName = node.getMethodName();
        
        if (className != null && methodName != null) {
            return className + "." + methodName;
        }
        
        // 从方法签名中提取
        String signature = node.getMethodSignature();
        if (signature != null && signature.length() > 40) {
            return signature.substring(0, 37) + "...";
        }
        return signature != null ? signature : "unknown";
    }
    
    private String getSimpleClassName(String fullClassName) {
        if (fullClassName == null || fullClassName.isEmpty()) {
            return "Unknown";
        }
        
        int lastDot = fullClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullClassName.substring(lastDot + 1) : fullClassName;
    }
    
    private void printGraphStats(Map<String, CallGraphNode> graph) {
        int totalEdges = graph.values().stream()
                .mapToInt(node -> node.getCallees() != null ? node.getCallees().size() : 0)
                .sum();
                
        Map<String, Long> classCount = graph.values().stream()
                .collect(Collectors.groupingBy(
                    node -> getSimpleClassName(node.getClassName()),
                    Collectors.counting()
                ));
        
        System.out.println("\n=== 调用图统计 ===");
        System.out.println("节点数: " + graph.size());
        System.out.println("边数: " + totalEdges);
        System.out.println("类分布:");
        classCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> 
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " 个方法"));
    }
    
    public enum LayoutEngine {
        DOT,     // 层次化布局 (默认，推荐用于调用图)
        NEATO    // 弹簧力布局 (备选，适合复杂关系网络)
    }
} 