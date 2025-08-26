import java.util.*;
import java.time.*;

/**
 * Electric Grid Failure Predictor
 * Uses DSA concepts: Graph, BFS, PriorityQueue, Dijkstra-like path.
 */

class GridNode {
    String nodeId;
    double voltage;
    double current;
    double temperature;
    double failureProbability;
    List<GridNode> neighbors;

    public GridNode(String nodeId) {
        this.nodeId = nodeId;
        this.neighbors = new ArrayList<>();
    }

    public void updateReadings(double voltage, double current, double temperature) {
        this.voltage = voltage;
        this.current = current;
        this.temperature = temperature;
        calculateFailureProbability();
    }

    private void calculateFailureProbability() {
        double voltageScore = Math.abs(voltage - 220) / 50;
        double currentScore = current > 15 ? (current - 15) / 10 : 0;
        double tempScore = temperature > 80 ? (temperature - 80) / 20 : 0;

        this.failureProbability = Math.min(0.3 * voltageScore + 0.4 * currentScore + 0.3 * tempScore, 1.0);
    }

    public void addNeighbor(GridNode node) {
        neighbors.add(node);
    }

    @Override
    public String toString() {
        return String.format(
            "%s [Voltage: %.1f V, Current: %.1f A, Temperature: %.1f °C, Failure Probability: %.1f%%]",
            nodeId, voltage, current, temperature, failureProbability * 100
        );
    }
}

class ElectricGrid {
    private Map<String, GridNode> nodes;

    public ElectricGrid() {
        nodes = new HashMap<>();
    }

    public void addNode(GridNode node) {
        nodes.put(node.nodeId, node);
    }

    public void connectNodes(String nodeId1, String nodeId2) {
        GridNode node1 = getNode(nodeId1);
        GridNode node2 = getNode(nodeId2);
        if (node1 != null && node2 != null) {
            node1.addNeighbor(node2);
            node2.addNeighbor(node1);
        }
    }

    public List<GridNode> getPotentialCascade(String startNodeId) {
        List<GridNode> cascade = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<GridNode> queue = new LinkedList<>();

        GridNode startNode = getNode(startNodeId);
        if (startNode == null) return cascade;

        queue.add(startNode);
        visited.add(startNodeId);

        while (!queue.isEmpty()) {
            GridNode current = queue.poll();
            cascade.add(current);

            for (GridNode neighbor : current.neighbors) {
                if (!visited.contains(neighbor.nodeId)) {
                    visited.add(neighbor.nodeId);
                    queue.add(neighbor);
                }
            }
        }

        return cascade;
    }

    public List<String> findCriticalPath(String startNodeId, String endNodeId) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> queue = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (String nodeId : nodes.keySet()) {
            distances.put(nodeId, Double.MAX_VALUE);
        }
        distances.put(startNodeId, 0.0);
        queue.add(startNodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(endNodeId)) break;

            for (GridNode neighbor : getNode(current).neighbors) {
                double alt = distances.get(current) + neighbor.failureProbability;
                if (alt < distances.get(neighbor.nodeId)) {
                    distances.put(neighbor.nodeId, alt);
                    previous.put(neighbor.nodeId, current);
                    queue.add(neighbor.nodeId);
                }
            }
        }

        List<String> path = new ArrayList<>();
        for (String at = endNodeId; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);

        return path.size() > 1 && path.get(0).equals(startNodeId) ? path : Collections.emptyList();
    }

    public List<GridNode> getHighRiskNodes() {
        List<GridNode> risky = new ArrayList<>();
        for (GridNode node : nodes.values()) {
            if (node.failureProbability > 0.3) {
                risky.add(node);
            }
        }
        risky.sort((a, b) -> Double.compare(b.failureProbability, a.failureProbability));
        return risky;
    }

    public GridNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<GridNode> getAllNodes() {
        return nodes.values();
    }
}

class WeatherData {
    LocalDateTime timestamp;
    double temperature;
    double windSpeed;
    double precipitation;

    public WeatherData(double temperature, double windSpeed, double precipitation) {
        this.timestamp = LocalDateTime.now();
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.precipitation = precipitation;
    }
}

public class GridFailurePredictor {
    private ElectricGrid grid;
    private Queue<WeatherData> weatherHistory;
    private final int WEATHER_HISTORY_SIZE = 100;
    private Scanner scanner;

    public GridFailurePredictor() {
        grid = new ElectricGrid();
        weatherHistory = new LinkedList<>();
        scanner = new Scanner(System.in);
        initializeSampleGrid();
    }

    private void initializeSampleGrid() {
        String[] nodeIds = {"SUB1", "SUB2", "SUB3", "TR1", "TR2", "TR3", "GEN1", "GEN2"};
        for (String id : nodeIds) {
            GridNode node = new GridNode(id);
            node.updateReadings(
                200 + Math.random() * 40,
                5 + Math.random() * 15,
                30 + Math.random() * 60
            );
            grid.addNode(node);
        }

        grid.connectNodes("GEN1", "SUB1");
        grid.connectNodes("GEN2", "SUB2");
        grid.connectNodes("SUB1", "TR1");
        grid.connectNodes("SUB1", "TR2");
        grid.connectNodes("SUB2", "TR2");
        grid.connectNodes("SUB2", "TR3");
        grid.connectNodes("SUB3", "TR1");
        grid.connectNodes("SUB3", "TR3");
    }

    public void addWeatherData(WeatherData data) {
        weatherHistory.offer(data);
        if (weatherHistory.size() > WEATHER_HISTORY_SIZE) {
            weatherHistory.poll();
        }
        adjustGridParametersBasedOnWeather();
    }

    private void adjustGridParametersBasedOnWeather() {
        WeatherData currentWeather = weatherHistory.peek();
        if (currentWeather == null) return;

        for (GridNode node : grid.getAllNodes()) {
            node.updateReadings(node.voltage, node.current, node.temperature + currentWeather.temperature * 0.1);
        }
    }

    public void predictFailures() {
        System.out.println("\n=== Grid Status Report ===");
        for (GridNode node : grid.getAllNodes()) {
            System.out.println(node);
        }

        List<GridNode> highRiskNodes = grid.getHighRiskNodes();
        if (highRiskNodes.isEmpty()) {
            System.out.println("\nAll nodes are stable.");
        } else {
            System.out.println("\nHigh Risk Nodes Detected:");
            for (GridNode node : highRiskNodes) {
                System.out.println(">> " + node);
                if (node.failureProbability > 0.7) {
                    List<GridNode> cascade = grid.getPotentialCascade(node.nodeId);
                    System.out.println("   Possible cascade affecting " + cascade.size() + " nodes: " +
                            cascade.stream().map(n -> n.nodeId).toList());

                    List<String> pathToGen1 = grid.findCriticalPath(node.nodeId, "GEN1");
                    List<String> pathToGen2 = grid.findCriticalPath(node.nodeId, "GEN2");

                    System.out.println("   Critical path to GEN1: " + (pathToGen1.isEmpty() ? "No path" : pathToGen1));
                    System.out.println("   Critical path to GEN2: " + (pathToGen2.isEmpty() ? "No path" : pathToGen2));
                }
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n========= Electric Grid Failure Prediction =========");
        System.out.println("1. Enter Weather Data");
        System.out.println("2. Update Node Parameters");
        System.out.println("3. View All Nodes");
        System.out.println("4. Predict Failures");
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }

    private void enterWeatherData() {
        System.out.println("\nEnter Current Weather Conditions:");
        System.out.print("Temperature (°C): ");
        double temp = scanner.nextDouble();
        System.out.print("Wind Speed (km/h): ");
        double wind = scanner.nextDouble();
        System.out.print("Precipitation (mm): ");
        double precip = scanner.nextDouble();

        addWeatherData(new WeatherData(temp, wind, precip));
        System.out.println("Weather data recorded.");
    }

    private void updateNodeParameters() {
        System.out.println("\nAvailable Nodes:");
        for (GridNode node : grid.getAllNodes()) {
            System.out.println(node.nodeId);
        }

        System.out.print("\nEnter Node ID to update: ");
        String nodeId = scanner.next();
        GridNode node = grid.getNode(nodeId);
        if (node == null) {
            System.out.println("Node not found!");
            return;
        }

        System.out.println("\nCurrent: " + node);
        System.out.print("New Voltage: ");
        double voltage = scanner.nextDouble();
        System.out.print("New Current: ");
        double current = scanner.nextDouble();
        System.out.print("New Temperature: ");
        double temp = scanner.nextDouble();

        node.updateReadings(voltage, current, temp);
        System.out.println("Node updated.");
    }

    private void viewAllNodes() {
        System.out.println("\n=== Current Grid Status ===");
        for (GridNode node : grid.getAllNodes()) {
            System.out.println(node);
        }
    }

    public void run() {
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = scanner.nextInt();
            switch (choice) {
                case 1 -> enterWeatherData();
                case 2 -> updateNodeParameters();
                case 3 -> viewAllNodes();
                case 4 -> predictFailures();
                case 5 -> {
                    running = false;
                    System.out.println("Exiting system.");
                }
                default -> System.out.println("Invalid choice! Please try again.");
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        GridFailurePredictor predictor = new GridFailurePredictor();
        predictor.run();
    }
}
